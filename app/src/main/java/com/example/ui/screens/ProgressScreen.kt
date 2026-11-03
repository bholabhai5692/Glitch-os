package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.JobStatusResponse
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.components.StepIndicator
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import com.example.util.SocialShareHelper
import com.example.ui.viewmodels.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    jobId: String,
    viewModel: MainViewModel,
    onNavigateToPreview: (String, String) -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val progressState by viewModel.currentJobProgress.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(jobId) {
        viewModel.startPollingJob(jobId)
    }

    val status = progressState?.status ?: "processing"
    val progress = progressState?.progress ?: 5
    val currentStep = progressState?.step ?: "Initializing Studio Pipeline..."
    val isCompleted = status == "completed"
    val isFailed = status == "failed"
    val isPausedOffline = !isOnline || currentStep.contains("PAUSED_OFFLINE") || currentStep.contains("WAITING_FOR_INTERNET")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pipeline Execution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circular Progress Indicator
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(
                            width = 2.dp,
                            color = when {
                                isCompleted -> NeonGreen
                                isFailed -> ErrorRed
                                isPausedOffline -> WarningAmber.copy(alpha = pulseAlpha)
                                else -> CyberCyan.copy(alpha = pulseAlpha)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$progress%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isCompleted -> NeonGreen
                                isFailed -> ErrorRed
                                isPausedOffline -> WarningAmber
                                else -> CyberCyan
                            }
                        )
                        Text(
                            text = when {
                                isCompleted -> "READY"
                                isFailed -> "FAILED"
                                isPausedOffline -> "PAUSED (OFFLINE)"
                                else -> "GENERATING"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPausedOffline) WarningAmber else TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Current Step Box
            item {
                GlitchCard(
                    borderColor = if (isCompleted) NeonGreen.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CURRENT TASK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentStep,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Start
                    )

                    if (progressState?.currentImage != null && (progressState?.totalImages ?: 0) > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (progressState?.currentImage ?: 0).toFloat() / (progressState?.totalImages ?: 1).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberCyan,
                            trackColor = DarkBorder
                        )
                        Text(
                            text = "${progressState?.currentImage ?: 0} / ${progressState?.totalImages ?: 0} images ready",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            val imagePreviews = progressState?.imagePreviews.orEmpty()
            if (imagePreviews.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "LIVE SCENE PREVIEW (${imagePreviews.size})",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(imagePreviews.takeLast(8), key = { it }) { imagePath ->
                                AsyncImage(
                                    model = File(imagePath),
                                    contentDescription = "Generated video scene",
                                    modifier = Modifier
                                        .size(width = 112.dp, height = 190.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceElevated)
                                )
                            }
                        }
                    }
                }
            }

            // Pipeline 7 Steps Checklist
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PIPELINE PHASES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val steps = listOf(
                        "Step 1: Script & Viral Storyboard via AI Engine" to (progress >= 10),
                        "Step 2: Neural Voiceover Narration" to (progress >= 20),
                        "Step 3: Duration & Scene Breakdown" to (progress >= 25),
                        "Step 4: AI Visual Scene Prompts" to (progress >= 30),
                        "Step 5: Sequential High-Def Image Generation" to (progress >= 75),
                        "Step 6: Synced Subtitle Captions" to (progress >= 85),
                        "Step 7: Video Rendering & Muxing" to (progress >= 95),
                        "Step 8: Final Validation & Thumbnail" to (progress >= 100)
                    )

                    steps.forEachIndexed { idx, (name, completed) ->
                        val active = !completed && (idx == 0 || steps[idx - 1].second) && !isCompleted && !isFailed
                        StepIndicator(
                            stepName = name,
                            isActive = active,
                            isCompleted = completed,
                            stepNumber = idx + 1
                        )
                        if (idx < steps.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Actions (View or Cancel)
            item {
                if (isCompleted) {
                    val videoUrl = progressState?.output?.videoUrl?.takeIf { it.isNotBlank() }
                    if (videoUrl != null) {
                        val output = progressState?.output
                        val videoTitle = output?.title.orEmpty()
                        val description = output?.description.orEmpty()
                        val summary = output?.summary.orEmpty()
                        val category = output?.category.orEmpty()
                        val metadataText = listOf(videoTitle, description, summary)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString("\n\n")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            VideoPlayerView(
                                videoUri = Uri.parse(videoUrl),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceElevated)
                            )
                            NeonButton(
                                text = "WATCH FULL VIDEO",
                                leadingIcon = Icons.Default.PlayArrow,
                                color = NeonGreen,
                                onClick = { onNavigateToPreview(videoUrl, videoTitle) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("watch_video_button")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        SocialShareHelper.shareVideo(
                                            context = context,
                                            videoPath = videoUrl,
                                            title = videoTitle,
                                            caption = listOf(description, summary).filter { it.isNotBlank() }.joinToString("\n\n"),
                                            hashtags = category.takeIf { it.isNotBlank() }?.let { "#${it.replace(Regex("\\s+"), "")}" }.orEmpty()
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share + copy")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Video metadata", metadataText))
                                        Toast.makeText(context, "Video metadata copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = metadataText.isNotBlank()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy metadata")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Generation completed, but no playable video URL was returned.",
                            color = WarningAmber,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (isFailed) {
                    NeonButton(
                        text = "RETURN HOME",
                        color = ErrorRed,
                        onClick = onNavigateHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("failed_return_home_button")
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            viewModel.cancelActiveJob(jobId)
                            onNavigateHome()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_job_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorRed
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Generation")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
