package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val folder = remember {
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
            "GlitchVoiceRecordings"
        )
    }
    var recordings by remember { mutableStateOf(emptyList<File>()) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeRecording by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var status by remember { mutableStateOf("Recordings are saved only in this app's private folder.") }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun reloadRecordings() {
        recordings = folder.listFiles()
            ?.filter { it.isFile && it.extension.equals("m4a", ignoreCase = true) }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()
    }

    fun stopPlayback() {
        player?.runCatching { stop() }
        player?.release()
        player = null
        playingFile = null
        isPlaying = false
    }

    fun stopRecording() {
        val active = recorder ?: return
        var saved = false
        try {
            active.stop()
            saved = true
        } catch (_: RuntimeException) {
            activeRecording?.delete()
        } finally {
            active.release()
            recorder = null
            isRecording = false
            val savedFile = activeRecording
            activeRecording = null
            elapsedSeconds = 0
            if (saved && savedFile != null) {
                status = "Recording saved: ${savedFile.name}"
            } else {
                status = "Recording was too short or could not be saved."
            }
            reloadRecordings()
        }
    }

    fun startRecording() {
        if (!folder.exists() && !folder.mkdirs()) {
            status = "Could not create the private recordings folder."
            return
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val output = File(folder, "Voice_$timestamp.m4a")
        var pendingRecorder: MediaRecorder? = null
        try {
            val newRecorder = MediaRecorder()
            pendingRecorder = newRecorder
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(128_000)
            newRecorder.setAudioSamplingRate(44_100)
            newRecorder.setOutputFile(output.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            pendingRecorder = null
            activeRecording = output
            elapsedSeconds = 0
            isRecording = true
            status = "Recording microphone audio. Recording is visible on this screen."
        } catch (error: Exception) {
            output.delete()
            pendingRecorder?.release()
            recorder = null
            activeRecording = null
            status = "Could not start recording: ${error.localizedMessage ?: "microphone unavailable"}"
        }
    }

    fun playRecording(file: File) {
        if (playingFile == file && player != null) {
            if (isPlaying) {
                player?.pause()
                isPlaying = false
            } else {
                player?.start()
                isPlaying = true
            }
            return
        }
        stopPlayback()
        try {
            val newPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    player = null
                    playingFile = null
                    isPlaying = false
                }
                prepare()
                start()
            }
            player = newPlayer
            playingFile = file
            isPlaying = true
            status = "Playing ${file.name}"
        } catch (error: Exception) {
            stopPlayback()
            status = "Could not play recording: ${error.localizedMessage ?: "audio error"}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) startRecording() else status = "Microphone permission is required to record."
    }

    LaunchedEffect(Unit) { reloadRecordings() }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(1_000)
                elapsedSeconds++
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            recorder?.let { active ->
                runCatching { active.stop() }
                active.release()
                activeRecording?.delete()
            }
            player?.release()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isRecording) stopRecording()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Recorder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isRecording) Color.Red else CyberCyan,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        if (isRecording) "Recording in progress" else "Microphone recorder",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        if (isRecording) formatDuration(elapsedSeconds) else "Start and stop recording whenever you choose.",
                        color = if (isRecording) NeonGreen else TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isRecording) {
                        Button(onClick = ::stopRecording) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Text("  Stop and save")
                        }
                    } else {
                        Button(onClick = {
                            stopPlayback()
                            if (hasMicPermission) startRecording()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Text("  Start recording")
                        }
                    }
                    Text(
                        "Records microphone input only—not phone-call audio. Get consent from everyone present before recording. The active state is shown here.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("YOUR RECORDINGS", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("${recordings.size} files · private app folder", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                TextButtonWithFolder(path = folder.absolutePath)
            }

            Text(status, color = TextSecondary, style = MaterialTheme.typography.bodySmall)

            if (recordings.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No recordings yet", color = TextPrimary)
                        Text("Your saved microphone recordings will appear here.", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recordings, key = File::getAbsolutePath) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(file.name, color = TextPrimary, maxLines = 1)
                                    Text(
                                        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
                                            .format(Date(file.lastModified())),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = { playRecording(file) }) {
                                    Icon(
                                        if (playingFile == file && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playingFile == file && isPlaying) "Pause" else "Play",
                                        tint = CyberCyan
                                    )
                                }
                                IconButton(onClick = { deleteTarget = file }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete recording", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete recording?") },
            text = { Text("This permanently removes ${file.name} from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    if (playingFile == file) stopPlayback()
                    if (!file.delete()) status = "Could not delete the recording."
                    else status = "Recording deleted."
                    reloadRecordings()
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TextButtonWithFolder(path: String) {
    Text(
        path,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        color = CyberCyan,
        style = MaterialTheme.typography.labelSmall
    )
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}
