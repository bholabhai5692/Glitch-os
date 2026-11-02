package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class AutoToolTab {
    PIN_SEQUENCE,
    TURBO_ARCHITECTURE,
    TEXT_REPEATER,
    DATA_GENERATOR,
    SMART_MACROS,
    CUSTOM_COUNTER,
    TEXT_TRANSFORMER
}

@Composable
fun AutoTypePanelView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme
    var currentTab by remember { mutableStateOf(AutoToolTab.PIN_SEQUENCE) }
    val isAutoTypeLocked = stateHolder.autoTypeState == AutoTypeState.RUNNING ||
        stateHolder.autoTypeState == AutoTypeState.STOPPING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tab Header & Return to Keyboard Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AutoTabChip("🔢 PIN Sequence", currentTab == AutoToolTab.PIN_SEQUENCE, { currentTab = AutoToolTab.PIN_SEQUENCE }, theme, !isAutoTypeLocked)
                AutoTabChip("🤖 Smart Macros", currentTab == AutoToolTab.SMART_MACROS, { currentTab = AutoToolTab.SMART_MACROS }, theme, !isAutoTypeLocked)
                AutoTabChip("📈 Counter Engine", currentTab == AutoToolTab.CUSTOM_COUNTER, { currentTab = AutoToolTab.CUSTOM_COUNTER }, theme, !isAutoTypeLocked)
                AutoTabChip("✨ Text Stylizer", currentTab == AutoToolTab.TEXT_TRANSFORMER, { currentTab = AutoToolTab.TEXT_TRANSFORMER }, theme, !isAutoTypeLocked)
                AutoTabChip("⚡ Turbo Engine", currentTab == AutoToolTab.TURBO_ARCHITECTURE, { currentTab = AutoToolTab.TURBO_ARCHITECTURE }, theme, !isAutoTypeLocked)
                AutoTabChip("🔁 Repeater", currentTab == AutoToolTab.TEXT_REPEATER, { currentTab = AutoToolTab.TEXT_REPEATER }, theme, !isAutoTypeLocked)
                AutoTabChip("🎲 Auth & Data", currentTab == AutoToolTab.DATA_GENERATOR, { currentTab = AutoToolTab.DATA_GENERATOR }, theme, !isAutoTypeLocked)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = theme.primaryAccent.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { stateHolder.switchPanel(ActivePanel.KEYBOARD) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Main Keyboard",
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "QWERTY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryAccent
                        )
                    }
                }
                IconButton(
                    onClick = { stateHolder.switchPanel(ActivePanel.KEYBOARD) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.keySubContent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        when (currentTab) {
            AutoToolTab.PIN_SEQUENCE -> PinSequenceSection(stateHolder = stateHolder, theme = theme)
            AutoToolTab.SMART_MACROS -> SmartMacrosSection(stateHolder = stateHolder, theme = theme)
            AutoToolTab.CUSTOM_COUNTER -> CustomCounterSection(stateHolder = stateHolder, theme = theme)
            AutoToolTab.TEXT_TRANSFORMER -> TextTransformerSection(stateHolder = stateHolder, theme = theme)
            AutoToolTab.TURBO_ARCHITECTURE -> TurboArchitectureSection(stateHolder = stateHolder, theme = theme)
            AutoToolTab.TEXT_REPEATER -> TextRepeaterSection(stateHolder = stateHolder, theme = theme)
            AutoToolTab.DATA_GENERATOR -> DataGeneratorSection(stateHolder = stateHolder, theme = theme)
        }
    }
}

