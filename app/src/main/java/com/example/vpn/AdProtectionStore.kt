package com.example.vpn

import android.content.Context
import android.content.SharedPreferences

data class AppAdProtectionStats(
    val skippedAds: Long = 0,
    val closedAds: Long = 0,
    val mutedAds: Long = 0
)

data class AdProtectionStats(
    val blockedDnsLookups: Long = 0,
    val skippedAds: Long = 0,
    val closedAds: Long = 0,
    val mutedAds: Long = 0,
    val apps: Map<String, AppAdProtectionStats> = emptyMap()
) {
    val estimatedTimeSavedSeconds: Long
        get() = skippedAds * 5 + closedAds * 3
}

object AdProtectionStore {
    val supportedApps = listOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.facebook.katana"
    )

    private const val PREFERENCES = "ad_protection"
    private const val KEY_BLOCKED_DNS = "blocked_dns_lookups"
    private const val KEY_SKIPPED = "skipped_ads"
    private const val KEY_CLOSED = "closed_ads"
    private const val KEY_MUTED = "muted_ads"

    @Synchronized
    fun isAppEnabled(context: Context, packageName: String): Boolean =
        preferences(context).getBoolean(appEnabledKey(packageName), true)

    @Synchronized
    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        require(packageName in supportedApps) { "Unsupported ad-control app." }
        preferences(context).edit().putBoolean(appEnabledKey(packageName), enabled).apply()
    }

    @Synchronized
    fun readStats(context: Context): AdProtectionStats {
        val prefs = preferences(context)
        val perApp = supportedApps.associateWith { packageName ->
            AppAdProtectionStats(
                skippedAds = prefs.getLong(appActionKey(packageName, "skipped"), 0),
                closedAds = prefs.getLong(appActionKey(packageName, "closed"), 0),
                mutedAds = prefs.getLong(appActionKey(packageName, "muted"), 0)
            )
        }
        return AdProtectionStats(
            blockedDnsLookups = prefs.getLong(KEY_BLOCKED_DNS, 0),
            skippedAds = prefs.getLong(KEY_SKIPPED, 0),
            closedAds = prefs.getLong(KEY_CLOSED, 0),
            mutedAds = prefs.getLong(KEY_MUTED, 0),
            apps = perApp
        )
    }

    @Synchronized
    fun recordBlockedDnsLookups(context: Context, count: Long) {
        if (count <= 0) return
        increment(preferences(context), KEY_BLOCKED_DNS, count)
    }

    @Synchronized
    internal fun recordAdAction(context: Context, packageName: String, action: AdUiAction): Boolean {
        if (packageName !in supportedApps) return false
        val key = when (action) {
            AdUiAction.SKIP -> "skipped"
            AdUiAction.CLOSE -> "closed"
            AdUiAction.MUTE -> "muted"
            AdUiAction.UNMUTE -> return false
        }
        val prefs = preferences(context)
        increment(prefs, appActionKey(packageName, key), 1)
        increment(
            prefs,
            when (action) {
                AdUiAction.SKIP -> KEY_SKIPPED
                AdUiAction.CLOSE -> KEY_CLOSED
                AdUiAction.MUTE -> KEY_MUTED
                AdUiAction.UNMUTE -> return false
            },
            1
        )
        return true
    }

    @Synchronized
    fun resetStats(context: Context) {
        val prefs = preferences(context)
        val enabledValues = supportedApps.associate { packageName ->
            appEnabledKey(packageName) to prefs.getBoolean(appEnabledKey(packageName), true)
        }
        prefs.edit().clear().apply {
            enabledValues.forEach { (key, enabled) -> putBoolean(key, enabled) }
        }.apply()
    }

    private fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private fun increment(prefs: SharedPreferences, key: String, amount: Long) {
        prefs.edit().putLong(key, prefs.getLong(key, 0) + amount).apply()
    }

    private fun appEnabledKey(packageName: String) = "enabled_$packageName"
    private fun appActionKey(packageName: String, action: String) = "$packageName.$action"
}
