package com.example.ui.screens

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.MainApplication
import com.example.data.local.MusicTrackEntity
import com.example.media.GlitchTubePlaybackService
import com.example.media.GlitchTubePlaybackState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GlitchTube"
private const val ALL_SONGS = "All songs"
private const val FAVORITES = "Favorites"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlitchTubeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember(context) { (context.applicationContext as MainApplication).database.musicTrackDao() }
    val tracks by dao.observeTracks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(ALL_SONGS) }
    var searchText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var categoryEditTrack by remember { mutableStateOf<MusicTrackEntity?>(null) }
    var playback by remember { mutableStateOf(GlitchTubePlaybackService.getPlaybackState(context)) }

    val importSongs = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        isImporting = true
        scope.launch {
            try {
                val imported = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            readMusicMetadata(context, uri)
                        } catch (exception: Exception) {
                            Log.e(TAG, "Could not import selected music URI: $uri", exception)
                            null
                        }
                    }
                }
                if (imported.isNotEmpty()) dao.insertTracks(imported)
            } finally {
                isImporting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            playback = GlitchTubePlaybackService.getPlaybackState(context)
            delay(700)
        }
    }

    val categories = remember(tracks) {
        listOf(ALL_SONGS, FAVORITES) + tracks.map { it.category }.distinct().sorted()
    }
    val visibleTracks = remember(tracks, selectedCategory, searchText) {
        tracks.filter { track ->
            (selectedCategory == ALL_SONGS ||
                (selectedCategory == FAVORITES && track.isFavorite) ||
                track.category == selectedCategory) &&
                (searchText.isBlank() ||
                    track.title.contains(searchText, ignoreCase = true) ||
                    track.artist.contains(searchText, ignoreCase = true) ||
                    track.album.contains(searchText, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Glitch Tube", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "MUSIC PLAYER",
                            color = CyberCyan,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Button(
                    onClick = { importSongs.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    }
                    Text(
                        if (isImporting) "Importing songs..." else "Add songs from device",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (playback.isPlaying || playback.title.isNotBlank()) {
                item {
                    NowPlayingCard(
                        playback = playback,
                        onPrevious = { GlitchTubePlaybackService.previous(context) },
                        onPlayPause = { GlitchTubePlaybackService.toggle(context) },
                        onNext = { GlitchTubePlaybackService.next(context) },
                        onStop = { GlitchTubePlaybackService.stop(context) }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search songs") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, maxLines = 1) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = CyberCyan)
                    Text(
                        "${visibleTracks.size} songs",
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            val queue = visibleTracks
                            if (queue.isNotEmpty()) {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        dao.updateLastPlayed(queue.first().uri, System.currentTimeMillis())
                                    }
                                }
                                GlitchTubePlaybackService.play(
                                    context = context,
                                    tracks = queue,
                                    startIndex = 0
                                )
                            }
                        },
                        enabled = visibleTracks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Play all")
                    }
                }
            }

            if (visibleTracks.isEmpty()) {
                item {
                    Text(
                        if (tracks.isEmpty()) {
                            "Your local music library is empty. Add audio files from your device; songs and categories are saved on this device."
                        } else {
                            "No songs in this category match your search."
                        },
                        color = TextSecondary
                    )
                }
            }

            items(visibleTracks, key = MusicTrackEntity::uri) { track ->
                MusicTrackRow(
                    track = track,
                    isCurrent = playback.uri == track.uri,
                    onPlay = {
                        val queue = visibleTracks
                        val index = queue.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                dao.updateLastPlayed(track.uri, System.currentTimeMillis())
                            }
                        }
                        GlitchTubePlaybackService.play(context, queue, index)
                    },
                    onFavorite = {
                        scope.launch { withContext(Dispatchers.IO) { dao.updateFavorite(track.uri, !track.isFavorite) } }
                    },
                    onEditCategory = { categoryEditTrack = track },
                    onRemove = {
                        scope.launch { withContext(Dispatchers.IO) { dao.deleteTrack(track.uri) } }
                    }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    categoryEditTrack?.let { track ->
        CategoryEditDialog(
            initialCategory = track.category,
            onDismiss = { categoryEditTrack = null },
            onSave = { category ->
                scope.launch {
                    withContext(Dispatchers.IO) { dao.updateCategory(track.uri, category) }
                    categoryEditTrack = null
                }
            }
        )
    }
}

@Composable
private fun NowPlayingCard(
    playback: GlitchTubePlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(30.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("NOW PLAYING", color = NeonGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(playback.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Close, contentDescription = "Stop playback", tint = TextSecondary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous song", tint = TextPrimary, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Icon(
                        if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Play",
                        tint = CyberCyan,
                        modifier = Modifier.size(42.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next song", tint = TextPrimary, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MusicTrackRow(
    track: MusicTrackEntity,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onEditCategory: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) DarkSurfaceElevated else DarkSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Album, contentDescription = null, tint = if (isCurrent) NeonGreen else CyberCyan, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(track.title, color = if (isCurrent) NeonGreen else TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · ").ifBlank { "Unknown artist" },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(track.category, color = CyberCyan, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (track.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (track.isFavorite) NeonGreen else TextSecondary
                )
            }
            IconButton(onClick = onEditCategory) {
                Icon(Icons.Default.Tune, contentDescription = "Edit song category", tint = TextSecondary)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove song from library", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initialCategory: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song category") },
        text = {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category or playlist") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (category.isNotBlank()) onSave(category.trim()) },
                enabled = category.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun readMusicMetadata(context: android.content.Context, uri: Uri): MusicTrackEntity {
    val resolver = context.contentResolver
    val fallbackTitle = resolver.query(
        uri,
        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.') else null
    }.orEmpty().ifBlank { "Unknown song" }

    val metadata = MediaMetadataRetriever()
    return try {
        metadata.setDataSource(context, uri)
        val title = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().ifBlank { fallbackTitle }
        val artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().ifBlank {
            metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
        }
        val album = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty()
        val duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val category = album.ifBlank { artist.ifBlank { "Uncategorized" } }
        MusicTrackEntity(
            uri = uri.toString(),
            title = title,
            artist = artist,
            album = album,
            category = category,
            durationMs = duration,
            addedAtMs = System.currentTimeMillis()
        )
    } finally {
        metadata.release()
    }
}