@Composable
private fun AutoTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors,
    enabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PinSequenceSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    val isRunning = stateHolder.autoTypeState == AutoTypeState.RUNNING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status & Live Number Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (stateHolder.autoTypeCurrentFormatted.isEmpty()) "—" else stateHolder.autoTypeCurrentFormatted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = theme.primaryAccent
                )

                if (stateHolder.useSmartStringDiffing) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = theme.primaryAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "DIFF ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryAccent,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (stateHolder.autoTypeState) {
                    AutoTypeState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                    AutoTypeState.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                    AutoTypeState.STOPPING -> MaterialTheme.colorScheme.errorContainer
                    AutoTypeState.IDLE -> theme.actionKeyBackground
                }
            ) {
                Text(
                    text = stateHolder.autoTypeStatusMessage,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (stateHolder.autoTypeState) {
                        AutoTypeState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                        AutoTypeState.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> theme.keyContent
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Live Telemetry Bar
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = theme.actionKeyBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diff Chars Saved: ${stateHolder.autoTypeDiffCharsSaved}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryAccent
                )
                Text(
                    text = "Tx: ${stateHolder.autoTypeTotalTransactions}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.keyContent
                )
                Text(
                    text = "Batch: ${stateHolder.autoTypeBatchSize}x",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.keyContent
                )
            }
        }

        // Speed & Preset Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed: ${stateHolder.autoTypeSpeed} ops/sec", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(10, 50, 100, 250).forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (stateHolder.autoTypeSpeed == preset) theme.primaryAccent else theme.actionKeyBackground,
                        modifier = Modifier.clickable { stateHolder.updateAutoTypeSpeed(preset) }
                    ) {
                        Text(
                            text = "$preset/s",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stateHolder.autoTypeSpeed == preset) theme.surface else theme.keyContent,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = stateHolder.autoTypeSpeed.toFloat(),
            onValueChange = { stateHolder.updateAutoTypeSpeed(it.toInt()) },
            valueRange = 1f..500f,
            steps = 499,
            modifier = Modifier.height(14.dp),
            colors = SliderDefaults.colors(thumbColor = theme.primaryAccent, activeTrackColor = theme.primaryAccent)
        )

        // Digit Count Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Digits Length: ${stateHolder.autoTypeDigitCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
            val digits = stateHolder.autoTypeDigitCount
            val zeroStr = stateHolder.formatNumber(0L, digits).ifEmpty { "0000" }
            val maxStr = stateHolder.formatNumber(stateHolder.getMaxForDigitCount(digits), digits).ifEmpty { "9999" }
            Text(if (stateHolder.autoTypeStartFromZero) "$zeroStr ➔ $maxStr" else "$maxStr ➔ $zeroStr", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.primaryAccent)
        }

        Slider(
            value = stateHolder.autoTypeDigitCount.toFloat(),
            onValueChange = { stateHolder.updateAutoTypeDigitCount(it.toInt()) },
            valueRange = 0f..8f,
            steps = 7,
            enabled = !isRunning,
            modifier = Modifier.height(14.dp),
            colors = SliderDefaults.colors(thumbColor = theme.primaryAccent, activeTrackColor = theme.primaryAccent)
        )

        // Start Mode Option Switcher (0000 Start vs 9999 Start Switch)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.actionKeyBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val digits = stateHolder.autoTypeDigitCount
                val zeroStr = stateHolder.formatNumber(0L, digits).ifEmpty { "0000" }
                val maxVal = stateHolder.getMaxForDigitCount(digits)
                val maxStr = stateHolder.formatNumber(maxVal, digits).ifEmpty { "9999" }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Start Sequence Mode",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.keyContent
                        )
                    }

                    // Direct Switch Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (stateHolder.autoTypeStartFromZero) "$zeroStr Start" else "$maxStr Start",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = stateHolder.autoTypeStartFromZero,
                            onCheckedChange = { stateHolder.updateAutoTypeStartFromZero(it) },
                            enabled = !isRunning,
                            modifier = Modifier.scale(0.6f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = theme.primaryAccent,
                                checkedTrackColor = theme.primaryAccent.copy(alpha = 0.5f),
                                uncheckedThumbColor = theme.keyContent,
                                uncheckedTrackColor = theme.surface
                            )
                        )
                    }
                }

                // Two Interactive Segmented Option Cards: [0000 ➔ 9999] and [9999 ➔ 0000]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Option 1: Start from 0000 (Ascending)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (stateHolder.autoTypeStartFromZero) theme.primaryAccent else theme.surface,
                        border = if (stateHolder.autoTypeStartFromZero) null else androidx.compose.foundation.BorderStroke(1.dp, theme.keySubContent.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !isRunning) { stateHolder.updateAutoTypeStartFromZero(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (stateHolder.autoTypeStartFromZero) theme.surface else theme.keyContent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Start $zeroStr (➔ $maxStr)",
                                fontSize = 9.sp,
                                fontWeight = if (stateHolder.autoTypeStartFromZero) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (stateHolder.autoTypeStartFromZero) theme.surface else theme.keyContent
                            )
                        }
                    }

                    // Option 2: Start from 9999 (Descending)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (!stateHolder.autoTypeStartFromZero) theme.primaryAccent else theme.surface,
                        border = if (!stateHolder.autoTypeStartFromZero) null else androidx.compose.foundation.BorderStroke(1.dp, theme.keySubContent.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !isRunning) { stateHolder.updateAutoTypeStartFromZero(false) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (!stateHolder.autoTypeStartFromZero) theme.surface else theme.keyContent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Start $maxStr (➔ $zeroStr)",
                                fontSize = 9.sp,
                                fontWeight = if (!stateHolder.autoTypeStartFromZero) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (!stateHolder.autoTypeStartFromZero) theme.surface else theme.keyContent
                            )
                        }
                    }
                }
            }
        }

        // Optimization Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smart Diffing Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Smart Diff", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
                Switch(
                    checked = stateHolder.useSmartStringDiffing,
                    onCheckedChange = { stateHolder.toggleSmartStringDiffing() },
                    enabled = !isRunning,
                    modifier = Modifier.scale(0.6f),
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryAccent, checkedTrackColor = theme.primaryAccent.copy(alpha = 0.5f))
                )
            }

            // Batch Edit Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BatchEdit", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
                Switch(
                    checked = stateHolder.useBatchEditOptimization,
                    onCheckedChange = { stateHolder.toggleBatchEditOptimization() },
                    enabled = !isRunning,
                    modifier = Modifier.scale(0.6f),
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryAccent, checkedTrackColor = theme.primaryAccent.copy(alpha = 0.5f))
                )
            }

            // Auto Submit Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AutoSend", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
                Switch(
                    checked = stateHolder.autoTypeAutoSubmit,
                    onCheckedChange = { stateHolder.updateAutoTypeAutoSubmit(it) },
                    enabled = !isRunning,
                    modifier = Modifier.scale(0.6f),
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryAccent, checkedTrackColor = theme.primaryAccent.copy(alpha = 0.5f))
                )
            }
        }

        // Action Buttons (Start / Stop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { stateHolder.startAutoType() },
                enabled = !isRunning,
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("START TURBO SEQUENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.surface)
            }

            Button(
                onClick = { stateHolder.stopAutoType() },
                enabled = isRunning,
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("STOP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TurboArchitectureSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Architecture Header Card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.primaryAccent.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "⚡ High-Speed Direct Buffer Injection Architecture",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryAccent
                )
                Text(
                    text = "Normal apps rely on touch events and animations. This engine uses Direct Buffer Injection, Batching, and String Diffing to bypass UI rendering lag and IPC overhead.",
                    fontSize = 9.sp,
                    color = theme.keyContent
                )
            }
        }

        // Optimization 1: beginBatchEdit & endBatchEdit
        OptimizationCard(
            number = "1",
            title = "beginBatchEdit() & endBatchEdit() (IPC Reduction)",
            desc = "Pauses target app UI re-renders and combines multiple keystrokes into a single IPC message instead of firing an IPC call per character.",
            codeSnippet = "ic.beginBatchEdit()\n// multiple updates executed in memory\nic.endBatchEdit()",
            theme = theme
        )

        // Optimization 2: Smart String Diffing
        OptimizationCard(
            number = "2",
            title = "Smart String Diffing (getCommonLength)",
            desc = "Instead of deleting 4 digits and typing 4 new digits, it finds common prefix (e.g., '1234' -> '1235' shares '123') and replaces only the last suffix character.",
            codeSnippet = "val commonLen = getCommonLength(lastStr, nextStr)\nval delCount = lastStr.length - commonLen\nif (delCount > 0) ic.deleteSurroundingText(delCount, 0)\nic.commitText(nextStr.substring(commonLen), 1)",
            theme = theme
        )

        // Optimization 3: Low-Delay Handler & Batching
        OptimizationCard(
            number = "3",
            title = "Low-Delay Event Loop & Batch Multiplier",
            desc = "Executes batch updates (2x to 10x per frame) with 1ms-2ms minimal delays, directly injecting characters into the software buffer.",
            codeSnippet = "for (i in 0 until batchSize) {\n    applyDiffBatch(ic, currentVal)\n}\npostDelayed(this, 1L /* or yield */)",
            theme = theme
        )

        // Interactive Batch Multiplier Configuration
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.actionKeyBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Batch Size Per Event Iteration: ${stateHolder.autoTypeBatchSize}x",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.keyContent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1, 2, 5, 10).forEach { size ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (stateHolder.autoTypeBatchSize == size) theme.primaryAccent else theme.keyBackground,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { stateHolder.updateAutoTypeBatchSize(size) }
                        ) {
                            Text(
                                text = "${size}x Batch",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stateHolder.autoTypeBatchSize == size) theme.surface else theme.keyContent,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptimizationCard(
    number: String,
    title: String,
    desc: String,
    codeSnippet: String,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = theme.keyBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = theme.primaryAccent
                ) {
                    Text(
                        text = "#$number",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.surface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.keyContent
                )
            }
            Text(
                text = desc,
                fontSize = 8.5.sp,
                color = theme.keySubContent,
                lineHeight = 11.sp
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = codeSnippet,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64FFDA),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun TextRepeaterSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    var repeatText by remember { mutableStateOf("🔥 Power Glitch Keyboard!") }
    var repeatCount by remember { mutableStateOf(5) }
    var withIndex by remember { mutableStateOf(false) }
    var addNewline by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = repeatText,
            onValueChange = { repeatText = it },
            label = { Text("Text to repeat") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Repeat Count: $repeatCount times", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(3, 5, 10, 20, 50).forEach { cnt ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (repeatCount == cnt) theme.primaryAccent else theme.actionKeyBackground,
                        modifier = Modifier.clickable { repeatCount = cnt }
                    ) {
                        Text(
                            text = "${cnt}x",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (repeatCount == cnt) theme.surface else theme.keyContent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Numbered (1., 2.)", fontSize = 10.sp, color = theme.keyContent)
                Switch(
                    checked = withIndex,
                    onCheckedChange = { withIndex = it },
                    modifier = Modifier.scale(0.7f),
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryAccent, checkedTrackColor = theme.primaryAccent.copy(alpha = 0.5f))
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New Line", fontSize = 10.sp, color = theme.keyContent)
                Switch(
                    checked = addNewline,
                    onCheckedChange = { addNewline = it },
                    modifier = Modifier.scale(0.7f),
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.primaryAccent, checkedTrackColor = theme.primaryAccent.copy(alpha = 0.5f))
                )
            }
        }

        Button(
            onClick = {
                stateHolder.repeatAndInsertText(repeatText, repeatCount, withIndex, addNewline)
                stateHolder.switchPanel(ActivePanel.KEYBOARD)
            },
            modifier = Modifier.fillMaxWidth().height(34.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent)
        ) {
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("GENERATE & INSERT REPEATED TEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.surface)
        }
    }
}

