package com.example.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdUiActionClassifierTest {
    @Test
    fun recognizesExplicitAdIndicatorsButNotOrdinaryVideoLabels() {
        assertTrue(AdUiActionClassifier.isVideoAdContext(listOf("Advertisement")))
        assertTrue(AdUiActionClassifier.isVideoAdContext(listOf("Ad 1 of 2")))
        assertTrue(AdUiActionClassifier.isVideoAdContext(listOf("Skip ad")))
        assertTrue(AdUiActionClassifier.isVideoAdContext(listOf("Video", "Sponsored post")))
        assertFalse(AdUiActionClassifier.isVideoAdContext(listOf("Skip intro", "Unmute")))
    }

    @Test
    fun onlyClassifiesAdSpecificSkipAndCloseControls() {
        assertEquals(AdUiAction.SKIP, AdUiActionClassifier.actionFor("Skip this ad", false))
        assertEquals(AdUiAction.CLOSE, AdUiActionClassifier.actionFor("Dismiss ad", false))
        assertEquals(null, AdUiActionClassifier.actionFor("Skip", true))
        assertEquals(null, AdUiActionClassifier.actionFor("Close", true))
    }

    @Test
    fun muteAndUnmuteRequireExactPlayerControlLabels() {
        assertEquals(AdUiAction.MUTE, AdUiActionClassifier.actionFor("Mute video", true))
        assertEquals(null, AdUiActionClassifier.actionFor("Mute video", false))
        assertEquals(AdUiAction.UNMUTE, AdUiActionClassifier.actionFor("Unmute", false))
        assertEquals(AdUiAction.MUTE, AdUiActionClassifier.actionFor("Mute", true))
        assertEquals(AdUiAction.UNMUTE, AdUiActionClassifier.actionFor("Unmute", true))
    }
}
