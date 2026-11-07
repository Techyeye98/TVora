package tv.cleanyour.core.flows

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import tv.cleanyour.core.discovery.DiscoveryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tv.cleanyour.core.discovery.Discovery
import tv.cleanyour.core.model.AdbError
import tv.cleanyour.core.model.ConnectionState
import tv.cleanyour.core.model.DebugState
import tv.cleanyour.core.model.Endpoint
import tv.cleanyour.core.model.Tv
import tv.cleanyour.core.model.TvId
import tv.cleanyour.core.session.OpenResult
import tv.cleanyour.core.session.SessionHold
import tv.cleanyour.core.session.SessionManager
import tv.cleanyour.core.session.TvSession
import tv.cleanyour.core.store.Clock
import tv.cleanyour.core.store.TvRepository

// What the app shows while a returning write waits for the TV (UX 4.3, screen M1a). The app
// observes `reach` and lays the debugging step over whatever screen asked.
sealed interface Reach {
    data object Idle : Reach
    data class Connecting(val tvId: TvId, val tvName: String) : Reach
    data class NeedsDebugging(val tvId: TvId, val tvName: String) : Reach      // M1a: port 5555 closed
    data class AwaitingAllow(val tvId: TvId, val tvName: String, val fingerprint: String) : Reach   // after "Reset my key"
    data class WrongNetwork(val tvId: TvId, val tvName: String) : Reach        // D19: by subnet, never by name
}

sealed interface AccessResult {
    data class Ok(val session: TvSession) : AccessResult
    data object Cancelled : AccessResult
    data class Failed(val error: AdbError) : AccessResult    // WrongTv (D37), NoShellV2, auth
    data object Unknown : AccessResult                        // no record for this TvId
}