@Composable
private fun DataGeneratorSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    val secureRandom = remember { SecureRandom() }
    val items = listOf(
        Pair("🔐 Strong 16-Char Password") {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
            (1..16).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
        },
        Pair("🆔 UUID v4") {
            UUID.randomUUID().toString()
        },
        Pair("📜 Lorem Ipsum Paragraph") {
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
        },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items.size) { index ->
            val (title, generator) = items[index]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = theme.keyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val value = generator()
                        stateHolder.insertText(value)
                        stateHolder.switchPanel(ActivePanel.KEYBOARD)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = theme.keyContent)
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Insert",
                        tint = theme.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartMacrosSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    var selectedCategory by remember { mutableStateOf("TIMESTAMPS") }
    val categories = listOf("TIMESTAMPS", "CODE", "MARKDOWN", "FORMS")

    val timestampItems = remember {
        listOf(
            "ISO 8601 Timestamp" to { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()) },
            "Clean Date & Time" to { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()) },
            "Unix Epoch Millis" to { System.currentTimeMillis().toString() },
            "Date (YYYY-MM-DD)" to { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
        )
    }

    val codeSnippets = remember {
        listOf(
            "JSON API Payload" to { "{\n  \"status\": \"SUCCESS\",\n  \"code\": 200,\n  \"timestamp\": ${System.currentTimeMillis()}\n}" },
            "cURL Request" to { "curl -X POST \"https://api.example.com/v1/data\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{\"query\": \"sample\"}'" },
            "HTML5 Boilerplate" to { "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <title>App</title>\n</head>\n<body>\n  <h1>Hello World</h1>\n</body>\n</html>" },
            "SQL Select Query" to { "SELECT id, name, created_at FROM records WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 50;" },
            "Python Requests" to { "import requests\nres = requests.get('https://api.github.com')\nprint(res.json())" }
        )
    }

    val markdownItems = remember {
        listOf(
            "Markdown Table 3x3" to { "| Feature | Status | Priority |\n| --- | --- | --- |\n| Pipeline | Ready | High |\n| Engine | Active | Critical |" },
            "Kotlin Code Block" to { "```kotlin\nfun main() {\n    println(\"Hello Glitch!\")\n}\n```" },
            "Task Checklist" to { "- [ ] Task 1: Initialize project\n- [ ] Task 2: Build video pipeline\n- [x] Task 3: Deploy release" },
            "Blockquote Alert" to { "> **NOTE**: This process is fully automated by the agent engine." }
        )
    }

    val formItems = remember {
        listOf(
            "Contact Details Template" to { "Name: [your name]\nEmail: [your email]\nPhone: [your phone]\nRole: [your role]" },
            "Shipping Address Template" to { "Street: [street address]\nCity: [city]\nState/Region: [state or region]\nPostal Code: [postal code]\nCountry: [country]" },
            "Bug Report Template" to { "**Bug Description**:\n**Steps to Reproduce**:\n1. Open App\n2. Tap action\n**Expected**:\n**Actual**:" }
        )
    }

    val currentItems = when (selectedCategory) {
        "TIMESTAMPS" -> timestampItems
        "CODE" -> codeSnippets
        "MARKDOWN" -> markdownItems
        else -> formItems
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Category Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedCategory == cat) theme.primaryAccent else theme.actionKeyBackground,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCategory == cat) theme.surface else theme.keyContent,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Snippet List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(currentItems.size) { idx ->
                val (title, generator) = currentItems[idx]
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.keyBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val content = generator()
                            stateHolder.insertText(content)
                            stateHolder.switchPanel(ActivePanel.KEYBOARD)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.keyContent)
                            Text(
                                text = generator().take(40).replace("\n", " ") + "...",
                                fontSize = 9.sp,
                                color = theme.keySubContent,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Insert",
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCounterSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    var prefix by remember { mutableStateOf("ITEM_") }
    var suffix by remember { mutableStateOf("") }
    var currentNumber by remember { mutableStateOf(1) }
    var stepSize by remember { mutableStateOf(1) }
    var paddingDigits by remember { mutableStateOf(3) }

    val formattedNumber = remember(currentNumber, prefix, suffix, paddingDigits) {
        val padded = currentNumber.toString().padStart(paddingDigits, '0')
        "$prefix$padded$suffix"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Preview Header
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = theme.keyBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NEXT OUTPUT PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = theme.keySubContent)
                    Text(
                        text = formattedNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = theme.primaryAccent
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            stateHolder.insertText(formattedNumber)
                            currentNumber += stepSize
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Insert (+${stepSize})", fontSize = 11.sp, color = theme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("IMG_", "DOC_", "USER#", "ORDER_").forEach { p ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (prefix == p) theme.primaryAccent.copy(alpha = 0.25f) else theme.actionKeyBackground,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { prefix = p }
                ) {
                    Text(
                        text = p,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (prefix == p) theme.primaryAccent else theme.keyContent,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }
        }

        // Step Size & Padding Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step Increment:", fontSize = 10.sp, color = theme.keyContent, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 2, 5, 10).forEach { s ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (stepSize == s) theme.primaryAccent else theme.actionKeyBackground,
                        modifier = Modifier.clickable { stepSize = s }
                    ) {
                        Text(
                            text = "+$s",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stepSize == s) theme.surface else theme.keyContent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Zero Padding:", fontSize = 10.sp, color = theme.keyContent, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1 to "None", 2 to "01", 3 to "001", 4 to "0001").forEach { (digits, label) ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (paddingDigits == digits) theme.primaryAccent else theme.actionKeyBackground,
                        modifier = Modifier.clickable { paddingDigits = digits }
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (paddingDigits == digits) theme.surface else theme.keyContent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons: Burst & Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val batch = (0 until 10).map { i ->
                        val num = currentNumber + (i * stepSize)
                        val padded = num.toString().padStart(paddingDigits, '0')
                        "$prefix$padded$suffix"
                    }.joinToString("\n")
                    stateHolder.insertText(batch)
                    currentNumber += 10 * stepSize
                    stateHolder.switchPanel(ActivePanel.KEYBOARD)
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                Text("Burst 10 Items", fontSize = 11.sp, color = theme.surface, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { currentNumber = 1 },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset = 1", fontSize = 11.sp, color = theme.primaryAccent)
            }
        }
    }
}

