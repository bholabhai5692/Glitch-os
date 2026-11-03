package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.vpn.AdControlAccessibilityService
import com.example.vpn.AdProtectionStats
import com.example.vpn.AdProtectionStore
import com.example.vpn.GlitchVpnService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdProtectionScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            context,
            if (granted) "Notifications enabled." else "Notifications remain disabled.",
            Toast.LENGTH_SHORT
        ).show()
    }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (!GlitchVpnService.startAdBlocking(context)) {
                Toast.makeText(context, "Could not start DNS protection.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "VPN permission was not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    var dnsProtectionActive by remember { mutableStateOf(GlitchVpnService.isAdBlocking) }
    var accessibilityEnabled by remember {
        mutableStateOf(AdControlAccessibilityService.isEnabled(context))
    }
    var vpnPermissionGranted by remember {
        mutableStateOf(VpnService.prepare(context) == null)
    }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var stats by remember { mutableStateOf(AdProtectionStore.readStats(context)) }
    var enabledApps by remember {
        mutableStateOf(AdProtectionStore.supportedApps.filter { AdProtectionStore.isAppEnabled(context, it) }.toSet())
    }
    var vpnConflict by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            dnsProtectionActive = GlitchVpnService.isAdBlocking
            accessibilityEnabled = AdControlAccessibilityService.isEnabled(context)
            vpnPermissionGranted = VpnService.prepare(context) == null
            notificationsGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            stats = AdProtectionStore.readStats(context)
            delay(1_000)
        }
    }

    fun otherVpnIsActive(): Boolean {
        if (GlitchVpnService.isAdBlocking) return false
        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java) ?: return false
        return connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    fun toggleDnsProtection() {
        vpnConflict = false
        if (GlitchVpnService.isAdBlocking) {
            GlitchVpnService.stopVpn(context)
            dnsProtectionActive = false
            return
        }
        if (otherVpnIsActive()) {
            vpnConflict = true
            return
        }
        val permissionIntent = VpnService.prepare(context)
        if (permissionIntent != null) {
            vpnPermissionLauncher.launch(permissionIntent)
        } else if (!GlitchVpnService.startAdBlocking(context)) {
            Toast.makeText(context, "Could not start DNS protection.", Toast.LENGTH_LONG).show()
        }
    }

    fun openAccessibilitySettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "Accessibility settings are unavailable.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ad protection", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(DarkSurfaceElevated, DarkSurface)))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("YOUR AD CONTROL CENTER", color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Manage DNS filtering, optional on-screen controls, app access, and real activity counters.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (vpnConflict) {
                            PermissionStatus(
                                title = "Another VPN is active",
                                detail = "Android allows one VPN at a time. Disconnect your other VPN, then start DNS protection.",
                                granted = false
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.6f), DarkBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PermissionStatus(
                            title = "DNS filtering",
                            detail = if (dnsProtectionActive) {
                                "Active. Filters matching DNS domains; it is not an encrypted remote VPN."
                            } else if (vpnPermissionGranted) {
                                "Off. Android VPN permission is ready."
                            } else {
                                "Off. Android will ask before the local DNS filter starts."
                            },
                            granted = dnsProtectionActive
                        )
                        Text(
                            "A separate VPN app cannot run at the same time. Private DNS, encrypted in-app DNS, and first-party ad delivery may bypass domain filtering.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = ::toggleDnsProtection,
                            modifier = Modifier.fillMaxWidth().testTag("ad_protection_dns_toggle"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dnsProtectionActive) ErrorRed else CyberCyan,
                                contentColor = if (dnsProtectionActive) TextPrimary else DarkBackground
                            )
                        ) {
                            Text(
                                if (dnsProtectionActive) "STOP DNS PROTECTION" else "ENABLE DNS PROTECTION",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                StatsCard(stats)
            }

            item {
                SectionTitle("APP CONTROLS", "Choose where accessibility controls may look for ad buttons.")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdProtectionStore.supportedApps.forEach { packageName ->
                        val appName = appName(packageName)
                        AppTargetRow(
                            appName = appName,
                            enabled = packageName in enabledApps,
                            onToggle = { enabled ->
                                AdProtectionStore.setAppEnabled(context, packageName, enabled)
                                enabledApps = if (enabled) enabledApps + packageName else enabledApps - packageName
                            }
                        )
                    }
                }
                Text(
                    "These controls only act on clearly labeled Skip, Close, or Mute buttons when an ad is identifiable. Apps can hide labels or prevent automation; blocking every ad cannot be guaranteed.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                SectionTitle("REQUIRED PERMISSIONS", "Android requires you to approve each protected capability.")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionStatus(
                        title = "VPN permission",
                        detail = if (vpnPermissionGranted) "Granted for this app." else "Needed only to run DNS filtering.",
                        granted = vpnPermissionGranted,
                        icon = Icons.Default.Security
                    )
                    PermissionStatus(
                        title = "Accessibility ad controls",
                        detail = if (accessibilityEnabled) "Enabled in Android Settings." else "Optional. You must enable Glitch OS manually in Accessibility settings.",
                        granted = accessibilityEnabled,
                        icon = Icons.Default.AccessibilityNew
                    )
                    PermissionStatus(
                        title = "Notifications",
                        detail = if (notificationsGranted) "Allowed." else "Optional; needed for ongoing protection status on Android 13+.",
                        granted = notificationsGranted,
                        icon = Icons.Default.NotificationsActive
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = ::openAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth().testTag("ad_protection_accessibility_settings"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                ) {
                    Icon(Icons.Default.AccessibilityNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (accessibilityEnabled) "MANAGE ACCESSIBILITY PERMISSION" else "OPEN ACCESSIBILITY SETTINGS")
                }
                if (Build.VERSION.SDK_INT >= 33 && !notificationsGranted) {
                    Button(
                        onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ALLOW NOTIFICATIONS")
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = ElectricViolet)
                        Text(
                            "Screen labels are inspected locally only while this Accessibility service is enabled. Glitch OS does not save or upload screen content. Android may show a system disclosure when you enable the service.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: AdProtectionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ACTIVITY & TIME SAVED", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Counts update only after a real block or successful button action.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("DNS BLOCKS", stats.blockedDnsLookups.toString(), Modifier.weight(1f))
                MetricTile("SKIPPED", stats.skippedAds.toString(), Modifier.weight(1f))
                MetricTile("CLOSED", stats.closedAds.toString(), Modifier.weight(1f))
            }
            Text(
                "About ${formatDuration(stats.estimatedTimeSavedSeconds)} estimated saved. Estimate uses 5 seconds per successful Skip and 3 seconds per Close; mute actions do not add time.",
                color = NeonGreen,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (stats.mutedAds > 0) {
                Text("${stats.mutedAds} ad(s) muted successfully.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(13.dp)).background(DarkSurfaceElevated).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = CyberCyan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppTargetRow(appName: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(appName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Ad-control target", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("ad_target_${appName.lowercase()}")
            )
        }
    }
}

@Composable
private fun PermissionStatus(
    title: String,
    detail: String,
    granted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon ?: if (granted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = if (granted) NeonGreen else if (icon == null) ErrorRed else CyberCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(detail, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun appName(packageName: String): String = when (packageName) {
    "com.google.android.youtube" -> "YouTube"
    "com.instagram.android" -> "Instagram"
    "com.facebook.katana" -> "Facebook"
    else -> packageName
}

private fun formatDuration(seconds: Long): String {
    if (seconds < 60) return "$seconds sec"
    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    val remainingSeconds = seconds % 60
    if (minutes < 60) return "$minutes min $remainingSeconds sec"
    return "${TimeUnit.MINUTES.toHours(minutes)} hr ${minutes % 60} min"
}
