package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CreateJobRequest
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVideoScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProgress: (String) -> Unit
) {
    val settings by viewModel.settingsFlow.collectAsState()
    val jobState by viewModel.createJobState.collectAsState()
    var topic by remember { mutableStateOf("") }
    var language by remember(settings.defaultLanguage) { mutableStateOf(settings.defaultLanguage) }
    var category by remember(settings.defaultCategory) { mutableStateOf(settings.defaultCategory) }
    var style by remember(settings.defaultStyle) { mutableStateOf(settings.defaultStyle) }
    var aspect by remember(settings.defaultAspect) { mutableStateOf(settings.defaultAspect) }
    var duration by remember(settings.defaultDuration) { mutableStateOf(settings.defaultDuration.toString()) }
    var captions by remember(settings.showCaptions) { mutableStateOf(settings.showCaptions) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create video", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Video topic") },
                modifier = Modifier.fillMaxWidth().testTag("video_topic_field")
            )
            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Language") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = style,
                onValueChange = { style = it },
                label = { Text("Visual style") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = aspect,
                onValueChange = { aspect = it },
                label = { Text("Aspect ratio (for example 9:16)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter(Char::isDigit) },
                label = { Text("Duration in seconds") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = captions, onCheckedChange = { captions = it })
                Text("Generate captions", color = TextPrimary)
            }
            jobState.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    viewModel.createVideo(
                        CreateJobRequest(
                            topic = topic.trim(),
                            language = language.trim().ifBlank { "Hindi" },
                            category = category.trim().ifBlank { "Technology" },
                            durationSec = duration.toIntOrNull()?.coerceIn(15, 600) ?: 60,
                            style = style.trim().ifBlank { "Cinematic" },
                            aspectRatio = aspect.trim().ifBlank { "9:16" },
                            showCaptions = captions
                        ),
                        onNavigateToProgress
                    )
                },
                enabled = !jobState.isSubmitting && topic.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("create_video_button")
            ) {
                Text(if (jobState.isSubmitting) "Starting..." else "Generate video")
            }
        }
    }
}
