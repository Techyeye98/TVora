package tv.cleanyour.adb.session

import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.Timeout
import tv.cleanyour.adb.wire.AdbSocket
import tv.cleanyour.adb.wire.SocketFactory
import tv.cleanyour.core.model.AdbError
import tv.cleanyour.core.model.Endpoint
import tv.cleanyour.core.session.OpenResult
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 0.3.6 (issues #98, #270, #325, #468): a failed socket connect keeps its user-facing kind, and the
// diagnostics log gets the exception class and the errno style reason, never an address.
class ConnectFailureTest {
    @get:Rule val timeout: Timeout = Timeout.seconds(60)

    private val android = "failed to connect to /192.168.0.195 (port 5555) from /192.168.0.7 (port 41234) after 3000ms: " +
        "isConnected failed: EHOSTUNREACH (No route to host)"

    private class AppOwn(message: String) : IOException(message)

    @Test fun theClassAndTheErrnoReasonWithoutTheAddresses() {
        assertEquals("NoRouteToHostException, EHOSTUNREACH (No route to host)", ConnectFailure.detail(NoRouteToHostException(android)))
        val blocked = SocketException("socket failed: EPERM (Operation not permitted)")
        assertEquals("SocketException, EPERM (Operation not permitted)", ConnectFailure.detail(blocked))
        // The reason may sit on a cause, and the class is the first platform one; an app class
        // alone (renamed by R8 in a release) reads "other".
        assertEquals("SocketException, ENETUNREACH (Network is unreachable)", ConnectFailure.detail(AppOwn("wrapped").initCause(SocketException("connect failed: ENETUNREACH (Network is unreachable)"))))
        assertEquals("other", ConnectFailure.detail(AppOwn("to 192.168.0.195")))
        assertEquals("SocketException", ConnectFailure.detail(SocketException("Connection reset by tv.example.lan 192.168.0.195")))
        for (e in listOf(NoRouteToHostException(android), SocketException("ECONNABORTED (Software caused connection abort) 10.0.0.2"))) {
            val d = ConnectFailure.detail(e)
            assertFalse(Regex("\\d+\\.\\d+").containsMatchIn(d), d)
            assertFalse("5555" in d, d)
        }
    }

    @Test fun theManagerLogsTheDetailAndKeepsTheKind() = runTest {
        val lines = mutableListOf<String>()
        var next: Exception = NoRouteToHostException(android)
        val failing = object : SocketFactory {
            override suspend fun connect(endpoint: Endpoint, timeoutMs: Long): AdbSocket = throw next
        }
        val manager = AdbSessionManager(failing, Keys.signer, null, TestClock(this), backgroundScope, EmptyCoroutineContext, log = { lines += it })
        val r = manager.open(Endpoint.PlainTcp(HOST), null) {}
        assertEquals(OpenResult.Failed(AdbError.Transport.Unreachable), r)
        assertEquals("NoRouteToHostException, EHOSTUNREACH (No route to host)", manager.lastConnectFailure)
        assertEquals(listOf("session: socket connect failed: NoRouteToHostException, EHOSTUNREACH (No route to host)"), lines)
        assertTrue(lines.none { "192." in it || HOST in it })
        // Refused stays refused; its detail is logged as well.
        next = ConnectException("failed to connect to /192.168.0.195 (port 5555): connect failed: ECONNREFUSED (Connection refused)")
        assertEquals(OpenResult.Failed(AdbError.Transport.Refused), manager.open(Endpoint.PlainTcp(HOST), null) {})
        assertEquals("ConnectException, ECONNREFUSED (Connection refused)", manager.lastConnectFailure)
    }

    @Test fun aSocketThatConnectsClearsTheLastFailure() = runTest {
        val rig = Rig(this)
        assertNull(rig.manager.lastConnectFailure)
        rig.open()
        assertNull(rig.manager.lastConnectFailure)
    }
}
