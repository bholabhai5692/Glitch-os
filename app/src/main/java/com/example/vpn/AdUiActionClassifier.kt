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
            normalized == "sponsored" ||
            normalized.startsWith("sponsored ") ||
            normalized.startsWith("sponsored •") ||
            AD_SEQUENCE.matches(normalized)
    }

    private fun isSkipAdLabel(label: String): Boolean {
        val normalized = normalize(label)
        return normalized in SKIP_LABELS ||
            normalized.startsWith("skip ad in ") ||
            normalized.startsWith("skip this ad in ") ||
            normalized.startsWith("skip advertisement in ")
    }

    private fun isCloseAdLabel(label: String): Boolean {
        val normalized = normalize(label)
        return normalized in CLOSE_LABELS ||
            normalized.startsWith("close ad ") ||
            normalized.startsWith("close advertisement ")
    }

    private fun normalize(label: String): String =
        label.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private val AD_SEQUENCE = Regex("""ad\s+\d+\s+of\s+\d+""")
    private val SKIP_LABELS = setOf(
        "skip ad",
        "skip ads",
        "skip this ad",
        "skip video ad",
        "skip advertisement",
        "skip this advertisement"
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
