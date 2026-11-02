package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.*
import com.example.util.BackgroundPermissionHelper

@Composable
fun BackgroundPermissionsCard(
    modifier: Modifier = Modifier,
    title: String = "Background & Continuous Execution Health",
    isCompact: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionStatus by remember {
        mutableStateOf(BackgroundPermissionHelper.checkStatus(context))
    }
    var expanded by remember { mutableStateOf(!permissionStatus.isAllGranted) }

    // Re-check on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = BackgroundPermissionHelper.checkStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionStatus = BackgroundPermissionHelper.checkStatus(context)
    }

    GlitchCard(
        borderColor = if (permissionStatus.isAllGranted) NeonGreen else WarningAmber,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (permissionStatus.isAllGranted) NeonGreen else WarningAmber)
                    )
                    Column {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (permissionStatus.isAllGranted) NeonGreen else WarningAmber
                        )
                        Text(
                            text = if (permissionStatus.isAllGranted) {
                                "All background services & permissions active"
                            } else {
                                "Battery settings may interrupt video rendering in the background"
                            },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded || !permissionStatus.isAllGranted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = DarkBorder)

                    // 1. Foreground Service & Wake Lock
                    PermissionStatusRow(
                        title = "1. Foreground Service & CPU Wake Lock",
                        description = "Keeps CPU awake during video rendering.",
                        isGranted = true,
                        actionLabel = "Declared in Manifest",
                        onAction = {}
                    )

                    // 2. Notification Permission
                    PermissionStatusRow(
                        title = "2. System Notifications (Android 13+)",
                        description = "Mandatory for Foreground Services & progress tracking.",
                        isGranted = permissionStatus.isNotificationGranted,
                        actionLabel = if (permissionStatus.isNotificationGranted) "Granted" else "Grant Permission",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    // 3. Battery Optimization Exemption
                    PermissionStatusRow(
                        title = "3. Battery Optimization Exemption (Doze Mode)",
                        description = "Prevents Android from killing the app in the background.",
                        isGranted = permissionStatus.isBatteryOptimizationIgnored,
                        actionLabel = if (permissionStatus.isBatteryOptimizationIgnored) "Exempted" else "Disable Battery Saver",
                        onAction = {
                            BackgroundPermissionHelper.requestBatteryOptimizationExemption(context)
                        }
                    )

                    // Quick action buttons
                    if (!permissionStatus.isAllGranted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!permissionStatus.isNotificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else if (!permissionStatus.isBatteryOptimizationIgnored) {
                                        BackgroundPermissionHelper.requestBatteryOptimizationExemption(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Fix Permissions", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    BackgroundPermissionHelper.openAutoStartSettings(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("OEM Auto-Start", color = CyberCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) NeonGreen else WarningAmber,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Text(
                text = description,
                fontSize = 9.5.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 17.dp),
                lineHeight = 13.sp
            )
        }

        if (isGranted) {
            Text(
                text = actionLabel,
                fontSize = 10.sp,
                color = NeonGreen,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 6.dp)
            )
        } else {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 9.5.sp,
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
