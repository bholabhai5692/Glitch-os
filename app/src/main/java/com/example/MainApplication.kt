package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.SettingsManager
import com.example.data.local.VideoDatabase
import com.example.data.network.ApiClient
import com.example.data.repository.VideoRepository
import com.example.notification.PipelineNotificationManager

class MainApplication : Application() {

    lateinit var database: VideoDatabase
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var videoRepository: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val processName = getProcessName()
                if (packageName != processName) {
                    android.webkit.WebView.setDataDirectorySuffix(processName)
                }
            } catch (e: Exception) {
                Log.w("MainApplication", "WebView setDataDirectorySuffix notice: ${e.message}")
            }
        }

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MainApplication", "FATAL EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }

        try {
            val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmCacheDir.exists()) {
                wasmCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.w("MainApplication", "Could not pre-create wasm cache dir", e)
        }

        clearRemovedAccountData()

        database = VideoDatabase.getDatabase(this)
        settingsManager = SettingsManager(this)
        apiClient = ApiClient(settingsManager)
        videoRepository = VideoRepository(this, apiClient, database.projectDao(), settingsManager)

        PipelineNotificationManager.createNotificationChannel(this)
    }

    private fun clearRemovedAccountData() {
        val cleanupPreferences = getSharedPreferences("glitchos_privacy_cleanup", MODE_PRIVATE)
        if (!cleanupPreferences.getBoolean("legacy_channel_data_removed_v1", false)) {
            PipelineNotificationManager.clearNotificationHistory(this)
            cleanupPreferences.edit().putBoolean("legacy_channel_data_removed_v1", true).apply()
        }

        getSharedPreferences("youtube_auth_db", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("ai_video_settings", MODE_PRIVATE).edit()
            .remove("api_key")
            .remove("auto_publish_enabled")
            .remove("default_category_id")
            .apply()

        val sharedPrefsDirectory = java.io.File(applicationInfo.dataDir, "shared_prefs")
        val oldFirebasePrefs = sharedPrefsDirectory.listFiles()
            .orEmpty()
            .filter { it.name.startsWith("com.google.firebase.auth") }
        oldFirebasePrefs.forEach { file ->
            if (!file.delete()) {
                Log.w("MainApplication", "Could not remove legacy account preferences: ${file.name}")
            }
        }

        databaseList()
            .filter { it.startsWith("firestore.") || it == "glitchos_app_database" }
            .forEach { databaseName ->
                if (!deleteDatabase(databaseName)) {
                    Log.w("MainApplication", "Could not remove legacy private database: $databaseName")
                }
            }
    }
}
