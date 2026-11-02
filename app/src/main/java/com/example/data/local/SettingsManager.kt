package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val serverUrl: String = "http://10.0.2.2:5000",
    val engineMode: String = "Smart Auto", // "Smart Auto", "On-Device Studio Engine", "Remote Server Only"
    val defaultLanguage: String = "Hindi",
    val defaultCategory: String = "Technology",
    val defaultDuration: Int = 60,
    val defaultStyle: String = "Cinematic",
    val defaultAspect: String = "9:16",
    val visualRenderMode: String = "AI_IMAGES", // "AI_IMAGES", "HTML_CAPTION_ANIMATION"
    val htmlTheme: String = "Cyberpunk Dark",
    val ttsVoice: String = "",
    val whisperModel: String = "small",
    val maxRetries: Int = 3,
    val imageDelayMs: Int = 500,
    val imageDurationSec: Int = 3, // 3s or 6s
    val targetAudience: String = "General",
    val showCaptions: Boolean = true,
    val debugLogging: Boolean = false,
    val pollinationsEnabled: Boolean = true,
    val keyboardName: String = "GLITCH KEYBOARD",
    val keyboardPackage: String = "com.aistudio.glitchv3.trdxk",
    val autoSwitchKeyboard: Boolean = true,
    // Global Fallback & Fail-Safe Engine (Master Toggle)
    val globalFallbackEnabled: Boolean = false,
    // VPN Autopilot Shield Settings & Full Device Internet Cutoff Mode
    val fullDeviceInternetCutoffEnabled: Boolean = false,
    val vpnAutoConnectOnAutopilot: Boolean = false,
    val vpnAutoSwitchOnFailure: Boolean = true,
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_video_settings", Context.MODE_PRIVATE)

    init {
        prefs.edit()
            .remove("api_key")
            .remove("auto_publish_enabled")
            .remove("default_category_id")
            .remove("last_auto_publish_timestamp")
            .remove("next_auto_publish_timestamp")
            .remove("schedule_interval_hours")
            .remove("schedule_delay_seconds")
            .remove("auto_category")
            .remove("smart_auto_pivot_enabled")
            .remove("smart_timing_mode")
            .remove("smart_peak_hour")
            .remove("top_performing_category")
            .apply()
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            serverUrl = prefs.getString("server_url", "http://10.0.2.2:5000") ?: "http://10.0.2.2:5000",
            engineMode = prefs.getString("engine_mode", "Smart Auto") ?: "Smart Auto",
            defaultLanguage = prefs.getString("default_language", "Hindi") ?: "Hindi",
            defaultCategory = prefs.getString("default_category", "Technology") ?: "Technology",
            defaultDuration = prefs.getInt("default_duration", 60),
            defaultStyle = prefs.getString("default_style", "Cinematic") ?: "Cinematic",
            defaultAspect = prefs.getString("default_aspect", "9:16") ?: "9:16",
            visualRenderMode = prefs.getString("visual_render_mode", "AI_IMAGES") ?: "AI_IMAGES",
            htmlTheme = prefs.getString("html_theme", "Cyberpunk Dark") ?: "Cyberpunk Dark",
            ttsVoice = prefs.getString("tts_voice", "") ?: "",
            whisperModel = prefs.getString("whisper_model", "small") ?: "small",
            maxRetries = prefs.getInt("max_retries", 3),
            imageDelayMs = prefs.getInt("image_delay", 500),
            imageDurationSec = prefs.getInt("image_duration_sec", 3),
            targetAudience = prefs.getString("target_audience", "General") ?: "General",
            showCaptions = prefs.getBoolean("show_captions", true),
            debugLogging = prefs.getBoolean("debug_logging", false),
            pollinationsEnabled = prefs.getBoolean("pollinations_enabled", true),
            keyboardName = prefs.getString("keyboard_name", "GLITCH KEYBOARD") ?: "GLITCH KEYBOARD",
            keyboardPackage = prefs.getString("keyboard_package", "com.aistudio.glitchv3.trdxk") ?: "com.aistudio.glitchv3.trdxk",
            autoSwitchKeyboard = prefs.getBoolean("auto_switch_keyboard", true),
            globalFallbackEnabled = prefs.getBoolean("global_fallback_enabled", false),
            fullDeviceInternetCutoffEnabled = prefs.getBoolean("full_device_internet_cutoff_enabled", false),
            vpnAutoConnectOnAutopilot = prefs.getBoolean("vpn_auto_connect_on_autopilot", false),
            vpnAutoSwitchOnFailure = prefs.getBoolean("vpn_auto_switch_on_failure", true)
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putString("server_url", newSettings.serverUrl.trim().removeSuffix("/"))
            putString("engine_mode", newSettings.engineMode)
            putString("default_language", newSettings.defaultLanguage)
            putString("default_category", newSettings.defaultCategory)
            putInt("default_duration", newSettings.defaultDuration)
            putString("default_style", newSettings.defaultStyle)
            putString("default_aspect", newSettings.defaultAspect)
            putString("visual_render_mode", newSettings.visualRenderMode)
            putString("html_theme", newSettings.htmlTheme)
            putString("tts_voice", newSettings.ttsVoice)
            putString("whisper_model", newSettings.whisperModel)
            putInt("max_retries", newSettings.maxRetries)
            putInt("image_delay", newSettings.imageDelayMs)
            putInt("image_duration_sec", newSettings.imageDurationSec)
            putString("target_audience", newSettings.targetAudience)
            putBoolean("show_captions", newSettings.showCaptions)
            putBoolean("debug_logging", newSettings.debugLogging)
            putBoolean("pollinations_enabled", newSettings.pollinationsEnabled)
            putString("keyboard_name", newSettings.keyboardName)
            putString("keyboard_package", newSettings.keyboardPackage)
            putBoolean("auto_switch_keyboard", newSettings.autoSwitchKeyboard)
            putBoolean("global_fallback_enabled", newSettings.globalFallbackEnabled)
            putBoolean("full_device_internet_cutoff_enabled", newSettings.fullDeviceInternetCutoffEnabled)
            putBoolean("vpn_auto_connect_on_autopilot", newSettings.vpnAutoConnectOnAutopilot)
            putBoolean("vpn_auto_switch_on_failure", newSettings.vpnAutoSwitchOnFailure)
            apply()
        }
        _settingsFlow.value = newSettings
    }

    fun getServerUrl(): String {
        return _settingsFlow.value.serverUrl
    }
}
