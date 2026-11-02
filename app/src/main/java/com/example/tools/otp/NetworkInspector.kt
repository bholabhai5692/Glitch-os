package com.example.tools.otp

data class ApiInspectionRecord(
    val endpoint: String,
    val method: String,
    val responseStatus: Int,
    val durationMs: Long,
    val timestamp: String
)

object NetworkInspector {
    fun inspectNetworkCalls(calls: List<ApiInspectionRecord>?): CheckResult {
        if (calls == null) {
            return CheckResult(
                id = "network_inspector",
                status = "NETWORK_DATA_NOT_PROVIDED",
                findings = emptyList(),
                errors = listOf("No real network capture was provided. Capture or provide network call records and run the scan again.")
            )
        }

        val findings = calls.map { call ->
            SecretRedactor.redact("[${call.method}] ${call.endpoint} -> Status: ${call.responseStatus} (${call.durationMs}ms)")
        }

        return CheckResult(
            id = "network_inspector",
            status = if (calls.isEmpty()) "NO_CALLS_OBSERVED" else "AVAILABLE",
            findings = findings,
            errors = emptyList()
        )
    }
}
