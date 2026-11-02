package com.example.ui.keyboard

import android.content.Context
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.*
import java.util.UUID

class KeyboardStateHolder {

    var activeInputConnection by mutableStateOf<InputConnection?>(null)
    var inputConnectionProvider: (() -> InputConnection?)? = null
    private var currentImeAction by mutableIntStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)
    private var isSensitiveInput = false
    private var disallowPersonalizedLearning = false

    val resolvedInputConnection: InputConnection?
        get() = activeInputConnection ?: inputConnectionProvider?.invoke()

    var textFieldValue by mutableStateOf(TextFieldValue(""))
        private set

    var shiftState by mutableStateOf(ShiftState.OFF)
        private set

    var keyboardMode by mutableStateOf(KeyboardMode.QWERTY)
        private set

    var activePanel by mutableStateOf(ActivePanel.KEYBOARD)
        private set

    var themeType by mutableStateOf(KeyboardThemeType.DARK_MATERIAL)
        private set

    var oneHandedMode by mutableStateOf(OneHandedMode.OFF)
        private set

    var isHinglishModeEnabled by mutableStateOf(false)
        private set

    var onlineSuggestionsEnabled by mutableStateOf(false)
        private set

    // Sound and Haptics
    var soundManager: KeyboardSoundManager? = null
    var soundType by mutableStateOf(SoundType.OFF)
        private set
    var soundVolume by mutableStateOf(1.0f)
        private set
    var hapticStrength by mutableStateOf(HapticStrength.MEDIUM)
        private set

    // Layout Customization
    var showNumberRow by mutableStateOf(true)
        private set
    var showArrowRow by mutableStateOf(false)
        private set
    var keyboardHeightScale by mutableStateOf(1.0f) // 0.88f (Compact), 1.0f (Normal), 1.18f (Tall)
        private set

    var glideTrailPoints = mutableStateListOf<Offset>()
        private set

    var isVoiceTypingActive by mutableStateOf(false)
        private set

    var voiceStatusText by mutableStateOf("")
        private set

    // Auto Type System State & High-Speed Optimizations
    var autoTypeState by mutableStateOf(AutoTypeState.IDLE)
        private set

    var autoTypeSpeed by mutableStateOf(50) // 1 to 500 ops/sec
        private set

    var autoTypeDigitCount by mutableStateOf(4) // 0 to 9 digits
        private set

    var autoTypeStartFromZero by mutableStateOf(true) // true = 0->9, false = 9->0
        private set

    var autoTypeAutoSubmit by mutableStateOf(false) // true = press send after each, false = inline replacement
        private set

    // Advanced High-Speed Architecture Flags
    var useBatchEditOptimization by mutableStateOf(false)
        private set

    var useSmartStringDiffing by mutableStateOf(true)
        private set

    var autoTypeBatchSize by mutableStateOf(1) // 1, 2, 5, 10 updates per cycle
        private set

    var autoTypeLoopDelayMs by mutableStateOf(2L) // 1ms, 2ms, 5ms
        private set

    var autoTypeCurrentValue by mutableStateOf(0L)
        private set

    var autoTypeCurrentFormatted by mutableStateOf("0000")
        private set

    var autoTypePreviousFormatted by mutableStateOf("")
        private set

    var autoTypeStatusMessage by mutableStateOf("READY")
        private set

    var autoTypeDiffCharsSaved by mutableStateOf(0L)
        private set

    var autoTypeTotalTransactions by mutableStateOf(0L)
        private set

    var spaceFHoldProgress by mutableStateOf(0f)
        private set

    // Calculator State
    var calculatorExpression by mutableStateOf("")
        private set
    var calculatorResult by mutableStateOf("")
        private set
    var calculatorHistory = mutableStateListOf<CalculatorHistoryItem>()
        private set

    // Snippets State
    var snippetsList = mutableStateListOf(
        SnippetItem("1", "Email Greeting", "Email", "Hi {CLIPBOARD},\n\nI hope you're having a productive day!\n\nBest,\n", isFavorite = true),
        SnippetItem("2", "Meeting Confirmation", "Work", "Confirmed for our meeting on {DATE} at {TIME}. Looking forward to connecting!", isFavorite = true),
        SnippetItem("3", "Follow Up Note", "Work", "Just following up on our previous discussion. Please let me know when you have a moment to review."),
        SnippetItem("4", "UPI Payment ID", "Crypto/UPI", "user@okhdfcbank", isFavorite = true),
        SnippetItem("5", "Crypto Wallet (ETH/EVM)", "Crypto/UPI", "0x71C...492b", isFavorite = false),
        SnippetItem("6", "Social Bio", "Social", "🚀 Tech Explorer & Creator | 📍 Global | Building the future ✨"),
        SnippetItem("7", "Quick Hashtags", "Social", "#Technology #Innovation #Coding #GlitchOS #Future #AndroidDev", isFavorite = true),
        SnippetItem("8", "OTP / Verification Code", "Auth", "Your 6-digit verification code is: {RANDOM_OTP}. Valid for 10 minutes."),
        SnippetItem("9", "Unique Transaction ID", "Dev", "TXN-{YEAR}-{UUID}")
    )

    private val autoTypeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var autoTypeJob: Job? = null
    private var suggestionJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    val currentTheme: KeyboardThemeColors
        get() = when (themeType) {
            KeyboardThemeType.DYNAMIC_SYSTEM -> KeyboardThemes.DarkMaterial
            KeyboardThemeType.DARK_MATERIAL -> KeyboardThemes.DarkMaterial
            KeyboardThemeType.OLED_PITCH_BLACK -> KeyboardThemes.OledBlack
            KeyboardThemeType.CYBERPUNK_NEON -> KeyboardThemes.Cyberpunk
            KeyboardThemeType.WARM_PASTEL -> KeyboardThemes.WarmPastel
        }

    fun initializeAudio(context: Context) {
        if (soundManager == null) {
            soundManager = KeyboardSoundManager(context.applicationContext)
        }
    }

    fun playFeedback() {
        soundManager?.playSound(soundType, soundVolume)
        soundManager?.performHaptic(hapticStrength)
    }

    fun setTheme(theme: KeyboardThemeType) {
        themeType = theme
    }

    fun setOneHanded(mode: OneHandedMode) {
        oneHandedMode = mode
    }

    fun setSoundSettings(type: SoundType, volume: Float) {
        soundType = type
        soundVolume = volume.coerceIn(0f, 1f)
    }

    fun setHapticStrengthSetting(strength: HapticStrength) {
        hapticStrength = strength
    }

    fun toggleNumberRow() {
        showNumberRow = !showNumberRow
    }

    fun toggleArrowRow() {
        showArrowRow = !showArrowRow
    }

    fun setHeightScale(scale: Float) {
        keyboardHeightScale = scale.coerceIn(0.85f, 1.25f)
    }

    fun toggleHinglishMode() {
        isHinglishModeEnabled = !isHinglishModeEnabled
        updateSuggestions()
    }

    fun addGlidePoint(offset: Offset) {
        glideTrailPoints.add(offset)
    }

    fun clearGlideTrail() {
        glideTrailPoints.clear()
    }

    fun processGlideTrail(keyPositions: Map<String, Offset>) {
        if (glideTrailPoints.size > 5) {
            val decoded = GlideTypingDecoder.decodeTrail(glideTrailPoints, keyPositions)
            if (decoded.isNotEmpty()) {
                applySuggestion(decoded.first())
            }
        }
        clearGlideTrail()
    }

    var voiceInputHandler: VoiceInputHandler? = null

    fun toggleVoiceTyping(context: Context) {
        if (voiceInputHandler == null) {
            voiceInputHandler = VoiceInputHandler(
                context = context,
                onSpeechResult = { result ->
                    val textToCommit = if (result.endsWith(" ")) result else "$result "
                    insertText(textToCommit)
                    updateVoiceStatus(false, "")
                },
                onPartialSpeech = { partialText ->
                    updateVoiceStatus(true, partialText)
                },
                onError = { errorMsg ->
                    updateVoiceStatus(false, errorMsg)
                }
            )
        }

        val handler = voiceInputHandler ?: return
        if (isVoiceTypingActive) {
            handler.stopListening()
            updateVoiceStatus(false, "")
        } else {
            updateVoiceStatus(true, "🎙️ Listening...")
            handler.startListening(if (isHinglishModeEnabled) "hi-IN" else "en-US")
        }
    }

    fun updateVoiceStatus(active: Boolean, statusMsg: String) {
        isVoiceTypingActive = active
        voiceStatusText = statusMsg
    }

    var activePopupKey by mutableStateOf<KeyModel?>(null)
        private set

    var activeLongPressKey by mutableStateOf<KeyModel?>(null)
        private set

    var suggestions by mutableStateOf<List<WordSuggestion>>(emptyList())
        private set

    var clipboardHistory = mutableStateListOf(
        ClipboardItem("1", "Glitch Keyboard UI"),
        ClipboardItem("2", "Jetpack Compose Custom IME"),
        ClipboardItem("3", "Auto-Type OTP Generator Engine", isPinned = true)
    )

    var recentEmojis = mutableStateListOf("😊", "🔥", "👍", "❤️", "🚀", "✨", "🎉", "💯", "🤖", "⚡", "💡", "😂")

    val currentLayout: KeyboardLayout
        get() = when (keyboardMode) {
            KeyboardMode.QWERTY -> KeyboardLayouts.QWERTY
            KeyboardMode.NUMERIC -> KeyboardLayouts.NUMERIC
            KeyboardMode.SYMBOLS -> KeyboardLayouts.SYMBOLS
        }

    val enterKeyLabel: String
        get() = when (currentImeAction) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_PREVIOUS -> "Previous"
            else -> "↵"
        }

    fun updateEditorInfo(info: EditorInfo?) {
        currentImeAction = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED
        disallowPersonalizedLearning = info?.let {
            it.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        } ?: false

        val inputType = info?.inputType ?: InputType.TYPE_NULL
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        isSensitiveInput = inputClass != InputType.TYPE_CLASS_TEXT ||
            (inputClass == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (inputClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        updateSuggestions()
    }

    fun updateOnlineSuggestionsEnabled(enabled: Boolean) {
        onlineSuggestionsEnabled = enabled
        updateSuggestions()
    }

    fun updateTextFieldValue(newValue: TextFieldValue) {
        textFieldValue = newValue
        updateSuggestions()
    }

    fun onKeyPressed(key: KeyModel) {
        playFeedback()
        when (key.type) {
            KeyType.LETTER -> {
                val char = if (shiftState != ShiftState.OFF) key.shiftedLabel else key.label
                insertText(char)
                if (shiftState == ShiftState.ONCE) {
                    shiftState = ShiftState.OFF
                }
            }
            KeyType.SPACE -> {
                insertText(" ")
            }
            KeyType.BACKSPACE -> {
                handleBackspace()
            }
            KeyType.ENTER -> {
                resolvedInputConnection?.let(::performEnterAction)
                suggestions = emptyList()
                suggestionJob?.cancel()
            }
            KeyType.SHIFT -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ONCE
                    ShiftState.ONCE -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
            }
            KeyType.MODE_SWITCH -> {
                keyboardMode = when (key.label) {
                    "?123", "123" -> KeyboardMode.NUMERIC
                    "=\\<", "SYM" -> KeyboardMode.SYMBOLS
                    else -> KeyboardMode.QWERTY
                }
            }
            KeyType.EMOJI_SWITCH -> {
                activePanel = if (activePanel == ActivePanel.EMOJI) ActivePanel.KEYBOARD else ActivePanel.EMOJI
            }
            KeyType.CLIPBOARD_SWITCH -> {
                activePanel = if (activePanel == ActivePanel.CLIPBOARD) ActivePanel.KEYBOARD else ActivePanel.CLIPBOARD
            }
            KeyType.CURSOR_PAD_SWITCH -> {
                activePanel = if (activePanel == ActivePanel.CURSOR_PAD) ActivePanel.KEYBOARD else ActivePanel.CURSOR_PAD
            }
            KeyType.ACTION -> {}
        }
    }

    fun onLongPressKey(key: KeyModel) {
        playFeedback()
        if (key.longPressOptions.isNotEmpty()) {
            activeLongPressKey = key
        } else if (key.subLabel != null) {
            insertText(key.subLabel)
        }
    }

    fun selectLongPressOption(option: String) {
        playFeedback()
        insertText(option)
        activeLongPressKey = null
    }

    fun dismissLongPress() {
        activeLongPressKey = null
    }

    fun setPopupKey(key: KeyModel?) {
        activePopupKey = key
    }

    fun insertText(text: String) {
        resolvedInputConnection?.commitText(text, 1)

        val currentText = textFieldValue.text
        val selection = textFieldValue.selection

        val newText = StringBuilder(currentText)
            .replace(selection.min, selection.max, text)
            .toString()

        val newCursor = selection.min + text.length
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        updateSuggestions()
    }

    fun handleBackspace() {
        resolvedInputConnection?.let { conn ->
            val selectedText = conn.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                conn.commitText("", 1)
            } else {
                conn.deleteSurroundingText(1, 0)
            }
        }

        val currentText = textFieldValue.text
        val selection = textFieldValue.selection

        if (selection.min != selection.max) {
            val newText = StringBuilder(currentText).delete(selection.min, selection.max).toString()
            textFieldValue = TextFieldValue(newText, TextRange(selection.min))
        } else if (selection.min > 0) {
            val newText = StringBuilder(currentText).deleteAt(selection.min - 1).toString()
            val newCursor = selection.min - 1
            textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        }
        updateSuggestions()
    }

    fun deleteWordBackward() {
        val currentText = textFieldValue.text
        val cursor = textFieldValue.selection.min
        val boundary = KeyboardAlgorithms.findPreviousWordBoundary(currentText, cursor)
        if (cursor > boundary) {
            val deleteLen = cursor - boundary
            resolvedInputConnection?.deleteSurroundingText(deleteLen, 0)

            val newText = StringBuilder(currentText).delete(boundary, cursor).toString()
            textFieldValue = TextFieldValue(newText, TextRange(boundary))
            updateSuggestions()
        }
    }

    fun moveCursor(delta: Int) {
        val currentText = textFieldValue.text
        val newCursor = (textFieldValue.selection.min + delta).coerceIn(0, currentText.length)
        resolvedInputConnection?.setSelection(newCursor, newCursor)
        textFieldValue = textFieldValue.copy(selection = TextRange(newCursor))
    }

    fun selectAllText() {
        val currentText = textFieldValue.text
        resolvedInputConnection?.setSelection(0, currentText.length)
        textFieldValue = textFieldValue.copy(selection = TextRange(0, currentText.length))
    }

    fun copySelectedText() {
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection
        val selected = if (sel.min != sel.max) {
            currentText.substring(sel.min, sel.max)
        } else {
            resolvedInputConnection?.getSelectedText(0)?.toString() ?: currentText
        }
        if (selected.isNotBlank()) {
            addClipboardItem(selected)
        }
    }

    fun cutSelectedText() {
        copySelectedText()
        handleBackspace()
    }

    fun pasteFromClipboard() {
        val first = clipboardHistory.firstOrNull()?.text
        if (!first.isNullOrBlank()) {
            insertText(first)
        }
    }

    fun clearAllText() {
        resolvedInputConnection?.let { conn ->
            conn.beginBatchEdit()
            conn.deleteSurroundingText(10000, 10000)
            conn.commitText("", 1)
            conn.endBatchEdit()
        }
        textFieldValue = TextFieldValue("", TextRange(0))
        updateSuggestions()
    }

    fun applySuggestion(suggestion: WordSuggestion) {
        val cleanWord = suggestion.word.trim().split(Regex("\\s+")).firstOrNull() ?: suggestion.word.trim()
        val wordToCommit = "$cleanWord "
        val conn = resolvedInputConnection

        if (conn != null) {
            conn.beginBatchEdit()
            val textBefore = conn.getTextBeforeCursor(60, 0)?.toString() ?: ""
            val wordStartInConn = getCurrentWordStart(textBefore, textBefore.length)
            val charsToDelete = textBefore.length - wordStartInConn
            if (charsToDelete > 0) {
                conn.deleteSurroundingText(charsToDelete, 0)
            }
            conn.commitText(wordToCommit, 1)
            conn.endBatchEdit()
        }

        val currentText = textFieldValue.text
        val cursor = textFieldValue.selection.min
        val wordStart = getCurrentWordStart(currentText, cursor)

        val newText = if (currentText.isNotEmpty() && wordStart <= cursor && cursor <= currentText.length) {
            StringBuilder(currentText).replace(wordStart, cursor, wordToCommit).toString()
        } else {
            wordToCommit
        }

        val newCursor = wordStart + wordToCommit.length
        textFieldValue = TextFieldValue(newText, TextRange(newCursor))
        updateSuggestions()
    }

    private fun getCurrentWordStart(text: String, cursor: Int): Int {
        if (cursor <= 0) return 0
        var i = cursor - 1
        while (i >= 0 && !text[i].isWhitespace()) {
            i--
        }
        return i + 1
    }

    private fun updateSuggestions() {
        suggestionJob?.cancel()
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.min.coerceIn(0, text.length)
        val wordStart = getCurrentWordStart(text, cursor)
        val currentWord = if (wordStart <= cursor && cursor <= text.length) {
            text.substring(wordStart, cursor).trim()
        } else ""

        if (isSensitiveInput || currentWord.isBlank() || currentWord.contains(" ")) {
            suggestions = emptyList()
            return
        }

        val hinglish = if (isHinglishModeEnabled) {
            HinglishTransliteration.getHinglishSuggestions(currentWord)
        } else emptyList()
        val local = KeyboardAlgorithms.getSuggestions(currentWord, maxResults = 3)
        val baseCombined = normalizeSuggestions(hinglish + local, 3)
        suggestions = baseCombined

        if (!onlineSuggestionsEnabled || disallowPersonalizedLearning || currentWord.length < 2) return

        suggestionJob = autoTypeScope.launch {
            delay(180)
            val language = if (isHinglishModeEnabled) "hi" else "en"
            val online = GoogleSuggestService.fetchGoogleSuggestions(currentWord, lang = language, maxResults = 3)
            val latestWord = withContext(Dispatchers.Main.immediate) {
                val latestText = textFieldValue.text
                val latestCursor = textFieldValue.selection.min.coerceIn(0, latestText.length)
                val latestStart = getCurrentWordStart(latestText, latestCursor)
                latestText.substring(latestStart, latestCursor).trim()
            }
            if (latestWord.equals(currentWord, ignoreCase = true) && !isSensitiveInput && online.isNotEmpty()) {
                val merged = normalizeSuggestions(baseCombined + online, 4)
                withContext(Dispatchers.Main.immediate) {
                    suggestions = merged
                }
            }
        }
    }

    private fun normalizeSuggestions(items: List<WordSuggestion>, limit: Int): List<WordSuggestion> =
        items
            .map { it.copy(word = it.word.trim().split(Regex("\\s+")).firstOrNull() ?: it.word.trim()) }
            .filter { it.word.isNotBlank() && !it.word.contains(" ") }
            .distinctBy { it.word.lowercase() }
            .take(limit)

    fun dismissSuggestions() {
        suggestions = emptyList()
        suggestionJob?.cancel()
    }

    fun addClipboardItem(text: String) {
        if (text.isNotBlank()) {
            val existing = clipboardHistory.find { it.text == text }
            if (existing != null) {
                clipboardHistory.remove(existing)
            }
            clipboardHistory.add(0, ClipboardItem(id = UUID.randomUUID().toString(), text = text))
        }
    }

    fun deleteClipboardItem(item: ClipboardItem) {
        clipboardHistory.remove(item)
    }

    fun clearClipboardHistory() {
        val pinned = clipboardHistory.filter { it.isPinned }
        clipboardHistory.clear()
        clipboardHistory.addAll(pinned)
    }

    fun togglePinClipboardItem(item: ClipboardItem) {
        val index = clipboardHistory.indexOf(item)
        if (index != -1) {
            clipboardHistory[index] = item.copy(isPinned = !item.isPinned)
        }
    }

    fun switchPanel(panel: ActivePanel) {
        if (autoTypeState == AutoTypeState.RUNNING || autoTypeState == AutoTypeState.STOPPING) {
            activePanel = ActivePanel.AUTO_TYPE
            return
        }
        playFeedback()
        activePanel = if (activePanel == panel) ActivePanel.KEYBOARD else panel
    }

    // AI Writing & Transformations
    fun transformCurrentText(tone: TextTone) {
        playFeedback()
        val text = getTargetTextForTransformation()
        if (text.isNotBlank()) {
            val transformed = KeyboardTextTransformers.transformTone(text, tone)
            replaceTargetText(transformed)
        }
    }

    fun convertCurrentCase(caseType: CaseType) {
        playFeedback()
        val text = getTargetTextForTransformation()
        if (text.isNotBlank()) {
            val converted = KeyboardTextTransformers.convertCase(text, caseType)
            replaceTargetText(converted)
        }
    }

    fun applyFancyFontToCurrent(style: FancyFontStyle) {
        playFeedback()
        val text = getTargetTextForTransformation()
        if (text.isNotBlank()) {
            val styled = KeyboardTextTransformers.applyFancyFont(text, style)
            replaceTargetText(styled)
        }
    }

    private fun getTargetTextForTransformation(): String {
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection
        if (sel.min != sel.max && currentText.isNotEmpty()) {
            return currentText.substring(sel.min, sel.max)
        }
        val connSelected = resolvedInputConnection?.getSelectedText(0)?.toString()
        if (!connSelected.isNullOrBlank()) {
            return connSelected
        }
        return currentText
    }

    private fun replaceTargetText(newText: String) {
        val currentText = textFieldValue.text
        val sel = textFieldValue.selection

        if (sel.min != sel.max) {
            resolvedInputConnection?.commitText(newText, 1)
            val updated = StringBuilder(currentText).replace(sel.min, sel.max, newText).toString()
            textFieldValue = TextFieldValue(updated, TextRange(sel.min + newText.length))
        } else {
            clearAllText()
            insertText(newText)
        }
    }

    // Interactive Calculator Operations
    fun onCalculatorInput(token: String) {
        playFeedback()
        when (token) {
            "C" -> {
                calculatorExpression = ""
                calculatorResult = ""
            }
            "⌫" -> {
                if (calculatorExpression.isNotEmpty()) {
                    calculatorExpression = calculatorExpression.dropLast(1)
                    val (num, str) = KeyboardTextTransformers.evaluateMathExpression(calculatorExpression)
                    calculatorResult = if (num != null) "= $str" else ""
                }
            }
            "=" -> {
                val (num, str) = KeyboardTextTransformers.evaluateMathExpression(calculatorExpression)
                if (num != null) {
                    calculatorHistory.add(0, CalculatorHistoryItem(calculatorExpression, str))
                    calculatorResult = str
                }
            }
            else -> {
                calculatorExpression += token
                val (num, str) = KeyboardTextTransformers.evaluateMathExpression(calculatorExpression)
                calculatorResult = if (num != null) "= $str" else ""
            }
        }
    }

    fun insertCalculatorResult(insertEquation: Boolean) {
        playFeedback()
        val (num, str) = KeyboardTextTransformers.evaluateMathExpression(calculatorExpression)
        val textToInsert = if (num != null) {
            if (insertEquation) "$calculatorExpression = $str" else str
        } else calculatorExpression

        if (textToInsert.isNotBlank()) {
            insertText(textToInsert)
            calculatorExpression = ""
            calculatorResult = ""
            switchPanel(ActivePanel.KEYBOARD)
        }
    }

    // Snippets Operations
    fun insertSnippet(snippet: SnippetItem) {
        playFeedback()
        val clipboardText = clipboardHistory.firstOrNull()?.text
        val resolved = KeyboardTextTransformers.resolveSnippetPlaceholders(snippet.content, clipboardText)
        insertText(resolved)
        switchPanel(ActivePanel.KEYBOARD)
    }

    fun addCustomSnippet(title: String, category: String, content: String) {
        if (title.isNotBlank() && content.isNotBlank()) {
            snippetsList.add(0, SnippetItem(
                id = UUID.randomUUID().toString(),
                title = title,
                category = category.ifBlank { "Custom" },
                content = content,
                isFavorite = false
            ))
        }
    }

    fun removeSnippet(snippet: SnippetItem) {
        snippetsList.remove(snippet)
    }

    // Auto Type System Logic
    fun getMaxForDigitCount(count: Int): Long {
        if (count <= 0) return 0L
        var max = 1L
        repeat(count) { max *= 10L }
        return max - 1L
    }

    fun formatNumber(value: Long, count: Int): String {
        if (count <= 0) return ""
        return String.format(java.util.Locale.US, "%0${count}d", value)
    }

    fun updateAutoTypeSpeed(speed: Int) {
        autoTypeSpeed = speed.coerceIn(1, 500)
    }

    fun updateAutoTypeDigitCount(count: Int) {
        if (autoTypeState == AutoTypeState.RUNNING) return
        val clamped = count.coerceIn(0, 9)
        autoTypeDigitCount = clamped
        resetAutoTypeSequence()
    }

    fun updateAutoTypeStartFromZero(fromZero: Boolean) {
        if (autoTypeState == AutoTypeState.RUNNING) return
        autoTypeStartFromZero = fromZero
        resetAutoTypeSequence()
    }

    fun updateAutoTypeAutoSubmit(enabled: Boolean) {
        if (autoTypeState == AutoTypeState.RUNNING) return
        autoTypeAutoSubmit = enabled
    }

    fun updateAutoTypeBatchSize(size: Int) {
        autoTypeBatchSize = size.coerceIn(1, 10)
    }

    fun updateAutoTypeLoopDelayMs(delayMs: Long) {
        autoTypeLoopDelayMs = delayMs.coerceIn(0L, 50L)
    }

    fun toggleBatchEditOptimization() {
        useBatchEditOptimization = !useBatchEditOptimization
    }

    private fun performEnterAction(connection: InputConnection) {
        val action = currentImeAction
        val actionHandled = action != EditorInfo.IME_ACTION_UNSPECIFIED &&
            action != EditorInfo.IME_ACTION_NONE &&
            connection.performEditorAction(action)
        if (!actionHandled) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    fun toggleSmartStringDiffing() {
        useSmartStringDiffing = !useSmartStringDiffing
    }

    fun resetAutoTypeSequence() {
        val maxVal = getMaxForDigitCount(autoTypeDigitCount)
        autoTypeCurrentValue = if (autoTypeStartFromZero) 0L else maxVal
        autoTypeCurrentFormatted = formatNumber(autoTypeCurrentValue, autoTypeDigitCount)
        autoTypePreviousFormatted = ""
        autoTypeStatusMessage = "READY"
        autoTypeDiffCharsSaved = 0L
        autoTypeTotalTransactions = 0L
    }

    fun updateSpaceFHoldProgress(progress: Float) {
        spaceFHoldProgress = progress.coerceIn(0f, 1f)
    }

    fun getCommonLength(s1: String, s2: String): Int {
        val minLen = minOf(s1.length, s2.length)
        var i = 0
        while (i < minLen && s1[i] == s2[i]) {
            i++
        }
        return i
    }

    fun startAutoType() {
        if (autoTypeState == AutoTypeState.RUNNING) return

        val maxVal = getMaxForDigitCount(autoTypeDigitCount)

        if (autoTypeState == AutoTypeState.COMPLETED) {
            resetAutoTypeSequence()
        }

        autoTypeState = AutoTypeState.RUNNING
        autoTypeStatusMessage = "TURBO RUNNING (Batch: ${autoTypeBatchSize}x)..."

        autoTypeJob?.cancel()
        autoTypeJob = autoTypeScope.launch {
            try {
                var currentVal = autoTypeCurrentValue
                var lastTypedString = autoTypePreviousFormatted
                var lastUiUpdateMs = 0L
                var charsSavedCounter = autoTypeDiffCharsSaved
                var transactionsCounter = autoTypeTotalTransactions

                while (isActive && autoTypeState == AutoTypeState.RUNNING) {
                    val conn = resolvedInputConnection ?: inputConnectionProvider?.invoke()
                    val batchSize = if (autoTypeAutoSubmit) 1 else autoTypeBatchSize.coerceAtLeast(1)

                    if (conn == null && textFieldValue.text.isEmpty() && !autoTypeAutoSubmit) {
                        delay(50L)
                        continue
                    }

                    // Optimization 1: beginBatchEdit() - IPC Overhead & Redraw Reduction
                    if (useBatchEditOptimization) {
                        conn?.beginBatchEdit()
                    }

                    try {
                        for (batchIndex in 0 until batchSize) {
                            val formattedValue = formatNumber(currentVal, autoTypeDigitCount)

                            if (autoTypeAutoSubmit) {
                                performAutoSubmitType(formattedValue)
                                transactionsCounter++
                            } else {
                                if (useSmartStringDiffing && lastTypedString.isNotEmpty()) {
                                    // Optimization 2: Smart String Diffing (getCommonLength)
                                    val commonLen = getCommonLength(lastTypedString, formattedValue)
                                    val delCount = lastTypedString.length - commonLen
                                    
                                    charsSavedCounter += commonLen.toLong()

                                    if (conn != null) {
                                        if (delCount > 0) {
                                            conn.deleteSurroundingText(delCount, 0)
                                        }
                                        val appendText = formattedValue.substring(commonLen)
                                        if (appendText.isNotEmpty()) {
                                            conn.commitText(appendText, 1)
                                        }
                                    } else {
                                        // Playground fallback direct buffer injection
                                        val currentText = textFieldValue.text
                                        val base = if (currentText.length >= delCount) {
                                            currentText.dropLast(delCount)
                                        } else ""
                                        val appendText = formattedValue.substring(commonLen)
                                        val newText = base + appendText
                                        withContext(Dispatchers.Main.immediate) {
                                            textFieldValue = TextFieldValue(newText, TextRange(newText.length))
                                        }
                                    }
                                } else {
                                    // Full replacement mode (Composing / Whole text)
                                    if (conn != null) {
                                        conn.setComposingText(formattedValue, 1)
                                    } else {
                                        withContext(Dispatchers.Main.immediate) {
                                            textFieldValue = TextFieldValue(formattedValue, TextRange(formattedValue.length))
                                        }
                                    }
                                }
                                transactionsCounter++
                            }

                            lastTypedString = formattedValue

                            // Increment or Decrement sequence
                            if (autoTypeStartFromZero) {
                                if (currentVal >= maxVal) {
                                    withContext(Dispatchers.Main.immediate) {
                                        autoTypeState = AutoTypeState.COMPLETED
                                        autoTypeStatusMessage = "COMPLETED"
                                        autoTypeDiffCharsSaved = charsSavedCounter
                                        autoTypeTotalTransactions = transactionsCounter
                                        (resolvedInputConnection ?: inputConnectionProvider?.invoke())?.finishComposingText()
                                    }
                                    return@launch
                                }
                                currentVal++
                            } else {
                                if (currentVal <= 0L) {
                                    withContext(Dispatchers.Main.immediate) {
                                        autoTypeState = AutoTypeState.COMPLETED
                                        autoTypeStatusMessage = "COMPLETED"
                                        autoTypeDiffCharsSaved = charsSavedCounter
                                        autoTypeTotalTransactions = transactionsCounter
                                        (resolvedInputConnection ?: inputConnectionProvider?.invoke())?.finishComposingText()
                                    }
                                    return@launch
                                }
                                currentVal--
                            }
                        }
                    } finally {
                        // End of Batch Edit execution
                        if (useBatchEditOptimization) {
                            conn?.endBatchEdit()
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdateMs >= 40L) {
                        val tempFormatted = lastTypedString
                        val tempVal = currentVal
                        val savedNow = charsSavedCounter
                        val txNow = transactionsCounter
                        withContext(Dispatchers.Main.immediate) {
                            autoTypePreviousFormatted = tempFormatted
                            autoTypeCurrentFormatted = tempFormatted
                            autoTypeCurrentValue = tempVal
                            autoTypeDiffCharsSaved = savedNow
                            autoTypeTotalTransactions = txNow
                        }
                        lastUiUpdateMs = now
                    }

                    // Optimization 3: Low-Delay Handler/Coroutine Loop
                    val speed = autoTypeSpeed.coerceAtLeast(1)
                    val targetIntervalMs = (1000.0 / speed).toLong()
                    if (targetIntervalMs > 15L) {
                        delay(targetIntervalMs)
                    } else if (autoTypeLoopDelayMs > 0L) {
                        delay(autoTypeLoopDelayMs)
                    } else {
                        yield()
                    }
                }
            } catch (e: CancellationException) {
                // Stopped by user
            } catch (e: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    autoTypeStatusMessage = "ERROR: ${e.message}"
                    autoTypeState = AutoTypeState.IDLE
                }
            }
        }
    }

    fun stopAutoType() {
        autoTypeState = AutoTypeState.STOPPING
        autoTypeJob?.cancel()
        autoTypeJob = null
        resolvedInputConnection?.finishComposingText()
        autoTypeState = AutoTypeState.IDLE
        autoTypeStatusMessage = "STOPPED at $autoTypeCurrentFormatted"
    }

    fun performComposingType(nextStr: String) {
        resolvedInputConnection?.let { conn ->
            conn.beginBatchEdit()
            conn.setComposingText(nextStr, 1)
            conn.endBatchEdit()
        }
    }

    fun performAutoSubmitType(nextStr: String) {
        val conn = resolvedInputConnection ?: return
        conn.beginBatchEdit()
        conn.commitText(nextStr, 1)
        performEnterAction(conn)
        conn.endBatchEdit()
    }

    // Repeat Text Spammer
    fun repeatAndInsertText(textToRepeat: String, count: Int, withIndex: Boolean, addNewline: Boolean) {
        playFeedback()
        if (textToRepeat.isBlank() || count <= 0) return
        val sb = StringBuilder()
        for (i in 1..count) {
            if (withIndex) sb.append("$i. ")
            sb.append(textToRepeat)
            if (addNewline && i < count) sb.append("\n") else if (i < count) sb.append(" ")
        }
        insertText(sb.toString())
    }
}
