package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pipeline.NotrackWebViewClient
import com.example.pipeline.dom.*
import com.example.data.local.SettingsManager
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.util.StoryPromptEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    viewModel: MainViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToPreview: (String, String) -> Unit = { _, _ -> },
    hideTopBar: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val settingsState = viewModel?.settingsFlow?.collectAsState()
    val savedSettings = settingsState?.value ?: remember(context) {
        SettingsManager(context).settingsFlow.value
    }
    var selectedLanguage by remember(savedSettings.defaultLanguage) {
        mutableStateOf(if (savedSettings.defaultLanguage.startsWith("Hindi", true)) "Hindi" else "English")
    }
    var selectedCategory by remember(savedSettings.defaultCategory) {
        mutableStateOf(savedSettings.defaultCategory)
    }
    var selectedDuration by remember(savedSettings.defaultDuration) {
        mutableIntStateOf(savedSettings.defaultDuration.coerceIn(15, 300))
    }
    var selectedVoice by remember(savedSettings.ttsVoice, selectedLanguage) {
        val availableVoices = com.example.util.EdgeNeuralTtsService.AVAILABLE_VOICES
            .filter { it.language.startsWith(selectedLanguage, ignoreCase = true) }
        mutableStateOf(
            savedSettings.ttsVoice.takeIf { savedVoice ->
                availableVoices.any { it.id == savedVoice }
            } ?: availableVoices.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedAspect by remember(savedSettings.defaultAspect) {
        mutableStateOf(savedSettings.defaultAspect)
    }
    var showCaptions by remember(savedSettings.showCaptions) {
        mutableStateOf(savedSettings.showCaptions)
    }
    val languageOptions = listOf("Hindi", "English")
    val categoryOptions = remember { com.example.util.UnifiedCategories.ALL.distinct() }
    val durationOptions = remember(selectedDuration) {
        (listOf(30, 60, 90, 120, 180, 240, 300, selectedDuration))
            .filter { it in 15..300 }.distinct().sorted()
    }
    val voiceOptions = remember(selectedLanguage, selectedVoice) {
        com.example.util.EdgeNeuralTtsService.AVAILABLE_VOICES
            .filter { it.language.startsWith(selectedLanguage, ignoreCase = true) }
            .map { it.id }
            .let { voices -> (voices + selectedVoice).distinct() }
    }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = if (savedSettings.defaultLanguage.startsWith("Hindi", true)) {
                        "Namaste! Main Glitch OS AI hoon. Video ideas, scripts aur app ke tools ke baare mein poochhein."
                    } else {
                        "Hi, I’m Glitch OS AI. Ask me about video ideas, scripts, or the tools in this app."
                    },
                    isUser = false
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var notrackClient by remember { mutableStateOf<NotrackWebViewClient?>(null) }
    var showVideoSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            notrackClient = NotrackWebViewClient.create(context)
        } catch (e: Throwable) {
            android.util.Log.e("ChatbotScreen", "Error initializing NotrackWebViewClient: ${e.message}")
        }
    }

    val run10SceneVideoPipeline = { customTopic: String ->
        if (customTopic.isBlank()) {
            messages = messages + ChatMessage(
                text = "Enter a topic before starting the story pipeline.",
                isUser = false
            )
        } else if (!isGenerating) {
            val topicToUse = customTopic.trim()
            val userDisplayMsg = "⚡ Run 10-scene video pipeline: \"$topicToUse\""
            messages = messages + ChatMessage(text = userDisplayMsg, isUser = true)
            inputText = ""
            isGenerating = true

            scope.launch {
                listState.animateScrollToItem(messages.size)
                val statusMsgId = UUID.randomUUID().toString()
                var currentStatus = "Preparing the complete 10-scene video pipeline..."
                messages = messages + ChatMessage(id = statusMsgId, text = currentStatus, isUser = false)

                try {
                    val resultPackage = withContext(Dispatchers.IO) {
                        val orchestrator = DomAutomationOrchestrator(context)
                        orchestrator.runComplete10SceneDomVideoPipeline(
                            topic = topicToUse,
                            options = DomVideoOptions(
                                category = selectedCategory,
                                language = selectedLanguage,
                                durationSeconds = selectedDuration,
                                voiceName = selectedVoice,
                                aspectRatio = selectedAspect,
                                showCaptions = showCaptions
                            ),
                            onProgressUpdate = { percent, status ->
                                scope.launch {
                                    currentStatus = "[$percent%] $status"
                                    messages = messages.map {
                                        if (it.id == statusMsgId) it.copy(text = currentStatus) else it
                                    }
                                }
                            }
                        )
                    }

                    val renderedVideo = resultPackage.finalRenderedVideoFile
                        ?.takeIf { it.exists() && it.length() > 1_000L }
                        ?: throw IllegalStateException("The pipeline returned no playable video file.")
                    val libraryProject = viewModel?.saveGeneratedDomVideo(
                        packageResult = resultPackage,
                        topic = topicToUse,
                        language = selectedLanguage,
                        category = selectedCategory,
                        voiceName = selectedVoice,
                        aspectRatio = selectedAspect,
                        showCaptions = showCaptions
                    ) ?: throw IllegalStateException("Project library is unavailable; the video was not saved.")
                    messages = messages.filter { it.id != statusMsgId } + ChatMessage(
                        text = "Video ready and saved to your library: ${libraryProject.title} (${resultPackage.totalDurationSeconds}s). Opening the final video preview.",
                        isUser = false
                    )

                    val videoUri = Uri.fromFile(renderedVideo).toString()
                    onNavigateToPreview(videoUri, resultPackage.title)

                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.e("ChatbotScreen", "Story pipeline failed for topic: $topicToUse", e)
                    messages = messages.filter { it.id != statusMsgId } + ChatMessage(
                        text = "Story pipeline failed: ${e.localizedMessage ?: "provider or renderer unavailable"}",
                        isUser = false
                    )
                } finally {
                    isGenerating = false
                }
                try {
                    listState.animateScrollToItem(messages.size)
                } catch (_: Exception) {}
            }
        }
    }

    val sendMessage = { textToSend: String ->
        if (textToSend.isNotBlank() && !isGenerating) {
            val isStoryIntent = textToSend.contains("10-scene", ignoreCase = true) ||
                textToSend.contains("10 scene", ignoreCase = true)
            val recentMessages = messages.takeLast(8)

            val userMsg = ChatMessage(text = textToSend, isUser = true)
            messages = messages + userMsg
            inputText = ""
            isGenerating = true

            scope.launch {
                listState.animateScrollToItem(messages.size)
                try {
                    val reply = if (GlitchOsAssistantPrompt.isIdentityQuestion(textToSend)) {
                        GlitchOsAssistantPrompt.identityReply(textToSend)
                    } else {
                        withContext(Dispatchers.IO) {
                            if (isStoryIntent) {
                                val prompt = StoryPromptEngine.build10SceneEmotionalPrompt(textToSend)
                                fetch10SceneStoryJson(prompt, notrackClient)
                            } else {
                                generateAiChatbotResponse(textToSend, notrackClient, recentMessages)
                            }
                        }
                    }
                    messages = messages + ChatMessage(text = reply, isUser = false)
                } catch (exception: kotlinx.coroutines.CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    android.util.Log.e("ChatbotScreen", "Chat response failed.", exception)
                    messages = messages + ChatMessage(
                        text = "I couldn't get a response right now. Please try again.",
                        isUser = false
                    )
                } finally {
                    isGenerating = false
                }
                try {
                    listState.animateScrollToItem(messages.size)
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Glitch OS AI",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Creator assistant",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showVideoSettings = true },
                            modifier = Modifier.testTag("chatbot_video_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Video settings",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            // Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                    }
                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = NeonGreen,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Glitch OS AI is working on your request…",
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Keep the composer focused on the two primary actions: chat and video creation.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message Glitch OS AI", color = TextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chatbot_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { sendMessage(inputText) }
                        ),
                        maxLines = 3
                    )

                    IconButton(
                        onClick = { run10SceneVideoPipeline(inputText) },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("one_click_autonomous_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = "Create a video from this topic",
                            tint = if (inputText.isNotBlank()) CyberCyan else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) NeonGreen else DarkSurface)
                            .testTag("chatbot_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) DarkBackground else TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showVideoSettings) {
        ModalBottomSheet(
            onDismissRequest = { showVideoSettings = false },
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Video settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Used when you create a video from chat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                ChatbotSettingDropdown("Language", selectedLanguage, languageOptions) {
                    selectedLanguage = it
                    selectedVoice = com.example.util.EdgeNeuralTtsService.AVAILABLE_VOICES
                        .firstOrNull { voice ->
                            voice.language.startsWith(it, ignoreCase = true)
                        }?.id.orEmpty()
                }
                ChatbotSettingDropdown("Category", selectedCategory, categoryOptions) {
                    selectedCategory = it
                }
                ChatbotSettingDropdown(
                    "Duration",
                    "${selectedDuration}s",
                    durationOptions.map { "${it}s" }
                ) {
                    selectedDuration = it.removeSuffix("s").toInt()
                }
                ChatbotSettingDropdown("Voice", selectedVoice, voiceOptions) {
                    selectedVoice = it
                }
                ChatbotSettingDropdown("Aspect ratio", selectedAspect, listOf("9:16", "16:9", "1:1")) {
                    selectedAspect = it
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Include captions", color = TextPrimary)
                    Switch(
                        checked = showCaptions,
                        onCheckedChange = { showCaptions = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatbotSettingDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                    Text(
                        selected,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val isJsonStory = !isUser && (message.text.trim().startsWith("{") || message.text.contains("\"scenes\":"))

    val alignment = if (isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isUser) DarkSurfaceElevated else DarkSurface
    val borderColor = when {
        isUser -> CyberCyan.copy(alpha = 0.35f)
        isJsonStory -> NeonPink
        else -> DarkBorder
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bubbleColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = if (isJsonStory) Modifier.fillMaxWidth(0.95f) else Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (isJsonStory) {
                    // Header for JSON story output
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = NeonPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "10-scene story",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonPink
                            )
                        }
                    }
                    ParsedStoryView(jsonText = message.text)
                } else {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 21.sp
                    )
                }

            }
        }
    }
}

@Composable
fun ParsedStoryView(jsonText: String) {
    val storyObj = remember(jsonText) {
        try {
            val trimmed = jsonText.trim()
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                JSONObject(trimmed.substring(start, end + 1))
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    } ?: return

    val title = storyObj.optString("title", "")
    val englishTitle = storyObj.optString("english_title", "")
    val theme = storyObj.optString("theme", "")
    val moral = storyObj.optString("moral", "")
    val totalScenes = storyObj.optInt("total_scenes", 10)
    val durationSec = storyObj.optInt("total_duration_seconds", 150)
    val scenesArray = storyObj.optJSONArray("scenes")

    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Story Title & Theme Header
        if (title.isNotBlank() || englishTitle.isNotBlank()) {
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    if (englishTitle.isNotBlank()) {
                        Text(
                            text = englishTitle,
                            fontSize = 12.sp,
                            color = NeonPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (theme.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Theme: $theme",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyberCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🎬 $totalScenes Scenes",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⏱️ ${durationSec}s Total",
                                fontSize = 10.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Moral Banner
        if (moral.isNotBlank()) {
            Surface(
                color = NeonPink.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "❤️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "EMOTIONAL CORE & MORAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonPink,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = moral,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Toggle Scene Cards Breakdown
        if (scenesArray != null && scenesArray.length() > 0) {
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide 10 Scenes Visual Breakdown ▲" else "View All 10 Scenes Visual Breakdown (${scenesArray.length()} Scenes) ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    for (i in 0 until scenesArray.length()) {
                        val scene = scenesArray.optJSONObject(i) ?: continue
                        val sceneNum = scene.optInt("scene_number", i + 1)
                        val sceneTitle = scene.optString("scene_title", "Scene $sceneNum")
                        val hindiNarration = scene.optString("narration_hindi", "")
                        val engNarration = scene.optString("narration_english", "")
                        val visualDesc = scene.optString("visual_description", "")
                        val camera = scene.optString("camera_motion", "")
                        val weather = scene.optString("weather_atmosphere", "")
                        val sfx = scene.optString("audio_sfx", "")
                        val duration = scene.optInt("duration_seconds", 15)

                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = NeonPink,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "SCENE $sceneNum",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sceneTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = "${duration}s",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }

                                if (hindiNarration.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = hindiNarration,
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 17.sp
                                    )
                                }

                                if (engNarration.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = engNarration,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }

                                if (visualDesc.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🎨 Visual Prompt: $visualDesc",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (camera.isNotBlank()) {
                                        StoryBadgeChip(text = "🎥 $camera", color = CyberCyan)
                                    }
                                    if (weather.isNotBlank() && weather != "NONE") {
                                        StoryBadgeChip(text = "🌧️ $weather", color = Color(0xFF38BDF8))
                                    }
                                    if (sfx.isNotBlank()) {
                                        StoryBadgeChip(text = "🔊 $sfx", color = Color(0xFFA855F7))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryBadgeChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

private suspend fun generateAiChatbotResponse(
    query: String,
    client: NotrackWebViewClient?,
    recentMessages: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    val assistantPrompt = GlitchOsAssistantPrompt.buildPrompt(query, recentMessages)

    // 1. Try Live In-App Notrack Scraper Engine (WebView Client)
    if (client != null) {
        try {
            val liveScraped = client.askNotrackForText(assistantPrompt, timeout = 40_000)
            if (liveScraped.isNotBlank() && !liveScraped.startsWith("<!DOCTYPE", ignoreCase = true)) {
                return@withContext liveScraped.trim()
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatbotScreen", "Direct Notrack WebView client exception: ${e.message}")
        }
    }

    // 2. Try Node.js Scraper Server Loopback (Port 3000)
    val candidateUrls = listOf("http://10.0.2.2:3000/chat", "http://127.0.0.1:3000/chat", "http://localhost:3000/chat")
    val jsonPayload = JSONObject().apply {
        put("prompt", assistantPrompt)
    }.toString()

    for (endpoint in candidateUrls) {
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 3000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                connection.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }

                if (connection.responseCode in 200..299) {
                    val respText = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (respText.isNotBlank()) {
                        val jsonObj = JSONObject(respText)
                        val script = jsonObj.optString("script", "")
                        val title = jsonObj.optString("title", "")
                        val description = jsonObj.optString("description", "")
                        val response = jsonObj.optString("response", "")

                        val textBuilder = StringBuilder()
                        if (title.isNotBlank()) {
                            textBuilder.append("🎬 ").append(title).append("\n\n")
                        }
                        if (script.isNotBlank()) {
                            textBuilder.append(script).append("\n\n")
                        } else if (description.isNotBlank()) {
                            textBuilder.append(description).append("\n\n")
                        } else if (response.isNotBlank()) {
                            textBuilder.append(response).append("\n\n")
                        }

                        val finalOutput = textBuilder.toString().trim()
                        if (finalOutput.isNotBlank()) {
                            return@withContext finalOutput
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatbotScreen", "Endpoint $endpoint failed: ${e.message}")
        }
    }

    return@withContext "AI provider unavailable. Check the configured provider connection and try again."
}

private suspend fun fetch10SceneStoryJson(
    prompt: String,
    client: NotrackWebViewClient?
): String = withContext(Dispatchers.IO) {
    // 1. Try Live in-app NotrackWebViewClient
    if (client != null) {
        try {
            val jsonRaw = client.askNotrackForJsonObject(prompt, timeout = 55_000)
            val trimmed = jsonRaw.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val parsed = JSONObject(trimmed)
                if (parsed.has("scenes") || parsed.has("title")) {
                    return@withContext parsed.toString(2)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatbotScreen", "askNotrackForJsonObject failed: ${e.message}")
        }
    }

    // 2. Try Node.js Scraper Server Loopback (Port 3000)
    val candidateUrls = listOf("http://10.0.2.2:3000/chat", "http://127.0.0.1:3000/chat", "http://localhost:3000/chat")
    val jsonPayload = JSONObject().apply {
        put("prompt", prompt)
    }.toString()

    for (endpoint in candidateUrls) {
        try {
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 3000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                connection.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }

                if (connection.responseCode in 200..299) {
                    val respText = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (respText.isNotBlank()) {
                        val jsonObj = JSONObject(respText)
                        val rawObj = jsonObj.optJSONObject("raw")
                        if (rawObj != null && (rawObj.has("scenes") || rawObj.has("title"))) {
                            return@withContext rawObj.toString(2)
                        }
                        val respStr = jsonObj.optString("response", "")
                        if (respStr.startsWith("{") && respStr.endsWith("}")) {
                            return@withContext JSONObject(respStr).toString(2)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatbotScreen", "Endpoint $endpoint failed: ${e.message}")
        }
    }

    return@withContext "Story provider unavailable. Check the configured provider connection and try again."
}
