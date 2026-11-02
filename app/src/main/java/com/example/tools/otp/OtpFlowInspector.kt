package com.example.tools.otp

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object OtpFlowInspector {
    fun runInspection(
        context: Context,
        targetPackage: String?,
        customLogs: List<String>?,
        customSource: Map<String, String>?,
        customNetworkCalls: List<ApiInspectionRecord>? = null
    ): DiagnosticReport {
        val scanId = UUID.randomUUID().toString().substring(0, 8)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val timestamp = dateFormat.format(Date())

        // Check A: App Identifier
        val appInfo = AppIdentifier.inspectApp(context, targetPackage)

        // Check B: Logcat Analyzer
        val logResult = DebugLogAnalyzer.analyzeLogs(customLogs.orEmpty())

        // Check C: Source Code Search
        val sourceResult = SourceCodeAnalyzer.analyzeSource(customSource.orEmpty())

        // Check D: Network Inspector
        val networkResult = NetworkInspector.inspectNetworkCalls(customNetworkCalls)

        return DiagnosticReport(
            scanId = scanId,
            timestamp = timestamp,
            application = appInfo,
            checks = listOf(logResult, sourceResult, networkResult),
            security = SecurityMeta(secretsRedacted = true, networkTransfer = false)
        )
    }
}
