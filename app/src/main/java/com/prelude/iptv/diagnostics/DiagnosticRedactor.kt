package com.prelude.iptv.diagnostics

/** Removes provider and user secrets before app-authored diagnostic text is stored. */
object DiagnosticRedactor {
    private const val MAX_TEXT_LENGTH = 2_000
    private const val MAX_STACK_LINES = 24

    private val url = Regex("(?i)\\b(?:https?|rtsp|rtmp)://[^\\s<>'\\\"]+")
    private val secretParameter = Regex(
        "(?i)(username|user|password|pass|token|auth|authorization|api[_-]?key|mac)\\s*[=:]\\s*([^&\\s,;]+)"
    )
    private val macAddress = Regex("(?i)\\b(?:[0-9a-f]{2}:){5}[0-9a-f]{2}\\b")
    private val ipv4 = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?\\b")
    private val email = Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")

    fun redact(value: String?): String {
        if (value.isNullOrBlank()) return "Χωρίς μήνυμα"
        return value
            .replace(url, "[URL_REDACTED]")
            .replace(secretParameter) { match -> "${match.groupValues[1]}=[REDACTED]" }
            .replace(macAddress, "[MAC_REDACTED]")
            .replace(ipv4, "[IP_REDACTED]")
            .replace(email, "[EMAIL_REDACTED]")
            .take(MAX_TEXT_LENGTH)
    }

    fun fromThrowable(throwable: Throwable, capturedAtMillis: Long): PendingDiagnosticReport {
        val safeType = throwable.javaClass.name
            .takeIf { it.startsWith("java.") || it.startsWith("kotlin.") || it.startsWith("com.prelude.") }
            ?: throwable.javaClass.simpleName
        val safeStack = throwable.stackTrace
            .asSequence()
            .take(MAX_STACK_LINES)
            .joinToString("\n") { redact(it.toString()) }
        return PendingDiagnosticReport(
            capturedAtMillis = capturedAtMillis,
            exceptionType = redact(safeType),
            // Exception messages can contain a media title supplied by a remote
            // catalog. The local privacy-safe report intentionally omits them.
            summary = "Απροσδόκητος τερματισμός (${safeType.substringAfterLast('.')})",
            stackSummary = safeStack,
        )
    }
}
