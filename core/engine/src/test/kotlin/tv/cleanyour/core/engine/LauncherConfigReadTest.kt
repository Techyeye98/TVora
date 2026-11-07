package tv.cleanyour.core.engine

import kotlinx.coroutines.test.runTest
import tv.cleanyour.testing.fixtures.FakeProber
import tv.cleanyour.testing.fixtures.FakeTvSession
import tv.cleanyour.testing.fixtures.Recording
import kotlin.test.*

class LauncherConfigReadTest {
    private val read = "content read --uri content://com.gathin.tvhome.theme/config.json"
    @Test fun readsTheExistingConfigDirectlyWhenTheShellCanReadIt() = runTest {
        val session = FakeTvSession()
        val data = "{\"reorder_button\":false}".encodeToByteArray()
        session.files[FileOverlayTransport.CONFIG.absolute] = data
        assertContentEquals(data, FileOverlayTransport(FakeProber()).readConfig(session))
        assertTrue(session.execLines.isEmpty())
    }
    @Test fun readsThroughTheProviderWhenTheShellCannotReadTheFolder() = runTest {
        val data = "{\"show_bluetooth_pill\":true}"
        val session = FakeTvSession(recording = Recording().on(read, data))
        assertContentEquals(data.encodeToByteArray(), FileOverlayTransport(FakeProber()).readConfig(session))
        assertEquals(listOf(read), session.execLines)
    }
    @Test fun unsupportedProviderDoesNotMasqueradeAsSettings() = runTest {
        val session = FakeTvSession(recording = Recording().on(read, "Unsupported operation: read"))
        assertNull(FileOverlayTransport(FakeProber()).readConfig(session))
    }
    @Test fun oversizedProviderOutputIsRejected() = runTest {
        val session = FakeTvSession(recording = Recording().on(read, "x".repeat(70 * 1024)))
        assertFailsWith<IllegalArgumentException> { FileOverlayTransport(FakeProber()).readConfig(session) }
    }
}
