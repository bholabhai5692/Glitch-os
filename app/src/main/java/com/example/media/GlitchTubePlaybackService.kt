package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.MusicTrackEntity

data class GlitchTubePlaybackState(
    val uri: String = "",
    val title: String = "",
    val isPlaying: Boolean = false
)

class GlitchTubePlaybackService : Service() {
    companion object {
        private const val TAG = "GlitchTubePlayback"
        private const val CHANNEL_ID = "glitch_tube_playback"
        private const val NOTIFICATION_ID = 5091
        private const val ACTION_PLAY = "com.example.media.PLAY"
        private const val ACTION_TOGGLE = "com.example.media.TOGGLE"
        private const val ACTION_NEXT = "com.example.media.NEXT"
        private const val ACTION_PREVIOUS = "com.example.media.PREVIOUS"
        private const val ACTION_STOP = "com.example.media.STOP"
        private const val EXTRA_URIS = "playlist_uris"
        private const val EXTRA_TITLES = "playlist_titles"
        private const val EXTRA_INDEX = "playlist_index"
        private const val STATE_PREFERENCES = "glitch_tube_playback"

        fun getPlaybackState(context: Context): GlitchTubePlaybackState {
            val preferences = context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
            return GlitchTubePlaybackState(
                uri = preferences.getString("uri", "").orEmpty(),
                title = preferences.getString("title", "").orEmpty(),
                isPlaying = preferences.getBoolean("is_playing", false)
            )
        }

        fun play(context: Context, tracks: List<MusicTrackEntity>, startIndex: Int) {
            val intent = Intent(context, GlitchTubePlaybackService::class.java).apply {
                action = ACTION_PLAY
                putStringArrayListExtra(EXTRA_URIS, ArrayList(tracks.map { it.uri }))
                putStringArrayListExtra(EXTRA_TITLES, ArrayList(tracks.map { it.title }))
                putExtra(EXTRA_INDEX, startIndex)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun toggle(context: Context) {
            context.startService(Intent(context, GlitchTubePlaybackService::class.java).setAction(ACTION_TOGGLE))
        }

        fun next(context: Context) {
            context.startService(Intent(context, GlitchTubePlaybackService::class.java).setAction(ACTION_NEXT))
        }

        fun previous(context: Context) {
            context.startService(Intent(context, GlitchTubePlaybackService::class.java).setAction(ACTION_PREVIOUS))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, GlitchTubePlaybackService::class.java).setAction(ACTION_STOP))
        }
    }

    private var player: MediaPlayer? = null
    private var playlistUris: List<String> = emptyList()
    private var playlistTitles: List<String> = emptyList()
    private var currentIndex = 0
    private var isPrepared = false
    private lateinit var audioManager: AudioManager
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.takeIf { it.isPlaying }?.pause()
            AudioManager.AUDIOFOCUS_GAIN -> Unit
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit
        }
        player?.let { persistPlaybackState(it.isPlaying) }
        if (playlistUris.isNotEmpty()) updateNotification()
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                playlistUris = intent.getStringArrayListExtra(EXTRA_URIS).orEmpty()
                playlistTitles = intent.getStringArrayListExtra(EXTRA_TITLES).orEmpty()
                currentIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                if (playlistUris.isEmpty() || currentIndex !in playlistUris.indices) {
                    stopPlayback()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification(isPlaying = false))
                prepareTrack()
            }
            ACTION_TOGGLE -> {
                val currentPlayer = player ?: return START_NOT_STICKY
                if (!isPrepared) return START_NOT_STICKY
                if (currentPlayer.isPlaying) {
                    currentPlayer.pause()
                } else {
                    currentPlayer.start()
                }
                persistPlaybackState(currentPlayer.isPlaying)
                updateNotification()
            }
            ACTION_NEXT -> moveToTrack(currentIndex + 1)
            ACTION_PREVIOUS -> moveToTrack(currentIndex - 1)
            ACTION_STOP -> {
                stopPlayback()
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun prepareTrack() {
        releasePlayer()
        val uri = playlistUris[currentIndex]
        persistPlaybackState(isPlaying = false)
        try {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(applicationContext, Uri.parse(uri))
                setOnPreparedListener {
                    isPrepared = true
                    it.start()
                    persistPlaybackState(isPlaying = true)
                    updateNotification()
                }
                setOnCompletionListener {
                    if (currentIndex + 1 < playlistUris.size) {
                        moveToTrack(currentIndex + 1)
                    } else {
                        stopPlayback()
                        stopSelf()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Audio playback failed (what=$what, extra=$extra)")
                    stopPlayback()
                    stopSelf()
                    true
                }
                prepareAsync()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Could not prepare audio playback for $uri", exception)
            stopPlayback()
            stopSelf()
        }
    }

    private fun moveToTrack(index: Int) {
        if (index !in playlistUris.indices) return
        currentIndex = index
        persistPlaybackState(isPlaying = false)
        updateNotification()
        prepareTrack()
    }

    private fun stopPlayback() {
        releasePlayer()
        playlistUris = emptyList()
        playlistTitles = emptyList()
        currentIndex = 0
        getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun releasePlayer() {
        player?.let { currentPlayer ->
            runCatching {
                currentPlayer.setOnPreparedListener(null)
                currentPlayer.setOnCompletionListener(null)
                currentPlayer.setOnErrorListener(null)
                currentPlayer.release()
            }.onFailure { Log.w(TAG, "Could not release audio player cleanly.", it) }
        }
        player = null
        isPrepared = false
        if (::audioManager.isInitialized) {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(player?.isPlaying == true))
    }

    private fun persistPlaybackState(isPlaying: Boolean) {
        val uri = playlistUris.getOrNull(currentIndex).orEmpty()
        val title = playlistTitles.getOrNull(currentIndex).orEmpty()
        getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString("uri", uri)
            .putString("title", title)
            .putBoolean("is_playing", isPlaying)
            .apply()
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, GlitchTubePlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, GlitchTubePlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = playlistTitles.getOrNull(currentIndex) ?: "Glitch Tube"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing in Glitch Tube" else "Glitch Tube audio")
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Resume",
                toggleIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                PendingIntent.getService(
                    this,
                    4,
                    Intent(this, GlitchTubePlaybackService::class.java).setAction(ACTION_NEXT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Glitch Tube playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
