package com.example.vpn

import android.util.Base64

/**
 * Data Model for VPN Gate (University of Tsukuba, Japan) Academic Community Servers.
 * Parsed from live CSV API feed: http://www.vpngate.net/api/iphone/
 */
data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Long,
    val pingMs: Int,
    val speedBps: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Int,
    val uptimeSec: Long,
    val totalUsers: Long,
    val totalTrafficBytes: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVpnConfigBase64: String
) {
    val speedMbps: Double
        get() = (speedBps / 1_000_000.0 * 10).toInt() / 10.0

    val countryFlag: String
        get() = countryCodeToEmoji(countryShort)

    val decodedConfig: String
        get() = try {
            String(Base64.decode(openVpnConfigBase64, Base64.DEFAULT))
        } catch (_: Exception) {
            ""
        }

    val pingRating: PingQuality
        get() = when {
            pingMs in 1..45 -> PingQuality.EXCELLENT
            pingMs in 46..120 -> PingQuality.GOOD
            pingMs in 121..250 -> PingQuality.FAIR
            else -> PingQuality.HIGH_LATENCY
        }

    companion object {
        fun countryCodeToEmoji(code: String): String {
            if (code.length != 2) return "🌐"
            val firstChar = Character.codePointAt(code.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(code.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
    }
}

enum class PingQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    HIGH_LATENCY
}
