package tv.cleanyour.core.flows

// 0.3.6 (issues #98, #270, #325, #468): the TV's port answered, but the connection failed. The door
// shows a card with the likely causes on the phone's side and "Copy error", which puts `summary` on
// the clipboard so the person can paste it into an issue. `kind` is the log's word (LogCodes).
data class ConnectError(val kind: String, val summary: String)

// The short text "Copy error" copies: the app and Android version, the error kind with the class
// and errno reason the session logged, and the last lines of the diagnostics log. The log is
// redacted already (the app's Redactor); this masks once more what could name a network: IPv4 and
// IPv6 addresses (the last octet the Redactor keeps too) and local host names.
object ConnectErrorSummary {
    const val LOG_LINES = 10
    private const val HEADER = "Clean Your TV "
    private const val OCTET = "(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)"
    private val IPV4 = Regex("(?<![A-Za-z0-9.])$OCTET\\.$OCTET\\.$OCTET\\.$OCTET(?![A-Za-z0-9]|\\.[A-Za-z0-9])")
    private val MASKED_IPV4 = Regex("x\\.x\\.x\\.\\d{1,3}")
    private val IPV6 = Regex("(?i)(?<![A-Za-z0-9:])(?:[0-9a-f]{0,4}:){2,7}[0-9a-f]{0,4}(?![A-Za-z0-9:])")
    private val LOCAL_HOST = Regex("(?i)(?<![A-Za-z0-9.-])[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.(?:localdomain|local|lan|home\\.arpa)(?![A-Za-z0-9-])")

    fun redact(line: String): String = line
        .replace(LOCAL_HOST, "[host]")
        .replace(MASKED_IPV4, "[address]")
        .replace(IPV4, "[address]")
        .replace(IPV6, "[address]")

    fun build(about: AppAbout?, kind: String, detail: String?, log: String): String {
        val lines = log.lines().filter { it.isNotBlank() }.filterNot { it.startsWith(HEADER) }.takeLast(LOG_LINES)
        return buildList {
            add("Clean Your TV connection error")
            add("App: ${about?.appLine ?: "unknown"}")
            add("Phone: ${about?.phoneAndroid?.ifBlank { null } ?: "Android unknown"}")
            add("Error: $kind" + (detail?.let { " ($it)" } ?: ""))
            if (lines.isNotEmpty()) {
                add("Last log lines:")
                addAll(lines)
            }
        }.joinToString("\n") { redact(it) }
    }
}
