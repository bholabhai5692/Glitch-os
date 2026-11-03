package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.MainApplication
import com.example.R
import com.example.notification.PipelineNotificationManager
import com.example.pipeline.PipelineExecutionCoordinator
import com.example.pipeline.VideoRequest
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class AutoGenerationLoopWorker(
    appContext: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_TOPIC = "topic"
        const val KEY_CATEGORY = "category"
        const val KEY_LANGUAGE = "language"
        const val KEY_DURATION = "duration_sec"
        const val KEY_STYLE = "style"
        const val KEY_ASPECT = "aspect"
        const val KEY_VOICE = "voice"
        const val KEY_INTERVAL = "interval_minutes"
        const val KEY_CAPTIONS = "captions"

        private const val CHANNEL_ID = "auto_video_loop_channel"
        private const val NOTIFICATION_ID = 2077
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(0, "Preparing next category video")

    override suspend fun doWork(): Result {
        val app = applicationContext as? MainApplication ?: return Result.failure()
        val category = inputData.getString(KEY_CATEGORY).orEmpty().ifBlank { "Technology" }
        val baseTopic = inputData.getString(KEY_TOPIC).orEmpty().ifBlank { category }
        val intervalMinutes = inputData.getInt(KEY_INTERVAL, AutoGenerationLoopManager.MIN_INTERVAL_MINUTES)
        val timeBucket = System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
        val request = VideoRequest(
            topic = "Fresh $category video angle based on $baseTopic, variation $timeBucket",
            language = inputData.getString(KEY_LANGUAGE).orEmpty().ifBlank { "Hindi" },
            category = category,
            targetDurationSec = inputData.getInt(KEY_DURATION, 60).coerceIn(15, 300),
            style = inputData.getString(KEY_STYLE).orEmpty().ifBlank { "Cinematic" },
            aspect = inputData.getString(KEY_ASPECT).orEmpty().ifBlank { "9:16" },
            voiceName = inputData.getString(KEY_VOICE).orEmpty().ifBlank { "hi-IN-MadhurNeural" },
            showCaptions = inputData.getBoolean(KEY_CAPTIONS, true)
        )
        val jobId = "auto_loop_$timeBucket"
        val acquired = PipelineExecutionCoordinator.tryAcquireExecution(
            jobId = jobId,
            title = request.topic,
            isWorkerTask = true
        )
        if (!acquired) return Result.retry()

        PipelineExecutionCoordinator.registerJobHandle(coroutineContext[kotlinx.coroutines.Job]!!)
        return try {
            setForeground(foregroundInfo(0, "Starting ${request.category} video"))
            val result = app.videoRepository.runNativePipeline(request) { progress, status ->
                setProgressAsync(androidx.work.workDataOf("progress" to progress, "status" to status))
                setForegroundAsync(foregroundInfo(progress, status))
            }
            PipelineNotificationManager.showCompletionNotification(
                context = applicationContext,
                title = result.metadata.title,
                category = request.category,
                language = request.language,
                durationSec = request.targetDurationSec
            )
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            PipelineNotificationManager.showErrorNotification(
                context = applicationContext,
                title = "Scheduled video generation failed",
                errorMessage = error.localizedMessage ?: "The scheduled video could not be generated."
            )
            Result.retry()
        } finally {
            PipelineExecutionCoordinator.releaseExecution(jobId)
        }
    }

    private fun foregroundInfo(progress: Int, status: String): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Scheduled video generation")
            .setContentText(status.take(140))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Scheduled video generation", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }
}