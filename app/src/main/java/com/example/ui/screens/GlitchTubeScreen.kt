package com.example.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.example.media.GlitchTubePlaybackService
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GlitchTube"
private const val STORE_NAME = "glitch_tube_library"
private const val STORE_TRACKS = "tracks"
private val SUPPORTED_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "mp4", "m4v", "webm", "3gp")

private data class TubeTrack(
    val uri: String,
    val title: String,
    val mimeType: String,
    val downloadId: Long? = null,
    val downloadStatus: String? = null
) {
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/") ||
            title.substringAfterLast('.', "").lowercase() in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac")

    val isDownloaded: Boolean
        get() = uri.isNotBlank() && downloadId == null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlitchTubeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf(readTracks(context)) }
    var selectedVideo by remember { mutableStateOf<TubeTrack?>(null) }
    var mediaUrl by remember { mutableStateOf("") }
    var downloadRightsConfirmed by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val imported = uris.mapNotNull { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (exception: SecurityException) {
                    Log.w(TAG, "Could not persist permission for selected media.", exception)
                }
                val mime = context.contentResolver.getType(uri) ?: return@mapNotNull null
                val title = queryMediaTitle(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Media"
                TubeTrack(uri.toString(), title, mime)
            }
            tracks = (tracks + imported).distinctBy(TubeTrack::uri)
            writeTracks(context, tracks)
            downloadMessage = "Added ${imported.size} media file(s) from your device."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val downloadManager = context.getSystemService(DownloadManager::class.java)
            var changed = false
            tracks = tracks.map { track ->
                val id = track.downloadId ?: return@map track
                val query = DownloadManager.Query().setFilterById(id)
                downloadManager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        changed = true
                        track.copy(downloadStatus = "Download no longer exists", downloadId = null)
                    } else {
                        when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val localUri = downloadManager.getUriForDownloadedFile(id)
                                changed = true
                                if (localUri != null) {
                                    track.copy(
                                        uri = localUri.toString(),
                                        downloadId = null,
                                        downloadStatus = "Downloaded"
                                    )
                                } else {
                                    track.copy(downloadId = null, downloadStatus = "Download finished, but its file could not be opened")
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                changed = true
                                track.copy(downloadId = null, downloadStatus = "Download failed ($reason)")
                            }
                            else -> track.copy(downloadStatus = "Downloading…")
                        }
                    }
                }
            }
            if (changed) writeTracks(context, tracks)
            delay(1_500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Glitch Tube", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = { mediaPicker.launch(arrayOf("video/*", "audio/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Text("Add videos and music from this device", modifier = Modifier.padding(start = 8.dp))
                }
            }

            item {
                DirectDownloadCard(
                    url = mediaUrl,
                    onUrlChange = { mediaUrl = it },
                    rightsConfirmed = downloadRightsConfirmed,
                    onRightsChange = { downloadRightsConfirmed = it },
                    message = downloadMessage,
                    onDownload = {
                        val validated = validateDirectMediaUrl(mediaUrl)
                        if (validated == null) {
                            downloadMessage = "Enter a direct HTTPS link to an audio or video file. Streaming pages are not supported."
                        } else if (!downloadRightsConfirmed) {
                            downloadMessage = "Confirm that you own this media or have permission to download it."
                        } else {
                            try {
                                val filename = safeFileName(URLUtil.guessFileName(validated, null, null))
                                val request = DownloadManager.Request(Uri.parse(validated))
                                    .setTitle(filename)
                                    .setDescription("Glitch Tube authorized media download")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setAllowedOverMetered(true)
                                    .setDestinationInExternalFilesDir(
                                        context,
                                        Environment.DIRECTORY_DOWNLOADS,
                                        "GlitchTube_$filename"
                                    )
                                val id = context.getSystemService(DownloadManager::class.java).enqueue(request)
                                val mime = guessMimeType(filename)
                                tracks = tracks + TubeTrack(
                                    uri = "",
                                    title = filename,
                                    mimeType = mime,
                                    downloadId = id,
                                    downloadStatus = "Waiting to download…"
                                )
                                writeTracks(context, tracks)
                                downloadMessage = "Download started. The file will appear in your library when complete."
                                mediaUrl = ""
                                downloadRightsConfirmed = false
                            } catch (exception: Exception) {
                                downloadMessage = "Could not start download: ${exception.localizedMessage ?: "unknown error"}"
                                Log.e(TAG, "Could not start the authorized direct-media download.", exception)
                            }
                        }
                    }
                )
            }

            selectedVideo?.let { video ->
                item(key = "selected_video") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(video.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TubeVideoPlayer(video.uri, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("YOUR LIBRARY", color = CyberCyan, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (isAudioPlaying) {
                        IconButton(onClick = {
                            GlitchTubePlaybackService.toggle(context)
                            isAudioPlaying = false
                        }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause background music", tint = CyberCyan)
                        }
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Text(
                        "Your library is empty. Add media files from your device or download a direct link to media you are authorized to use.",
                        color = TextSecondary
                    )
                }
            }

            itemsIndexed(
                tracks,
                key = { index, track ->
                    track.downloadId?.let { id -> "download:$id" }
                        ?: track.uri.ifBlank { "unavailable:$index:${track.title}" }
                }
            ) { _, track ->
                TubeTrackRow(
                    track = track,
                    onPlay = {
                        if (track.downloadId != null || track.uri.isBlank()) {
                            downloadMessage = track.downloadStatus ?: "This download is not ready yet."
                        } else if (track.isAudio) {
                            val playlist = tracks.filter { it.isAudio && it.isDownloaded }
                            val index = playlist.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
                            GlitchTubePlaybackService.play(
                                context,
                                playlist.map(TubeTrack::uri),
                                playlist.map(TubeTrack::title),
                                index
                            )
                            isAudioPlaying = true
                        } else {
                            selectedVideo = track
                        }
                    },
                    onRemove = {
                        tracks = tracks.filterNot { it == track }
                        if (selectedVideo == track) selectedVideo = null
                        writeTracks(context, tracks)
                    }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DirectDownloadCard(
    url: String,
    onUrlChange: (String) -> Unit,
    rightsConfirmed: Boolean,
    onRightsChange: (Boolean) -> Unit,
    message: String?,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Download authorized media", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                "Direct audio/video file links only. Video-platform pages and yt-dlp extraction are not supported. Use downloads only for media you own or are licensed to download.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Direct media URL") },
                placeholder = { Text("https://example.com/song.mp3") },
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rightsConfirmed, onCheckedChange = onRightsChange)
                Text(
                    "I own this media or have permission to download it",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text("Download", modifier = Modifier.padding(start = 8.dp))
            }
            if (!message.isNullOrBlank()) {
                Text(message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TubeTrackRow(track: TubeTrack, onPlay: () -> Unit, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (track.isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
                contentDescription = null,
                tint = if (track.isAudio) NeonGreen else CyberCyan,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(track.title, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    track.downloadStatus ?: if (track.isAudio) "Audio · background playback" else "Video",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onPlay, enabled = track.downloadId == null) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = CyberCyan)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Remove from library", tint = DarkSurfaceElevated)
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun TubeVideoPlayer(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { viewContext -> PlayerView(viewContext).apply { this.player = player } },
        modifier = modifier.background(androidx.compose.ui.graphics.Color.Black)
    )
}

private fun validateDirectMediaUrl(value: String): String? {
    val uri = runCatching { Uri.parse(value.trim()) }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host.isNullOrBlank() || !uri.userInfo.isNullOrBlank()) return null
    val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: return null
    if (extension !in SUPPORTED_EXTENSIONS) return null
    val host = uri.host!!.lowercase()
    val platformHosts = listOf("youtube.com", "youtu.be", "instagram.com", "facebook.com", "tiktok.com", "soundcloud.com")
    if (platformHosts.any { host == it || host.endsWith(".$it") }) return null
    return uri.toString()
}

private fun safeFileName(value: String): String {
    val cleaned = value.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
    return cleaned.ifBlank { "glitch_tube_media" }
}

private fun guessMimeType(filename: String): String = when (filename.substringAfterLast('.', "").lowercase()) {
    "mp3" -> "audio/mpeg"
    "m4a" -> "audio/mp4"
    "aac" -> "audio/aac"
    "wav" -> "audio/wav"
    "ogg" -> "audio/ogg"
    "flac" -> "audio/flac"
    "webm" -> "video/webm"
    "3gp" -> "video/3gpp"
    else -> "video/mp4"
}

private fun readTracks(context: Context): List<TubeTrack> {
    val raw = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE).getString(STORE_TRACKS, null)
        ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TubeTrack(
                uri = item.optString("uri"),
                title = item.optString("title"),
                mimeType = item.optString("mimeType", "application/octet-stream"),
                downloadId = if (item.has("downloadId") && !item.isNull("downloadId")) item.getLong("downloadId") else null,
                downloadStatus = item.optString("downloadStatus").takeIf(String::isNotBlank)
            )
        }
    }.getOrElse {
        Log.e(TAG, "Could not read the saved Glitch Tube library.", it)
        emptyList()
    }
}

private fun writeTracks(context: Context, tracks: List<TubeTrack>) {
    val array = JSONArray()
    tracks.forEach { track ->
        array.put(
            JSONObject()
                .put("uri", track.uri)
                .put("title", track.title)
                .put("mimeType", track.mimeType)
                .put("downloadId", track.downloadId)
                .put("downloadStatus", track.downloadStatus.orEmpty())
        )
    }
    context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(STORE_TRACKS, array.toString())
        .apply()
}

private fun queryMediaTitle(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
