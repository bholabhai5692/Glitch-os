package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Glitch OS", appName)
  }

  @Test
  fun `verify global fallback defaults to false and persists changes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)

    assertFalse("Global fallback should default to false", settingsManager.settingsFlow.value.globalFallbackEnabled)

    // Update to true
    val current = settingsManager.settingsFlow.value
    settingsManager.updateSettings(current.copy(globalFallbackEnabled = true))
    assertTrue("Global fallback should be true after updating", settingsManager.settingsFlow.value.globalFallbackEnabled)

    // Re-instantiate SettingsManager to verify SharedPreferences persistence
    val reloadedManager = SettingsManager(context)
    assertTrue("Global fallback should persist as true in SharedPreferences", reloadedManager.settingsFlow.value.globalFallbackEnabled)

    reloadedManager.updateSettings(current.copy(globalFallbackEnabled = false))
    assertFalse("Global fallback should reset back to false", reloadedManager.settingsFlow.value.globalFallbackEnabled)
  }

}
