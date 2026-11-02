package com.example.tools.otp

object DebugLogAnalyzer {
    fun analyzeLogs(rawLogs: List<String>): CheckResult {
        if (rawLogs.isEmpty()) {
            return CheckResult(
                id = "debug_log_analyzer",
                status = "LOGS_NOT_PROVIDED",
                findings = emptyList(),
                errors = listOf("No real diagnostic logs were provided. Add logs from the target app and run the scan again.")
            )
        }

        val findings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val searchTerms = listOf("cancel", "otp_request", "verification_failed", "orderId", "generateOtp")

        for (log in rawLogs) {
            val redacted = SecretRedactor.redact(log)
            for (term in searchTerms) {
                if (redacted.contains(term, ignoreCase = true)) {
                    findings.add("Found event '$term': $redacted")
                }
            }
        }

        val status = if (findings.isNotEmpty()) "AVAILABLE" else "NO_MATCHES"
        return CheckResult(
            id = "debug_log_analyzer",
            status = status,
            findings = findings,
            errors = errors
        )
    }
}
