package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.model.CreateJobRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class AutoGenerationLoopStatus(
    val isScheduled: Boolean = false,
    val intervalMinutes: Int = AutoGenerationLoopManager.MIN_INTERVAL_MINUTES
)

object AutoGenerationLoopManager {
    const val UNIQUE_WORK_NAME = "glitch_category_video_loop"
    const val MIN_INTERVAL_MINUTES = 15
    const val MAX_INTERVAL_MINUTES = 24 * 60
    private const val PREFERENCES = "auto_generation_loop"
    private const val KEY_INTERVAL = "interval_minutes"

    fun schedule(context: Context, request: CreateJobRequest, intervalMinutes: Int) {
        val interval = intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
        val input = Data.Builder()
            .putString(AutoGenerationLoopWorker.KEY_TOPIC, request.topic)
            .putString(AutoGenerationLoopWorker.KEY_CATEGORY, request.category)
            .putString(AutoGenerationLoopWorker.KEY_LANGUAGE, request.language)
            .putInt(AutoGenerationLoopWorker.KEY_DURATION, request.durationSec)
            .putString(AutoGenerationLoopWorker.KEY_STYLE, request.style)
            .putString(AutoGenerationLoopWorker.KEY_ASPECT, request.aspectRatio)
            .putString(AutoGenerationLoopWorker.KEY_VOICE, request.voiceName)
            .putInt(AutoGenerationLoopWorker.KEY_INTERVAL, interval)
            .putBoolean(AutoGenerationLoopWorker.KEY_CAPTIONS, request.showCaptions)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val work = PeriodicWorkRequestBuilder<AutoGenerationLoopWorker>(interval.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL, interval)
            .apply()
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_INTERVAL)
            .apply()
    }

    suspend fun getStatus(context: Context): AutoGenerationLoopStatus = withContext(Dispatchers.IO) {
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME)
            .get()
        val active = work.firstOrNull {
            it.state == WorkInfo.State.ENQUEUED ||
                it.state == WorkInfo.State.RUNNING ||
                it.state == WorkInfo.State.BLOCKED
        }
        AutoGenerationLoopStatus(
            isScheduled = active != null,
            intervalMinutes = if (active != null) {
                context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                    .getInt(KEY_INTERVAL, MIN_INTERVAL_MINUTES)
            } else {
                MIN_INTERVAL_MINUTES
            }
        )
    }
}