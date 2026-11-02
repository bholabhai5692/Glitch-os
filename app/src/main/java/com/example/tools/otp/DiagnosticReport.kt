package com.example.tools.otp

data class DiagnosticReport(
    val schemaVersion: String = "1.0",
    val module: String = "otp_flow_inspector",
    val scanId: String,
    val timestamp: String,
    val application: AppDiagnosticInfo,
    val checks: List<CheckResult>,
    val security: SecurityMeta
)

data class AppDiagnosticInfo(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long,
    val targetSdk: Int,
    val status: String,
    val message: String? = null
)

data class CheckResult(
    val id: String,
    val status: String,
    val findings: List<String>,
    val errors: List<String>
)

data class SecurityMeta(
    val secretsRedacted: Boolean = true,
    val networkTransfer: Boolean = false
)
