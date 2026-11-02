package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyboardPlayground: () -> Unit,
    onNavigateToVpnShield: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onNavigateToCreatorAssistant: () -> Unit,
    onNavigateToLocation: () -> Unit
) {
    val dashboard by viewModel.dashboardState.collectAsState()
    val online by viewModel.isOnline.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkServerHealth()
        viewModel.loadProjects()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Glitch OS Video Studio", color = TextPrimary) }) },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Create videos from your own topic", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Video generation runs with the configured online providers or the on-device engine. No sample or simulated results are used.",
                            color = TextSecondary
                        )
                        Text(
                            if (online) "Video service connected" else dashboard.errorMessage ?: "Online video service unavailable; on-device generation may still be available.",
                            color = if (online) NeonGreen else TextSecondary
                        )
                        Text("Saved videos: ${dashboard.totalVideosGenerated}", color = TextSecondary)
                        Button(onClick = onNavigateToCreate, modifier = Modifier.fillMaxWidth()) {
                            Text("Create a video")
                        }
                    }
                }
            }
            item { HomeAction("Video library", onNavigateToLibrary) }
            item { HomeAction("Video settings", onNavigateToSettings) }
            item { HomeAction("Keyboard tools", onNavigateToKeyboardPlayground) }
            item { HomeAction("VPN settings", onNavigateToVpnShield) }
            item { HomeAction("Location", onNavigateToLocation, Icons.Default.Place) }
            item { HomeAction("Creator assistant", onNavigateToCreatorAssistant) }
            item {
                HomeAction(
                    label = "Recover files from disk image",
                    onClick = onNavigateToRecovery,
                    leadingIcon = Icons.Default.Restore
                )
            }
        }
    }
}

@Composable
private fun HomeAction(label: String, onClick: () -> Unit, leadingIcon: ImageVector? = null) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(label)
    }
}
