package com.example.tools.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealInputDiagnosticsTest {
    @Test
    fun missingLogsAreReportedInsteadOfReplacedWithExamples() {
        val result = DebugLogAnalyzer.analyzeLogs(emptyList())

        assertEquals("LOGS_NOT_PROVIDED", result.status)
        assertTrue(result.findings.isEmpty())
        assertTrue(result.errors.single().contains("No real diagnostic logs"))
    }

    @Test
    fun missingSourceIsReportedInsteadOfReplacedWithExamples() {
        val result = SourceCodeAnalyzer.analyzeSource(emptyMap())

        assertEquals("SOURCE_NOT_PROVIDED", result.status)
        assertTrue(result.findings.isEmpty())
        assertTrue(result.errors.single().contains("No real source files"))
    }

    @Test
    fun missingNetworkCaptureIsReportedInsteadOfInventingCalls() {
        val result = NetworkInspector.inspectNetworkCalls(null)

        assertEquals("NETWORK_DATA_NOT_PROVIDED", result.status)
        assertTrue(result.findings.isEmpty())
        assertTrue(result.errors.single().contains("No real network capture"))
    }

    @Test
    fun emptyNetworkCaptureReportsThatNoCallsWereObserved() {
        val result = NetworkInspector.inspectNetworkCalls(emptyList())

        assertEquals("NO_CALLS_OBSERVED", result.status)
        assertTrue(result.findings.isEmpty())
        assertTrue(result.errors.isEmpty())
    }
}
