package tv.cleanyour.core.probe

import tv.cleanyour.core.model.*
import tv.cleanyour.core.session.*

// The one launcher config file, read only; the URI and shell command never contain user data.
object LauncherSettingsFile {
    private const val CAP = 64L * 1024
    private val path = RemotePath(RemoteDir.OVERLAY, listOf("config.json"))
    @OptIn(RawShell::class)
    suspend fun read(session: TvSession): ByteArray? {
        try { return session.pull(path, CAP) }
        catch (e: AdbException) {
            if (e.error !is AdbError.NonZeroExit && e.error !is AdbError.Transport.Protocol) throw e
        }
        val result = session.exec(ShellLine("content read --uri content://com.gathin.tvhome.theme/config.json"),
            ExecSpec(stdoutCap = CAP.toInt() + 1, retry = RetryClass.IDEMPOTENT_READ))
        if (result.exit != 0 || result.stderr.isNotBlank() || result.stdout.isBlank() ||
            result.stdout.startsWith("Error") || result.stdout.startsWith("Unsupported operation")) return null
        val bytes = result.stdout.encodeToByteArray()
        require(!result.truncated && bytes.size <= CAP) { "launcher settings exceed the read limit" }
        return bytes
    }
}
