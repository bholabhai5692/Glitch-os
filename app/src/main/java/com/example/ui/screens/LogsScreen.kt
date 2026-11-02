package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.NotificationHistoryItem
import com.example.notification.PipelineNotificationManager
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val logs by viewModel.serverLogs.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Notifications, 1: Server Logs
    var notificationHistory by remember { mutableStateOf(emptyList<NotificationHistoryItem>()) }

    fun refreshHistory() {
        notificationHistory = PipelineNotificationManager.getNotificationHistory(context)
    }

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
        refreshHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "System & Notification Logs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("logs_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = {
                            PipelineNotificationManager.clearNotificationHistory(context)
                            refreshHistory()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Notifications",
                                tint = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.loadLogs()
                        refreshHistory()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Logs",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurfaceElevated,
                contentColor = CyberCyan
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NOTIFICATIONS (${notificationHistory.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SERVER LOGS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // Notifications History Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (notificationHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No video generation notifications yet.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        items(notificationHistory) { item ->
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                            val isError = item.type == "ERROR"
                            val cardBorderColor = if (isError) ErrorRed else CyberCyan

                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isError) "⚠️ GENERATION ERROR" else "✅ VIDEO GENERATED",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isError) ErrorRed else CyberCyan
                                        )
                                        Text(
                                            text = dateStr,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )

                                    if (!item.message.isBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.message,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    // Display detailed metadata fields
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (!item.category.isNullOrBlank()) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = {
                                                    Text("📁 ${item.category}", fontSize = 10.sp)
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkBackground)
                                            )
                                        }
                                        if (item.durationSec != null) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("⏱ ${item.durationSec}s", fontSize = 10.sp) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkBackground)
                                            )
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            } else {
                // Server Logs Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "No logs recorded yet or server offline.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        items(logs) { logLine ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = logLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (logLine.contains("ERROR", ignoreCase = true)) ErrorRed else CyberCyan,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