@Composable
private fun TextTransformerSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    var sourceText by remember { mutableStateOf("Glitch Modern Keyboard") }

    val transforms = remember(sourceText) {
        listOf(
            "UPPERCASE" to sourceText.uppercase(),
            "lowercase" to sourceText.lowercase(),
            "Title Case" to sourceText.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            "camelCase" to sourceText.split(" ").mapIndexed { i, s -> if (i == 0) s.lowercase() else s.replaceFirstChar { c -> c.uppercase() } }.joinToString(""),
            "snake_case" to sourceText.lowercase().replace(" ", "_"),
            "kebab-case" to sourceText.lowercase().replace(" ", "-"),
            "1337 Speak" to sourceText.uppercase().replace("A", "4").replace("E", "3").replace("I", "1").replace("O", "0").replace("T", "7"),
            "Vaporwave" to sourceText.map { c -> if (c in '!'..'~') (c.code + 0xFEE0).toChar() else c }.joinToString(""),
            "Reverse" to sourceText.reversed(),
            "Encased" to "⟦ $sourceText ⟧"
        )
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Input text field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = sourceText,
                onValueChange = { sourceText = it },
                singleLine = true,
                placeholder = { Text("Type or paste text...", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.keyContent,
                    unfocusedTextColor = theme.keyContent,
                    focusedBorderColor = theme.primaryAccent,
                    unfocusedBorderColor = theme.actionKeyBackground
                )
            )
            IconButton(
                onClick = {
                    val clip = clipboardManager.getText()?.text
                    if (!clip.isNullOrBlank()) {
                        sourceText = clip
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = theme.primaryAccent)
            }
        }

        // List of transformed variants
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(transforms.size) { idx ->
                val (label, result) = transforms[idx]
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = theme.keyBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            stateHolder.insertText(result)
                            stateHolder.switchPanel(ActivePanel.KEYBOARD)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = theme.primaryAccent)
                            Text(result, fontSize = 11.sp, color = theme.keyContent, maxLines = 1)
                        }
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Insert",
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