// Connects to a TV the phone already knows, for any write outside the first-run wizard. A live
// session is reused; otherwise each remembered address is probed, and while port 5555 stays
// closed the debugging step shows and the probe repeats every 2 s until the TV answers or the
// person cancels. `expected` is always the TvId, so a different TV at the address never gets a
// line (D37).
@OptIn(ExperimentalCoroutinesApi::class)
class TvAccess(
    private val sessions: SessionManager,
    private val discovery: Discovery,
    private val tvs: TvRepository,
    private val clock: Clock,
    private val pollMs: Long = 2_000,
    private val giveUpMs: Long = 10 * 60_000,
    private val wirelessPairing: WirelessPairing? = null,
    private val searchMs: Long = 15_000,
    private val searchEveryMs: Long = 30_000,
) {
    private val mutableReach = MutableStateFlow<Reach>(Reach.Idle)
    val reach: StateFlow<Reach> = mutableReach.asStateFlow()
    private val cancelGen = MutableStateFlow(0L)
    private val reconnectLock = Mutex()

    fun cancelWaiting() { cancelGen.value = cancelGen.value + 1 }

    // T7 item 1: AdbTvSession closes its socket 60 s after the last hold, and the engine refuses
    // NOT_CONNECTED on an idle session. The wizard keeps one hold per TV from the connect until Done
    // or a stop card, so a slow read of "What we found" keeps the session (and its keepalive ping).
    private val held = HashMap<TvId, Pair<TvSession, SessionHold>>()

    fun keep(session: TvSession) {
        val old = synchronized(held) {
            val now = held[session.tvId]
            if (now?.first === session) return
            held.put(session.tvId, session to session.hold())
        }
        old?.second?.close()
    }

    fun release(tvId: TvId) { synchronized(held) { held.remove(tvId) }?.second?.close() }

    fun holding(tvId: TvId): Boolean = synchronized(held) { tvId in held }

    fun live(tvId: TvId): TvSession? = sessions.existing(tvId)?.takeIf { it.state.value is ConnectionState.Connected }

    suspend fun session(tvId: TvId): AccessResult = reconnectLock.withLock { connect(tvId) }

    // Automatic reconnect never sends a public key or opens the debugging sheet.
    suspend fun quiet(tvId: TvId): TvSession? = withTimeoutOrNull(20_000) {
        reconnectLock.withLock {
            live(tvId)?.let { return@withLock it }
            val tv = tvs.tvs.value.firstOrNull { it.id == tvId } ?: return@withLock null
            if (!discovery.network().value.onWifi) return@withLock null
            for (endpoint in tv.endpoints) {
                if (endpoint is Endpoint.PlainTcp && !discovery.probe5555(endpoint.host)) continue
                val result = attempt { sessions.openKnown(endpoint, tvId) }.getOrNull()
                if (result is OpenResult.Ok) { remember(tv, endpoint); return@withLock result.session }
            }
            val found = findMoved(tv, emptySet()) ?: return@withLock null
            remember(tv, found.first)
            found.second
        }
    }

    private suspend fun remember(tv: Tv, endpoint: Endpoint) {
        val net = discovery.network().value
        tvs.upsert(current(tv.id, tv).copy(endpoints = listOf(endpoint), debugging = DebugState.ON,
            lastSeenAt = clock.nowMs(), lastSubnet = net.subnet ?: tv.lastSubnet))
    }

    private suspend fun connect(tvId: TvId): AccessResult {
        live(tvId)?.let { return AccessResult.Ok(it) }
        val tv = tvs.tvs.value.firstOrNull { it.id == tvId } ?: return AccessResult.Unknown
        val gen = cancelGen.value
        val hosts = tv.endpoints.filterIsInstance<Endpoint.PlainTcp>()
        try {
            return coroutineScope {
                val moved = CompletableDeferred<Pair<Endpoint, TvSession>>()
                val searcher = launch {
                    delay(pollMs)
                    while (true) {
                        if (discovery.network().value.onWifi) {
                            findMoved(tv, hosts.map { it.host }.toSet())?.let { moved.complete(it); return@launch }
                        }
                        delay(searchEveryMs)
                    }
                }
                try { poll(tv, gen, hosts, moved) } finally { searcher.cancel() }
            }
        } finally { mutableReach.value = Reach.Idle }
    }

    private suspend fun poll(tv: Tv, gen: Long, hosts: List<Endpoint.PlainTcp>, moved: CompletableDeferred<Pair<Endpoint, TvSession>>): AccessResult {
        val tvId = tv.id
        var waited = 0L
        while (true) {
            if (cancelGen.value != gen) return AccessResult.Cancelled
            val net = discovery.network().value
            if (moved.isCompleted) {
                val (endpoint, session) = moved.getCompleted()
                tvs.upsert(current(tvId, tv).copy(endpoints = listOf(endpoint) + tv.endpoints.filter { it !is Endpoint.PlainTcp },
                    lastSeenAt = clock.nowMs(), debugging = DebugState.ON, lastSubnet = net.subnet ?: tv.lastSubnet))
                return AccessResult.Ok(session)
            }
            // A changed subnet is a hint, not a reason to stop looking for the same trusted TV.
            if (!net.onWifi) {
                mutableReach.value = Reach.WrongNetwork(tvId, tv.nickname)
            } else {
                val tls = tv.endpoints.filterIsInstance<Endpoint.Tls>().firstOrNull()
                val fresh = if (tls != null && wirelessPairing != null) withTimeoutOrNull(1_500) {
                    discovery.discover().first { event ->
                        val found = when (event) { is DiscoveryEvent.Found -> event.tv; is DiscoveryEvent.Updated -> event.tv; else -> null }
                        found?.host == tls.host && found.connectService != null
                    }
                } else null
                val found = when (fresh) { is DiscoveryEvent.Found -> fresh.tv; is DiscoveryEvent.Updated -> fresh.tv; else -> null }
                val open: Endpoint? = tls?.let { wirelessPairing?.endpoint(it.host, found?.connectService?.port ?: it.port) }
                    ?: hosts.firstOrNull { discovery.probe5555(it.host) }
                if (open != null) {
                    mutableReach.value = Reach.Connecting(tvId, tv.nickname)
                    val elsewhere = tv.lastSubnet != null && net.subnet != null && tv.lastSubnet != net.subnet
                    val result = if (elsewhere) sessions.openKnown(open, tvId) else sessions.open(open, tvId) { fp ->
                        mutableReach.value = Reach.AwaitingAllow(tvId, tv.nickname, fp)
                    }
                    when (result) {
                        is OpenResult.Ok -> {
                            tvs.upsert(current(tvId, tv).copy(lastSeenAt = clock.nowMs(), debugging = DebugState.ON,
                                endpoints = listOf(open), lastSubnet = net.subnet ?: tv.lastSubnet))
                            return AccessResult.Ok(result.session)
                        }
                        is OpenResult.Failed -> if (!elsewhere && result.error !is AdbError.Policy.WrongTv &&
                            (result.error == AdbError.Transport.NoShellV2 || result.error == AdbError.Auth.Denied)) return AccessResult.Failed(result.error)
                    }
                }
                mutableReach.value = Reach.NeedsDebugging(tvId, tv.nickname)
            }
            if (waited >= giveUpMs) return AccessResult.Cancelled
            withTimeoutOrNull(pollMs) { moved.await() }
            waited += pollMs
        }
    }

    // Discovery is only a candidate list: only the saved key and TvId can establish this is our TV.
    private suspend fun findMoved(tv: Tv, saved: Set<String>): Pair<Endpoint, TvSession>? {
        val tried = HashSet<String>()
        var hit: Pair<Endpoint, TvSession>? = null
        withTimeoutOrNull(searchMs) {
            discovery.discover().firstOrNull { event ->
                val found = when (event) { is DiscoveryEvent.Found -> event.tv; is DiscoveryEvent.Updated -> event.tv; else -> null }
                if (found == null || found.host in saved || !tried.add(found.host)) return@firstOrNull false
                val connectService = found.connectService
                val endpoint = if (connectService != null && wirelessPairing != null)
                    wirelessPairing.endpoint(found.host, connectService.port)
                else if (discovery.probe5555(found.host)) Endpoint.PlainTcp(found.host) else null
                if (endpoint == null) return@firstOrNull false
                when (val result = attempt { sessions.openKnown(endpoint, tv.id) }.getOrNull()) {
                    is OpenResult.Ok -> { hit = endpoint to result.session; true }
                    else -> false
                }
            }
        }
        return hit
    }

    private fun current(tvId: TvId, fallback: Tv): Tv = tvs.tvs.value.firstOrNull { it.id == tvId } ?: fallback
}
