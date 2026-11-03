package com.example.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class AdProtectionStatsTest {
    @Test
    fun estimatedTimeSavedUsesOnlySuccessfulSkipAndCloseActions() {
        val stats = AdProtectionStats(
            skippedAds = 2,
            closedAds = 3,
            mutedAds = 12,
            blockedDnsLookups = 500
        )

        assertEquals(19L, stats.estimatedTimeSavedSeconds)
    }

    @Test
    fun noAdActionsMeansNoEstimatedTimeSaved() {
        assertEquals(0L, AdProtectionStats().estimatedTimeSavedSeconds)
    }
}
