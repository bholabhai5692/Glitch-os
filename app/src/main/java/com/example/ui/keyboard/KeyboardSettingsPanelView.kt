package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyboardSettingsPanelView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = theme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = theme.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Keyboard Super-Preferences",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.keyContent
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            contentDescription = "Close Settings",
                            tint = theme.keySubContent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Number Row & Arrow Row Toggles
                item {
                    SettingsCard(theme = theme) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingToggleRow(
                                title = "Top Number Row (1-0)",
                                subtitle = "Show numbers row above QWERTY keyboard",
                                isChecked = stateHolder.showNumberRow,
                                onCheckedChange = { stateHolder.toggleNumberRow() },
                                theme = theme
                            )
                            HorizontalDivider(color = theme.keySubContent.copy(alpha = 0.2f))
                            SettingToggleRow(
                                title = "Dedicated Arrow Keys (← ↑ ↓ →)",
                                subtitle = "Navigation bar below spacebar",
                                isChecked = stateHolder.showArrowRow,
                                onCheckedChange = { stateHolder.toggleArrowRow() },
                                theme = theme
                            )
                        }
                    }
                }

                // Theme Selection
                item {
                    SettingsCard(theme = theme) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🎨 Keyboard Theme",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryAccent
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThemeChip("Cyberpunk", stateHolder.themeType == KeyboardThemeType.CYBERPUNK_NEON, { stateHolder.setTheme(KeyboardThemeType.CYBERPUNK_NEON) }, theme)
                                ThemeChip("Dark M3", stateHolder.themeType == KeyboardThemeType.DARK_MATERIAL, { stateHolder.setTheme(KeyboardThemeType.DARK_MATERIAL) }, theme)
                                ThemeChip("OLED Black", stateHolder.themeType == KeyboardThemeType.OLED_PITCH_BLACK, { stateHolder.setTheme(KeyboardThemeType.OLED_PITCH_BLACK) }, theme)
                                ThemeChip("Warm Pastel", stateHolder.themeType == KeyboardThemeType.WARM_PASTEL, { stateHolder.setTheme(KeyboardThemeType.WARM_PASTEL) }, theme)
                            }
                        }
                    }
                }

                // Audio Key Click Feedback
                item {
                    SettingsCard(theme = theme) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🔊 Key Press Audio Sound",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryAccent
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SoundChip("Click", stateHolder.soundType == SoundType.SYSTEM_CLICK, { stateHolder.setSoundSettings(SoundType.SYSTEM_CLICK, stateHolder.soundVolume) }, theme)
                                SoundChip("Mech", stateHolder.soundType == SoundType.MECHANICAL_CLICK, { stateHolder.setSoundSettings(SoundType.MECHANICAL_CLICK, stateHolder.soundVolume) }, theme)
                                SoundChip("Bubble", stateHolder.soundType == SoundType.POP_BUBBLE, { stateHolder.setSoundSettings(SoundType.POP_BUBBLE, stateHolder.soundVolume) }, theme)
                                SoundChip("Typewriter", stateHolder.soundType == SoundType.TYPEWRITER, { stateHolder.setSoundSettings(SoundType.TYPEWRITER, stateHolder.soundVolume) }, theme)
                                SoundChip("Mute", stateHolder.soundType == SoundType.OFF, { stateHolder.setSoundSettings(SoundType.OFF, stateHolder.soundVolume) }, theme)
                            }
                        }
                    }
                }

                item {
                    SettingsCard(theme = theme) {
                        SettingToggleRow(
                            title = "Online Suggestions",
                            subtitle = "Send typed word prefixes to Google for extra suggestions",
                            isChecked = stateHolder.onlineSuggestionsEnabled,
                            onCheckedChange = stateHolder::updateOnlineSuggestionsEnabled,
                            theme = theme
                        )
                    }
                }

                // Haptic Feedback
                item {
                    SettingsCard(theme = theme) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📳 Haptic Vibration Feedback",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryAccent
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HapticChip("Off", stateHolder.hapticStrength == HapticStrength.OFF, { stateHolder.setHapticStrengthSetting(HapticStrength.OFF) }, theme)
                                HapticChip("Light", stateHolder.hapticStrength == HapticStrength.LIGHT, { stateHolder.setHapticStrengthSetting(HapticStrength.LIGHT) }, theme)
                                HapticChip("Medium", stateHolder.hapticStrength == HapticStrength.MEDIUM, { stateHolder.setHapticStrengthSetting(HapticStrength.MEDIUM) }, theme)
                                HapticChip("Strong", stateHolder.hapticStrength == HapticStrength.STRONG, { stateHolder.setHapticStrengthSetting(HapticStrength.STRONG) }, theme)
                            }
                        }
                    }
                }

                // Height Scale
                item {
                    SettingsCard(theme = theme) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📏 Keyboard Height",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryAccent
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HeightChip("Compact", stateHolder.keyboardHeightScale < 0.95f, { stateHolder.setHeightScale(0.88f) }, theme)
                                HeightChip("Standard", stateHolder.keyboardHeightScale in 0.95f..1.05f, { stateHolder.setHeightScale(1.0f) }, theme)
                                HeightChip("Tall / Spacious", stateHolder.keyboardHeightScale > 1.05f, { stateHolder.setHeightScale(1.18f) }, theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    theme: KeyboardThemeColors,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = theme.keyBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(10.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: KeyboardThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = theme.keyContent)
            Text(text = subtitle, fontSize = 10.sp, color = theme.keySubContent)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.surface,
                checkedTrackColor = theme.primaryAccent,
                uncheckedThumbColor = theme.keySubContent,
                uncheckedTrackColor = theme.actionKeyBackground
            )
        )
    }
}

@Composable
private fun ThemeChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SoundChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HapticChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HeightChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
