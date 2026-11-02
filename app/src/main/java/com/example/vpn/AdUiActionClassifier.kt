package com.example.vpn

import java.util.Locale

internal enum class AdUiAction {
    MUTE,
    SKIP,
    CLOSE,
    UNMUTE
}

internal object AdUiActionClassifier {
    fun isVideoAdContext(labels: Iterable<String>): Boolean =
        labels.any(::isVideoAdLabel) || labels.any(::isSkipAdLabel)

    fun actionFor(label: String, isVideoAdContext: Boolean): AdUiAction? {
        val normalized = normalize(label)
        return when {
            isSkipAdLabel(normalized) -> AdUiAction.SKIP
            isCloseAdLabel(normalized) -> AdUiAction.CLOSE
            isVideoAdContext && normalized in MUTE_LABELS -> AdUiAction.MUTE
            normalized in UNMUTE_LABELS -> AdUiAction.UNMUTE
            else -> null
        }
    }

    private fun isVideoAdLabel(label: String): Boolean {
        val normalized = normalize(label)
        return normalized == "advertisement" ||
            normalized.startsWith("advertisement ") ||
            normalized.startsWith("video ad") ||
            AD_SEQUENCE.matches(normalized)
    }

    private fun isSkipAdLabel(label: String): Boolean = normalize(label) in SKIP_LABELS

    private fun isCloseAdLabel(label: String): Boolean = normalize(label) in CLOSE_LABELS

    private fun normalize(label: String): String =
        label.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private val AD_SEQUENCE = Regex("""ad\s+\d+\s+of\s+\d+""")
    private val SKIP_LABELS = setOf(
        "skip ad",
        "skip this ad",
        "skip video ad",
        "skip advertisement"
    )
    private val CLOSE_LABELS = setOf(
        "close ad",
        "close advertisement",
        "dismiss ad",
        "dismiss advertisement"
    )
    private val MUTE_LABELS = setOf("mute", "mute video", "mute ad")
    private val UNMUTE_LABELS = setOf("unmute", "unmute video", "unmute ad")
}
