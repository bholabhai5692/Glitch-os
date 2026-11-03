package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlitchCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.vpn.VpnGateManager
import com.example.vpn.VpnServer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnShieldScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdProtection: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val servers by VpnGateManager.servers.collectAsState()
    val isLoading by VpnGateManager.isLoading.collectAsState()
    val isShowingCachedProfiles by VpnGateManager.isShowingCachedProfiles.collectAsState()

    LaunchedEffect(Unit) {
        if (servers.isEmpty()) VpnGateManager.fetchServers(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VPN profiles", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { VpnGateManager.fetchServers(context, forceRefresh = true) } },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh live profiles", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlitchCard(borderColor = CyberCyan, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Remote VPN status", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "This app does not contain a remote OpenVPN tunnel. A server ping is not a VPN connection, so Glitch OS will not show a false “connected” status. You can export a VPN Gate profile to a compatible VPN app.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Android allows one active VPN at a time. Glitch OS DNS ad protection uses that VPN slot and cannot run together with a separate VPN app.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = onNavigateToAdProtection, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Shield, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Manage DNS ad protection")
                        }
                    }
                }
            }

            item {
                Text(
                    when {
                        isLoading -> "Loading VPN Gate profiles…"
                        isShowingCachedProfiles -> "PREVIOUSLY FETCHED VPN GATE PROFILES"
                        else -> "VPN GATE COMMUNITY PROFILES"
                    },
                    color = CyberCyan,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (servers.isEmpty() && !isLoading) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Text(
                            "No profiles are available. Connect to the internet and refresh. Only profiles fetched from VPN Gate or a previous live-data cache are shown; no sample servers are used.",
                            modifier = Modifier.padding(18.dp),
                            color = TextSecondary
                        )
                    }
                }
            }

            items(servers, key = { it.ip + it.hostName }) { server ->
                LiveVpnProfileCard(
                    server = server,
                    onExport = { VpnGateManager.shareOvpnFile(context, server) }
                )
            }
        }
    }
}

@Composable
private fun LiveVpnProfileCard(server: VpnServer, onExport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.countryFlag, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(server.countryLong, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${server.hostName} · ${server.ip}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "Feed-reported ping: ${server.pingMs} ms · feed-reported speed: ${server.speedMbps} Mbps",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export profile to a VPN app")
            }
        }
    }
}
