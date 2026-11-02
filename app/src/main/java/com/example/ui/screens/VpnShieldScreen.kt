package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.vpn.AdControlAccessibilityService
import com.example.vpn.GlitchVpnService
import com.example.vpn.PingQuality
import com.example.vpn.VpnConnectionState
import com.example.vpn.VpnGateManager
import com.example.vpn.VpnServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnShieldScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { com.example.data.local.SettingsManager(context) }
    val appSettings by settingsManager.settingsFlow.collectAsState()

    val servers by VpnGateManager.servers.collectAsState()
    val isLoading by VpnGateManager.isLoading.collectAsState()
    val status by VpnGateManager.status.collectAsState()
    val lastUpdated by VpnGateManager.lastUpdated.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCountryFilter by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("PING") } // PING, SPEED, SCORE

    var pendingServerToConnect by remember { mutableStateOf<VpnServer?>(null) }
    var isAdBlockActive by remember { mutableStateOf(GlitchVpnService.isAdBlocking) }
    var isAdControlEnabled by remember {
        mutableStateOf(AdControlAccessibilityService.isEnabled(context))
    }

    val vpnPrepareLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val s = pendingServerToConnect
            if (s != null) {
                VpnGateManager.connectServer(s, context)
                Toast.makeText(context, "🛡️ VPN Shield Activated for ${s.countryLong}!", Toast.LENGTH_SHORT).show()
            } else {
                VpnGateManager.autoConnectFastest(context)
                Toast.makeText(context, "🛡️ Fastest VPN Shield Activated!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "VPN Permission was denied by user.", Toast.LENGTH_SHORT).show()
        }
        pendingServerToConnect = null
    }

    val adBlockPrepareLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (!GlitchVpnService.startAdBlocking(context)) {
                Toast.makeText(context, "Could not start the DNS blocker.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "VPN permission was denied.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAndConnectVpn(server: VpnServer?) {
        pendingServerToConnect = server
        val prepareIntent = android.net.VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            if (server != null) {
                VpnGateManager.connectServer(server, context)
            } else {
                VpnGateManager.autoConnectFastest(context)
            }
        }
    }

    fun toggleAdBlocker() {
        if (GlitchVpnService.isAdBlocking) {
            GlitchVpnService.stopVpn(context)
            Toast.makeText(context, "DNS blocker stopped.", Toast.LENGTH_SHORT).show()
            return
        }

        val prepareIntent = android.net.VpnService.prepare(context)
        if (prepareIntent != null) {
            adBlockPrepareLauncher.launch(prepareIntent)
        } else if (!GlitchVpnService.startAdBlocking(context)) {
            Toast.makeText(context, "Could not start the DNS blocker.", Toast.LENGTH_LONG).show()
        }
    }

    fun openAccessibilitySettings() {
        try {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Accessibility settings are unavailable on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Uptime timer formatted string
    var sessionDurationSec by remember { mutableStateOf(0L) }
    LaunchedEffect(status.state, status.connectedSinceMs) {
        if (status.state == VpnConnectionState.CONNECTED && status.connectedSinceMs > 0) {
            while (true) {
                sessionDurationSec = (System.currentTimeMillis() - status.connectedSinceMs) / 1000
                delay(1000)
            }
        } else {
            sessionDurationSec = 0L
        }
    }

    LaunchedEffect(Unit) {
        if (servers.isEmpty()) {
            VpnGateManager.fetchServers(context)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isAdBlockActive = GlitchVpnService.isAdBlocking
            isAdControlEnabled = AdControlAccessibilityService.isEnabled(context)
            delay(500)
        }
    }

    // Filter and sort servers
    val filteredServers = remember(servers, searchQuery, selectedCountryFilter, sortBy) {
        var list = servers

        if (selectedCountryFilter != "ALL") {
            list = list.filter { it.countryShort.equals(selectedCountryFilter, ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase().trim()
            list = list.filter {
                it.countryLong.lowercase().contains(q) ||
                it.ip.contains(q) ||
                it.hostName.lowercase().contains(q) ||
                it.operator.lowercase().contains(q)
            }
        }

        when (sortBy) {
            "PING" -> list.sortedBy { it.pingMs }
            "SPEED" -> list.sortedByDescending { it.speedBps }
            "SCORE" -> list.sortedByDescending { it.score }
            else -> list
        }
    }

    // Unique countries in list for filters
    val availableCountries = remember(servers) {
        val set = mutableSetOf("ALL")
        servers.forEach { if (it.countryShort.isNotBlank()) set.add(it.countryShort) }
        set.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "VPN GATE SHIELD",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("vpn_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                VpnGateManager.fetchServers(context, forceRefresh = true)
                                Toast.makeText(context, "VPN Gate CSV Feed Updated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("vpn_refresh_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh CSV Feed",
                                tint = CyberCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Shield Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                VpnStatusHeroCard(
                    status = status,
                    sessionDurationSec = sessionDurationSec,
                    onConnectFastest = {
                        requestAndConnectVpn(null)
                    },
                    onDisconnect = {
                        VpnGateManager.disconnect(context)
                        Toast.makeText(context, "VPN Disconnected", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                GlitchCard(
                    borderColor = if (isAdBlockActive) NeonGreen else CyberCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "VLOCKER DNS AD BLOCKER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAdBlockActive) NeonGreen else CyberCyan
                        )
                        Text(
                            text = "Filters system IPv4 DNS requests with local domain rules. Allowed lookups go to Cloudflare (1.1.1.1) over DNS/UDP; other traffic stays on your normal network. Private DNS, encrypted DNS, and apps using their own DNS resolver may bypass filtering. Android allows only one active VPN.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                        NeonButton(
                            text = if (isAdBlockActive) "STOP DNS BLOCKER" else "START DNS BLOCKER",
                            onClick = ::toggleAdBlocker,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vlocker_dns_toggle")
                        )
                    }
                }
            }

            item {
                GlitchCard(
                    borderColor = if (isAdControlEnabled) NeonGreen else CyberCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "OPTIONAL ACCESSIBILITY AD CONTROLS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isAdControlEnabled) NeonGreen else CyberCyan
                        )
                        Text(
                            text = "With your explicit Android Accessibility permission, Glitch OS checks visible control labels locally. It clicks only clearly labeled “Skip ad”, “Close ad”, or “Dismiss ad” controls; mute/unmute is attempted only with a clear video-ad signal. No screen text is saved or sent. App/player support varies; arbitrary swipes and actions on ordinary videos are intentionally disabled.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                        NeonButton(
                            text = if (isAdControlEnabled) {
                                "ACCESSIBILITY CONTROLS ENABLED — MANAGE"
                            } else {
                                "REVIEW & ENABLE IN ANDROID SETTINGS"
                            },
                            onClick = ::openAccessibilitySettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vlocker_accessibility_settings")
                        )
                    }
                }
            }

            // Autopilot & Failover Automation Controls Card
            item {
                GlitchCard(
                    borderColor = CyberCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⚡", fontSize = 16.sp)
                            Text(
                                text = "AUTOPILOT & FAIL-SAFE SHIELD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CyberCyan
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-activate VPN on Autopilot",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Video generation start hone par VPN automatically active ho jayega.",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    lineHeight = 13.sp
                                )
                            }
                            Switch(
                                checked = appSettings.vpnAutoConnectOnAutopilot,
                                onCheckedChange = { isChecked ->
                                    settingsManager.updateSettings(appSettings.copy(vpnAutoConnectOnAutopilot = isChecked))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                            )
                        }

                        HorizontalDivider(color = DarkBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Switch IP on Failure",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Video generation me network error aane par VPN next available server par switch ho sakta hai.",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    lineHeight = 13.sp
                                )
                            }
                            Switch(
                                checked = appSettings.vpnAutoSwitchOnFailure,
                                onCheckedChange = { isChecked ->
                                    settingsManager.updateSettings(appSettings.copy(vpnAutoSwitchOnFailure = isChecked))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // Search Bar & Sort Row
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country, IP, or node name...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vpn_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            // Country Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableCountries) { countryCode ->
                        val isSelected = selectedCountryFilter == countryCode
                        val flag = if (countryCode == "ALL") "🌐" else VpnServer.countryCodeToEmoji(countryCode)
                        val label = if (countryCode == "ALL") "All Regions (${servers.size})" else "$flag $countryCode"

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCountryFilter = countryCode },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkBorder,
                                selectedBorderColor = CyberCyan
                            )
                        )
                    }
                }
            }

            // Sort & Counter Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Relays (${filteredServers.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort:",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("PING" to "Ping", "SPEED" to "Speed", "SCORE" to "Tsukuba Score")) { (key, label) ->
                                val isSel = sortBy == key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                        .clickable { sortBy = key }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSel) CyberCyan else TextSecondary,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Server List Items
            if (filteredServers.isEmpty()) {
                item {
                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VpnLock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No VPN Gate servers found", color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("Try refreshing the live CSV feed or changing search filters.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(filteredServers, key = { it.ip + it.hostName }) { server ->
                    VpnServerCard(
                        server = server,
                        isConnected = status.state == VpnConnectionState.CONNECTED && status.currentServer?.ip == server.ip,
                        isConnecting = status.state == VpnConnectionState.CONNECTING && status.currentServer?.ip == server.ip,
                        onConnectToggle = {
                            if (status.state == VpnConnectionState.CONNECTED && status.currentServer?.ip == server.ip) {
                                VpnGateManager.disconnect(context)
                            } else {
                                requestAndConnectVpn(server)
                            }
                        },
                        onExportOvpn = {
                            VpnGateManager.shareOvpnFile(context, server)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun VpnStatusHeroCard(
    status: com.example.vpn.VpnShieldStatus,
    sessionDurationSec: Long,
    onConnectFastest: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = status.state == VpnConnectionState.CONNECTED
    val isConnecting = status.state == VpnConnectionState.CONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val borderColor = when {
        isConnected -> NeonGreen
        isConnecting -> WarningAmber
        else -> CyberCyan.copy(alpha = 0.5f)
    }

    GlitchCard(
        borderColor = borderColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shield Icon & Status Text
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected -> NeonGreen.copy(alpha = 0.18f)
                            isConnecting -> WarningAmber.copy(alpha = 0.18f)
                            else -> CyberCyan.copy(alpha = 0.12f)
                        }
                    )
                    .border(
                        2.dp,
                        when {
                            isConnected -> NeonGreen
                            isConnecting -> WarningAmber
                            else -> CyberCyan
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Shield else if (isConnecting) Icons.Default.HourglassTop else Icons.Default.VpnLock,
                    contentDescription = null,
                    tint = when {
                        isConnected -> NeonGreen
                        isConnecting -> WarningAmber
                        else -> CyberCyan
                    },
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when (status.state) {
                    VpnConnectionState.CONNECTED -> "SECURE SHIELD ACTIVE"
                    VpnConnectionState.CONNECTING -> "ESTABLISHING ENCRYPTED TUNNEL..."
                    VpnConnectionState.DISCONNECTING -> "DISCONNECTING..."
                    else -> "VPN SHIELD DISCONNECTED"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = when (status.state) {
                    VpnConnectionState.CONNECTED -> NeonGreen
                    VpnConnectionState.CONNECTING -> WarningAmber
                    else -> TextPrimary
                }
            )

            if (isConnected && status.currentServer != null) {
                val s = status.currentServer
                Text(
                    text = "${s.countryFlag} Connected to ${s.countryLong} (${s.ip})",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("UPTIME", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        val mins = sessionDurationSec / 60
                        val secs = sessionDurationSec % 60
                        Text(String.format("%02d:%02d", mins, secs), fontSize = 13.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LATENCY", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("${s.pingMs} ms", fontSize = 13.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPEED", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("${status.downloadSpeedMbps} Mb/s", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ENCRYPTION", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Text("AES-256", fontSize = 13.sp, color = ElectricViolet, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(ErrorRed, NeonPink))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vpn_disconnect_button")
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DISCONNECT SHIELD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Text(
                    text = "Browse more privately and switch network servers with one tap.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonButton(
                    text = if (isConnecting) "CONNECTING..." else "⚡ 1-TAP SMART CONNECT FASTEST RELAY",
                    onClick = onConnectFastest,
                    enabled = !isConnecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vpn_smart_connect_button")
                )
            }
        }
    }
}

@Composable
fun VpnServerCard(
    server: VpnServer,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnectToggle: () -> Unit,
    onExportOvpn: () -> Unit
) {
    GlitchCard(
        borderColor = if (isConnected) NeonGreen else if (isConnecting) WarningAmber else DarkBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = server.countryFlag, fontSize = 22.sp)
                    Column {
                        Text(
                            text = server.countryLong,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = server.ip,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Ping & Speed Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val pingColor = when (server.pingRating) {
                        PingQuality.EXCELLENT -> NeonGreen
                        PingQuality.GOOD -> CyberCyan
                        PingQuality.FAIR -> WarningAmber
                        PingQuality.HIGH_LATENCY -> ErrorRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pingColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${server.pingMs} ms",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = pingColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricViolet.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${server.speedMbps} Mb/s",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet
                        )
                    }
                }
            }

            // Operator info / Sessions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operator: ${server.operator.ifBlank { "Community Node" }}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${server.numVpnSessions} active sessions",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            // Actions: Connect Button & Export .ovpn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnectToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) ErrorRed.copy(alpha = 0.8f) else if (isConnecting) WarningAmber else CyberCyan.copy(alpha = 0.2f),
                        contentColor = if (isConnected) Color.White else if (isConnecting) DarkBackground else CyberCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Stop else if (isConnecting) Icons.Default.HourglassTop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "DISCONNECT" else if (isConnecting) "CONNECTING..." else "CONNECT RELAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onExportOvpn,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(DarkBorder, TextMuted))),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export .ovpn", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(".OVPN", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
