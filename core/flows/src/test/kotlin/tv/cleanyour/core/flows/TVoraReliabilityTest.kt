package tv.cleanyour.core.flows

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import tv.cleanyour.core.discovery.*
import tv.cleanyour.core.model.*
import tv.cleanyour.testing.fixtures.Samples
import tv.cleanyour.testing.fixtures.FakeTvSession
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TVoraReliabilityTest {
    @Test fun dockOffersLaunchableSystemAppsAndExcludesBackgroundServices() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = h.knownTv()
        val store = PackageName("com.android.vending")
        val settings = PackageName("com.android.tv.settings")
        val curtain = PackageName("tv.cleanyour.curtain")
        val token = PackageName("com.example.tokenservice")
        val vlc = PackageName("org.videolan.vlc")
        val fallback = Component(settings, ".FallbackHome")
        fun st(system: Boolean) = PackageState(EnabledState.ENABLED, system, 1, null, false, false, null, false)
        h.snapshots.save(Samples.snapshot(takenAtMs = Samples.NOW_MS + 1,
            home = HomeResolution(Launcher.home, listOf(HomeClaimant(Launcher.home, 0), HomeClaimant(fallback, -1000))),
            packages = Samples.miTvPackages() + mapOf(store to st(true), settings to st(true), curtain to st(false), token to st(false), vlc to st(false))))
        h.prober.launchableApps = setOf(store, settings, curtain, vlc)
        val m = HomeScreenMachine(h.manageDeps(), backgroundScope, tv.id)
        settle()
        val offered = m.state.value.apps.map { it.pkg }.toSet()
        assertTrue(setOf(store, settings, vlc).all { it in offered }, offered.toString())
        assertFalse(curtain in offered || token in offered)
        h.prober.launchableApps = null
        val offline = HomeScreenMachine(h.manageDeps(), backgroundScope, tv.id)
        settle()
        assertFalse(curtain in offline.state.value.apps.map { it.pkg })
    }

    @Test fun quietReconnectFindsTheTrustedTvAcrossANewSubnet() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = Samples.tv().copy(lastSubnet = "192.0.2.0/24")
        h.tvs.upsert(tv)
        val host = "198.51.100.44"
        h.sessions.register(host, h.session)
        h.discovery.open5555[host] = true
        h.discovery.emit(DiscoveryEvent.NetworkState(true, "198.51.100.0/24"))
        h.discovery.emit(DiscoveryEvent.Found(FoundTv(host, null, null, setOf(DiscoverySource.GOOGLE_CAST), false, null, null, Samples.NOW_MS)))
        assertNotNull(h.access.quiet(tv.id))
        assertEquals(listOf(Endpoint.PlainTcp(host)), h.tvs.tvs.value.single().endpoints)
        assertEquals("198.51.100.0/24", h.tvs.tvs.value.single().lastSubnet)
        assertTrue(h.sessions.opens.isEmpty(), "automatic reconnect never sends a public key")
        assertEquals(Reach.Idle, h.access.reach.value)
    }

    @Test fun aSavedAddressOnAnotherNetworkNeverShowsAnAllowDialog() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = Samples.tv().copy(lastSubnet = "198.51.100.0/24")
        h.tvs.upsert(tv)
        h.discovery.open5555[HOST] = true
        h.sessions.allowPromptFor[HOST] = "AB:CD"
        val job = backgroundScope.launch { h.access.session(tv.id) }
        settle(2100)
        assertTrue(h.sessions.opens.isEmpty())
        assertTrue(h.sessions.knownOpens.isNotEmpty())
        assertIs<Reach.NeedsDebugging>(h.access.reach.value)
        job.cancel(); settle()
        assertEquals(0, h.discovery.activeCollectors)
    }

    @Test fun dashboardHoldsTheConnectionOnlyWhileResumed() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = h.knownTv()
        val m = ManageMachine(h.manageDeps(), backgroundScope, tv.id)
        settle()
        assertEquals(0, h.session.openHolds)
        m.dispatch(ManageIntent.Resumed); settle()
        assertEquals(1, h.session.openHolds)
        m.dispatch(ManageIntent.Paused); settle()
        assertEquals(0, h.session.openHolds)
    }
    @Test fun resumedDashboardQuietlyReconnectsWithoutChangingItsLayout() = runTest(timeout = 30.seconds) {
        val h = Harness()
        val tv = h.knownTv()
        h.sessions.closeAll()
        val connected = FakeTvSession()
        h.sessions.register(HOST, connected)
        h.discovery.open5555[HOST] = true
        val m = ManageMachine(h.manageDeps(), backgroundScope, tv.id)
        settle()
        assertTrue(m.state.value.offline)
        val opensBefore = h.sessions.opens.size
        m.dispatch(ManageIntent.Resumed); settle()
        assertFalse(m.state.value.offline)
        assertEquals(opensBefore, h.sessions.opens.size)
        assertTrue(h.sessions.knownOpens.isNotEmpty())
        assertEquals(1, connected.openHolds)
        m.dispatch(ManageIntent.Paused); settle()
        assertEquals(0, connected.openHolds)
    }

}
