package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String, String) -> Unit
) {
    val details by viewModel.currentProjectDetails.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadProjectDetails(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Video project",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        val project = details?.project
        if (project == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            val story = details?.story
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                        Text(project.title.ifBlank { project.topic }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${project.category} • ${project.duration}s • ${project.language} • ${project.aspectRatio}",
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                        Spacer(Modifier.height(12.dp))
                        if (project.video.isNotBlank()) {
                            NeonButton(
                                text = "WATCH VIDEO",
                                leadingIcon = Icons.Default.PlayArrow,
                                onClick = { onNavigateToPreview(project.video, project.title) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("The rendered video file is not available.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                story?.productionSettings?.let { settings ->
                    item {
                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                            Text("VIDEO SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Voice: ${settings.voice.ifBlank { "Default" }} • Captions: ${if (settings.captions) "On" else "Off"}",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                story?.summary100?.takeIf { it.isNotBlank() }?.let { summary ->
                    item {
                        ContentCard(title = "VIDEO SUMMARY", content = summary)
                    }
                }
                story?.narration?.takeIf { it.isNotBlank() }?.let { narration ->
                    item {
                        ContentCard(title = "NARRATION", content = narration)
                    }
                }

                if (details?.visualPrompts?.isNotEmpty() == true) {
                    item {
                        Text("SCENES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    items(details?.visualPrompts.orEmpty()) { scene ->
                        ContentCard(
                            title = "Scene ${scene.index}: ${scene.purpose}",
                            content = scene.prompt
                        )
                    }
                }

                items(details?.images.orEmpty()) { image ->
                    if (image.url.isNotBlank()) {
                        AsyncImage(
                            model = image.url,
                            contentDescription = image.fileName.ifBlank { "Generated scene image" },
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Spacer(Modifier.height(8.dp))
            Text(content, fontSize = 13.sp, color = TextPrimary)
        }
    }
}
