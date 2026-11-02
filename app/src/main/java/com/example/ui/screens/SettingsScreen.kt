package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettings
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit,
    hideTopBar: Boolean = false
) {
    val saved by viewModel.settingsFlow.collectAsState()
    var serverUrl by remember(saved.serverUrl) { mutableStateOf(saved.serverUrl) }
    var language by remember(saved.defaultLanguage) { mutableStateOf(saved.defaultLanguage) }
    var category by remember(saved.defaultCategory) { mutableStateOf(saved.defaultCategory) }
    var duration by remember(saved.defaultDuration) { mutableStateOf(saved.defaultDuration.toString()) }
    var engineMode by remember(saved.engineMode) { mutableStateOf(saved.engineMode) }
    var renderMode by remember(saved.visualRenderMode) { mutableStateOf(saved.visualRenderMode) }
    var htmlTheme by remember(saved.htmlTheme) { mutableStateOf(saved.htmlTheme) }
    var captions by remember(saved.showCaptions) { mutableStateOf(saved.showCaptions) }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = { Text("Video settings", color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
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
            Text(
                "Only local video preferences are stored. No sign-in, API-key, channel, or cloud account settings are used.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = engineMode,
                onValueChange = { engineMode = it },
                label = { Text("Engine mode (Smart Auto, On-Device Studio Engine, Remote Server Only)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Video service URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Default language") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Default category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter(Char::isDigit) },
                label = { Text("Default duration in seconds") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = renderMode,
                onValueChange = { renderMode = it },
                label = { Text("Visual render mode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = htmlTheme,
                onValueChange = { htmlTheme = it },
                label = { Text("Animation theme") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = captions, onCheckedChange = { captions = it })
                Text("Generate captions", color = TextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    viewModel.updateAppSettings(
                        saved.copy(
                            serverUrl = serverUrl.trim(),
                            engineMode = engineMode.trim().ifBlank { "Smart Auto" },
                            defaultLanguage = language.trim().ifBlank { "Hindi" },
                            defaultCategory = category.trim().ifBlank { "Technology" },
                            defaultDuration = duration.toIntOrNull()?.coerceIn(15, 600) ?: saved.defaultDuration,
                            visualRenderMode = renderMode.trim().ifBlank { "AI_IMAGES" },
                            htmlTheme = htmlTheme.trim().ifBlank { "Cyberpunk Dark" },
                            showCaptions = captions
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save video settings")
            }
            Button(onClick = onNavigateToLogs, modifier = Modifier.fillMaxWidth()) {
                Text("View video service logs")
            }
        }
    }
}
