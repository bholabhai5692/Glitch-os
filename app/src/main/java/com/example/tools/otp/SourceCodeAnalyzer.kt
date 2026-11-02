package com.example.tools.otp

object SourceCodeAnalyzer {
    fun analyzeSource(codeSnippets: Map<String, String>): CheckResult {
        val findings = mutableListOf<String>()
        val searchTerms = listOf("otp", "verify", "cancelOrder", "orderId", "generateOtp")

        if (codeSnippets.isEmpty()) {
            return CheckResult(
                id = "source_code_search",
                status = "SOURCE_NOT_PROVIDED",
                findings = emptyList(),
                errors = listOf("No real source files were provided. Select source files and run the scan again.")
            )
        }

        for ((fileName, content) in codeSnippets) {
            val lines = content.lines()
            for ((index, line) in lines.withIndex()) {
                val redacted = SecretRedactor.redact(line)
                for (term in searchTerms) {
                    if (redacted.contains(term, ignoreCase = true)) {
                        findings.add("$fileName:${index + 1} -> matched '$term': $redacted")
                    }
                }
            }
        }

        return CheckResult(
            id = "source_code_search",
            status = if (findings.isEmpty()) "NO_MATCHES" else "AVAILABLE",
            findings = findings,
            errors = emptyList()
        )
    }
}
