package com.example.ui.keyboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun KeyView(
    keyModel: KeyModel,
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val theme = stateHolder.currentTheme
    val coroutineScope = rememberCoroutineScope()

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 60),
        label = "KeyScale"
    )

    val isAutoTypeTriggerKey = keyModel.code == "f" ||
            keyModel.label.equals("f", ignoreCase = true)

    var holdProgress by remember { mutableStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var autoTypeTriggered by remember { mutableStateOf(false) }

    val keyBackground = when (keyModel.type) {
        KeyType.LETTER -> theme.keyBackground
        KeyType.SPACE -> theme.keyBackground
        KeyType.ENTER -> theme.primaryAccent
        KeyType.SHIFT -> when (stateHolder.shiftState) {
            ShiftState.OFF -> theme.actionKeyBackground
            ShiftState.ONCE -> theme.primaryAccent
            ShiftState.CAPS_LOCK -> theme.primaryAccent
        }
        KeyType.BACKSPACE -> theme.actionKeyBackground
        else -> theme.actionKeyBackground
    }

    val keyContentColor = when (keyModel.type) {
        KeyType.ENTER -> theme.surface
        KeyType.SHIFT -> when (stateHolder.shiftState) {
            ShiftState.OFF -> theme.actionKeyContent
            ShiftState.ONCE -> theme.surface
            ShiftState.CAPS_LOCK -> theme.surface
        }
        else -> theme.keyContent
    }

    val displayLabel = when (keyModel.type) {
        KeyType.LETTER -> if (stateHolder.shiftState != ShiftState.OFF) keyModel.shiftedLabel else keyModel.label
        KeyType.ENTER -> stateHolder.enterKeyLabel
        else -> keyModel.label
    }

    var dragDistance by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 3.dp)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(keyBackground)
            .pointerInput(keyModel) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val downChange = downEvent.changes.firstOrNull { it.pressed } ?: continue

                        isPressed = true
                        dragDistance = 0f
                        autoTypeTriggered = false
                        holdProgress = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        stateHolder.setPopupKey(keyModel)
                        stateHolder.addGlidePoint(downChange.position)

                        var backspaceJob: Job? = null
                        if (keyModel.type == KeyType.BACKSPACE) {
                            stateHolder.onKeyPressed(keyModel)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            backspaceJob = coroutineScope.launch {
                                delay(400L) // Wait before starting auto-repeat
                                while (isActive) {
                                    stateHolder.onKeyPressed(keyModel)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    delay(80L) // Repeat deletion every 80ms
                                }
                            }
                        }

                        var spaceJob: Job? = null
                        if (keyModel.type == KeyType.SPACE) {
                            stateHolder.onKeyPressed(keyModel)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            spaceJob = coroutineScope.launch {
                                delay(400L) // Wait before starting auto-repeat
                                while (isActive) {
                                    stateHolder.onKeyPressed(keyModel)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    delay(80L) // Repeat space entry every 80ms
                                }
                            }
                        }

                        var symbolTriggered = false
                        var symbolHoldJob: Job? = null
                        if (keyModel.type == KeyType.LETTER && keyModel.subLabel != null) {
                            symbolHoldJob = coroutineScope.launch {
                                delay(2000L) // Wait for exactly 2 seconds
                                if (kotlin.math.abs(dragDistance) < 20f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    stateHolder.insertText(keyModel.subLabel)
                                    symbolTriggered = true
                                }
                            }
                        }

                        if (isAutoTypeTriggerKey) {
                            holdJob?.cancel()
                            holdJob = coroutineScope.launch {
                                val startTime = System.currentTimeMillis()
                                while (isActive) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val progress = (elapsed / 5000f).coerceIn(0f, 1f)
                                    holdProgress = progress
                                    stateHolder.updateSpaceFHoldProgress(progress)

                                    if (elapsed >= 5000L) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        stateHolder.switchPanel(ActivePanel.AUTO_TYPE)
                                        autoTypeTriggered = true
                                        holdProgress = 0f
                                        stateHolder.updateSpaceFHoldProgress(0f)
                                        break
                                    }
                                    delay(50L)
                                }
                            }
                        }

                        var anyPressed = true
                        while (anyPressed) {
                            val moveEvent = awaitPointerEvent()
                            val changes = moveEvent.changes
                            val activeChange = changes.firstOrNull()
                            if (activeChange != null) {
                                if (activeChange.pressed) {
                                    val position = activeChange.position
                                    val previousPosition = activeChange.previousPosition
                                    val diffX = position.x - previousPosition.x
                                    dragDistance += diffX
                                    stateHolder.addGlidePoint(position)

                                    if (kotlin.math.abs(dragDistance) > 30f) {
                                        holdJob?.cancel()
                                        holdJob = null
                                        holdProgress = 0f
                                        stateHolder.updateSpaceFHoldProgress(0f)
                                        backspaceJob?.cancel()
                                        backspaceJob = null
                                        spaceJob?.cancel()
                                        spaceJob = null
                                        symbolHoldJob?.cancel()
                                        symbolHoldJob = null
                                    }

                                    if (keyModel.type == KeyType.SPACE) {
                                        if (dragDistance > 40f) {
                                            stateHolder.moveCursor(1)
                                            dragDistance = 0f
                                        } else if (dragDistance < -40f) {
                                            stateHolder.moveCursor(-1)
                                            dragDistance = 0f
                                        }
                                    } else if (keyModel.type == KeyType.BACKSPACE) {
                                        if (dragDistance < -60f) {
                                            stateHolder.deleteWordBackward()
                                            dragDistance = 0f
                                        }
                                    }
                                    activeChange.consume()
                                } else {
                                    anyPressed = false
                                }
                            } else {
                                anyPressed = false
                            }
                        }

                        // Release / Up
                        isPressed = false
                        holdJob?.cancel()
                        holdJob = null
                        backspaceJob?.cancel()
                        backspaceJob = null
                        spaceJob?.cancel()
                        spaceJob = null
                        symbolHoldJob?.cancel()
                        symbolHoldJob = null
                        val wasTriggered = autoTypeTriggered || symbolTriggered
                        autoTypeTriggered = false
                        holdProgress = 0f
                        stateHolder.updateSpaceFHoldProgress(0f)
                        stateHolder.setPopupKey(null)
                        stateHolder.clearGlideTrail()

                        if (!wasTriggered && kotlin.math.abs(dragDistance) < 20f) {
                            if (keyModel.type != KeyType.BACKSPACE && keyModel.type != KeyType.SPACE) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                stateHolder.onKeyPressed(keyModel)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val isDuplicateNumberHint = stateHolder.keyboardMode == KeyboardMode.QWERTY &&
            stateHolder.showNumberRow && keyModel.subLabel?.singleOrNull()?.isDigit() == true
        if (keyModel.subLabel != null && keyModel.type == KeyType.LETTER && !isDuplicateNumberHint) {
            Text(
                text = keyModel.subLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = theme.keySubContent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }

        // Main Key Content Label / Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (keyModel.type) {
                KeyType.SHIFT -> {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Shift",
                        tint = keyContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                KeyType.BACKSPACE -> {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = keyContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> {
                    Text(
                        text = displayLabel,
                        fontSize = if (keyModel.type == KeyType.LETTER) 19.sp else 14.sp,
                        fontWeight = if (keyModel.type == KeyType.LETTER) FontWeight.Normal else FontWeight.Medium,
                        color = keyContentColor
                    )
                }
            }

            if (isAutoTypeTriggerKey && holdProgress > 0f) {
                val remainingSec = (5 - (holdProgress * 5).toInt()).coerceAtLeast(1)
                Text(
                    text = "Hold 5s (${remainingSec}s)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryAccent
                )
            }
        }

        // 5-second Hold Progress Fill Bar at bottom of key
        if (isAutoTypeTriggerKey && holdProgress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(holdProgress)
                    .height(3.dp)
                    .background(theme.primaryAccent)
            )
        }
    }
}
