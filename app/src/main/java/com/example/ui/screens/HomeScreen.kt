package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardGradientEnd
import com.example.ui.theme.CardGradientStart
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricViolet
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
    onNavigateToLocation: () -> Unit,
    onNavigateToAdProtection: () -> Unit,
    onNavigateToGlitchTube: () -> Unit
) {
    val dashboard by viewModel.dashboardState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkServerHealth()
        viewModel.loadProjects()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GLITCH OS", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text(
                            "VIDEO STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            letterSpacing = 2.sp
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.6f), ElectricViolet.copy(alpha = 0.45f)))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd)))
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Your next video starts here",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Turn your own idea into a video. Real provider output only—no demo or sample results.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.12f))
                                    .padding(11.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (dashboard.isServerOnline) NeonGreen.copy(alpha = 0.12f) else DarkSurfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (dashboard.isServerOnline) NeonGreen else TextSecondary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (dashboard.isServerOnline) "Video service connected"
                                else dashboard.errorMessage ?: "Online service unavailable; check your connection or provider setup.",
                                color = if (dashboard.isServerOnline) NeonGreen else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onNavigateToCreate,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkBackground
                            )
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Create a video", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                HomeMetricCard(
                    label = "SAVED VIDEOS",
                    value = dashboard.totalVideosGenerated.toString(),
                    icon = Icons.Default.VideoLibrary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionLabel("YOUR WORKSPACE", "Create, review, and manage projects") }
            item { HomeAction("Video library", "Your saved projects", Icons.Default.VideoLibrary, onNavigateToLibrary) }
            item { HomeAction("Glitch Tube", "Play and manage your videos and music", Icons.Default.VideoLibrary, onNavigateToGlitchTube, CyberCyan) }
            item { HomeAction("Creator assistant", "Draft ideas and scripts", Icons.Default.AutoAwesome, onNavigateToCreatorAssistant) }

            item { SectionLabel("PRIVACY & UTILITIES", "Tools you control") }
            item { HomeAction("Ad protection", "DNS filtering, app controls, permissions & stats", Icons.Default.Shield, onNavigateToAdProtection, CyberCyan) }
            item { HomeAction("VPN profiles", "Review live profiles and limitations", Icons.Default.Settings, onNavigateToVpnShield) }
            item { HomeAction("Keyboard tools", "Customize your keyboard", Icons.Default.Keyboard, onNavigateToKeyboardPlayground) }
            item { HomeAction("Location", "View device location", Icons.Default.Place, onNavigateToLocation) }
            item { HomeAction("Recover files", "Scan a disk image you select", Icons.Default.Restore, onNavigateToRecovery) }
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun HomeMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder.copy(alpha = 0.35f)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun HomeAction(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = ElectricViolet
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder.copy(alpha = 0.35f)))
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceElevated).padding(10.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = CyberCyan, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
