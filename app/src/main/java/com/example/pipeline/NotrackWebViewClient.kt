package com.example.pipeline

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class NotrackWebViewClient(
    private val webView: WebView?
) {
    companion object {
        suspend fun create(context: Context): NotrackWebViewClient = withContext(Dispatchers.Main) {
            try {
                NotrackWebViewClient(WebView(context))
            } catch (e: Throwable) {
                android.util.Log.w("NotrackWebViewClient", "WebView init fallback: ${e.message}")
                try {
                    NotrackWebViewClient(WebView(context.applicationContext))
                } catch (t: Throwable) {
                    android.util.Log.w("NotrackWebViewClient", "WebView completely unavailable: ${t.message}")
                    NotrackWebViewClient(null)
                }
            }
        }

        enum class VideoNiche {
            HORROR_THRILLER,
            MYSTERY_CRIME,
            HISTORY_CULTURE,
            MOTIVATION_FACTS,
            AI_FUTURISTIC,
            SCIENCE_SPACE,
            BUSINESS_FINANCE,
            GAMING,
            ENTERTAINMENT,
            TECHNOLOGY,
            FITNESS_HEALTH,
            PSYCHOLOGY_SECRETS
        }

        fun resolveNiche(category: String): VideoNiche {
            val lower = category.lowercase().trim()
            val words = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
            return when {
                lower.contains("horror") || lower.contains("thriller") || lower.contains("scary") || lower.contains("creepy") || lower.contains("ghost") -> VideoNiche.HORROR_THRILLER
                lower.contains("crime") || lower.contains("detective") || lower.contains("unsolved") || lower.contains("investigat") || (lower.contains("mystery") && !lower.contains("history")) -> VideoNiche.MYSTERY_CRIME
                lower.contains("history") || lower.contains("ancient") || lower.contains("culture") || lower.contains("empire") || lower.contains("civilization") || lower.contains("archaeolog") || lower.contains("pharaoh") -> VideoNiche.HISTORY_CULTURE
                lower.contains("fitness") || lower.contains("health") || lower.contains("workout") || lower.contains("gym") || lower.contains("biohack") || lower.contains("body") -> VideoNiche.FITNESS_HEALTH
                lower.contains("psychology") || lower.contains("manipulat") || lower.contains("behavior") || lower.contains("dark secret") || lower.contains("subconscious") || (words.contains("mind") && !lower.contains("mindset")) -> VideoNiche.PSYCHOLOGY_SECRETS
                lower.contains("business") || lower.contains("finance") || lower.contains("money") || lower.contains("wealth") || lower.contains("millionaire") || lower.contains("market") || lower.contains("invest") -> VideoNiche.BUSINESS_FINANCE
                lower.contains("motivation") || lower.contains("mindset") || lower.contains("discipline") || lower.contains("storytelling") || lower.contains("success") -> VideoNiche.MOTIVATION_FACTS
                lower.contains("gaming") || lower.contains("game") || lower.contains("esports") || lower.contains("gamer") -> VideoNiche.GAMING
                lower.contains("entertainment") || lower.contains("cinema") || lower.contains("movie") || lower.contains("hollywood") || lower.contains("pop culture") || lower.contains("lifestyle") -> VideoNiche.ENTERTAINMENT
                lower.contains("science") || lower.contains("space") || lower.contains("cosmos") || lower.contains("cosmic") || lower.contains("physics") || lower.contains("astronomy") || lower.contains("sci-fi") || lower.contains("scifi") -> VideoNiche.SCIENCE_SPACE
                lower.contains("tech") || lower.contains("software") || lower.contains("gadget") || lower.contains("computer") || lower.contains("hardware") -> VideoNiche.TECHNOLOGY
                words.contains("ai") || lower.contains("artificial intelligence") || lower.contains("futuristic") || lower.contains("robot") || lower.contains("robotics") || lower.contains("cyber") || lower.contains("android") -> VideoNiche.AI_FUTURISTIC
                else -> VideoNiche.ENTERTAINMENT
            }
        }

        fun getCategoryPromptGuidance(category: String): String {
            return when (resolveNiche(category)) {
                VideoNiche.HORROR_THRILLER ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on the HORROR & THRILLER genre. Write a truly spine-chilling, terrifying, dark, suspenseful script with eerie psychological tension or paranormal fear. DO NOT write about science, general facts, or modern technology."

                VideoNiche.MYSTERY_CRIME ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on MYSTERY & CRIME. Write an intriguing, suspenseful script about an unsolved criminal case, baffling detective enigma, stolen artifact, or shocking investigation. DO NOT deviate into unrelated topics."

                VideoNiche.HISTORY_CULTURE ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on HISTORY & CULTURE. Write a fascinating, epic script exploring ancient civilizations, forgotten empires, legendary historic rulers, monumental architectural wonders, or hidden archaeological discoveries. DO NOT write about modern AI or tech."

                VideoNiche.MOTIVATION_FACTS ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on MOTIVATION & MINDSET. Write a powerful, electrifying, fire-igniting script about ruthless discipline, the 1% winner mentality, overcoming adversity, and psychological truths about human greatness."

                VideoNiche.AI_FUTURISTIC ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on AI & FUTURISTIC CONCEPTS. Write about artificial superintelligence, autonomous humanoid robotics, neural networks, futuristic year 2050 timeline, and human-machine evolution."

                VideoNiche.SCIENCE_SPACE ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on SCIENCE & SPACE. Write about mind-expanding cosmic phenomena, supermassive black holes, quantum paradoxes, deep ocean abysses, or fundamental physics revelations."

                VideoNiche.BUSINESS_FINANCE ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on BUSINESS & FINANCE. Write about wealth accumulation psychology, billionaire habits, market strategies, startup empires, and financial sovereignty."

                VideoNiche.GAMING ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on GAMING. Write about shocking video game lore, legendary gaming moments, secret easter eggs, developer glitches, and gaming culture."

                VideoNiche.ENTERTAINMENT ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on ENTERTAINMENT & CINEMA. Write about captivating movie trivia, hidden director details, Hollywood secrets, or pop culture phenomena."

                VideoNiche.TECHNOLOGY ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on TECHNOLOGY. Write about groundbreaking gadgets, hardware engineering, computing revolutions, and cutting-edge tech innovations."

                VideoNiche.FITNESS_HEALTH ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on FITNESS, HEALTH & BIOHACKING. Write about physical stamina, muscle growth secrets, intermittent fasting, metabolic hacks, and athletic transformation."

                VideoNiche.PSYCHOLOGY_SECRETS ->
                    "STRICT CATEGORY MANDATE: This video MUST be 100% focused on PSYCHOLOGY & HUMAN BEHAVIOR. Write about subconscious mental tricks, dark psychology signs, body language analysis, and hidden behavioral secrets."
            }
        }

        fun getCategoryVisualGuidance(category: String): String {
            return when (resolveNiche(category)) {
                VideoNiche.HORROR_THRILLER ->
                    "VISUAL ATMOSPHERE: Dark, misty, eerie, cinematic gothic horror. Shadowy silhouettes, abandoned haunted halls, candlelit terror, cold blue and desaturated tones. NO bright tech or cheerful lighting."

                VideoNiche.MYSTERY_CRIME ->
                    "VISUAL ATMOSPHERE: Noir rainy city streets, detective trenchcoat silhouettes under streetlamps, evidence boards with red strings, locked steel safes, magnifying glass on clues, high contrast dramatic shadows."

                VideoNiche.HISTORY_CULTURE ->
                    "VISUAL ATMOSPHERE: Ancient stone temples, majestic golden pyramids, royal pharaoh chambers, flickering wall torches, ancient papyrus scrolls, desert ruins at sunset, epic historical scale. NO modern tech."

                VideoNiche.MOTIVATION_FACTS ->
                    "VISUAL ATMOSPHERE: Lone athlete training in the rain, dramatic mountain summit at sunrise with golden rays, determined human portraits, symbolic chess kings, intense willpower and triumph."

                VideoNiche.AI_FUTURISTIC ->
                    "VISUAL ATMOSPHERE: Sleek humanoid androids, glowing neural network synapses, neon cyberpunk cityscapes with flying vehicles, holographic transparent screens."

                VideoNiche.SCIENCE_SPACE ->
                    "VISUAL ATMOSPHERE: Swirling cosmic black holes with luminous accretion disks, deep space nebulae, subatomic quantum particle collisions, orbital views of planets."

                VideoNiche.BUSINESS_FINANCE ->
                    "VISUAL ATMOSPHERE: High-rise Manhattan boardroom overlooking skyscraper skyline, gold bullion in bank vault, electronic financial trading ticker, bespoke luxury suits."

                VideoNiche.GAMING ->
                    "VISUAL ATMOSPHERE: High-tech esports arena with stadium crowds, fantasy RPG open world with dragons, high-end liquid-cooled RGB gaming battlestations."

                VideoNiche.ENTERTAINMENT ->
                    "VISUAL ATMOSPHERE: Red carpet Hollywood premiere spotlights, behind-the-scenes cinema film sets with heavy cameras, vibrant stage lighting."

                VideoNiche.TECHNOLOGY ->
                    "VISUAL ATMOSPHERE: Sleek modern research laboratory, macro close-up of glowing silicon microprocessors, translucent holographic computing interfaces."

                VideoNiche.FITNESS_HEALTH ->
                    "VISUAL ATMOSPHERE: High-intensity athletic training in dynamic gym lighting, pristine outdoor sprint on mountain ridge at sunrise, nutritious meal prep, ice bath recovery immersion, biometric vitals telemetry."

                VideoNiche.PSYCHOLOGY_SECRETS ->
                    "VISUAL ATMOSPHERE: Moody cinematic chiaroscuro silhouette, extreme macro pupil reflection, translucent psychological chess game, optical illusion vortex, deep subconscious mind visual metaphor."
            }
        }

        fun buildMetadataPrompt(
            request: VideoRequest
        ): String {
            val topic = request.topic.trim()
            require(topic.isNotBlank() && !topic.startsWith("Auto", ignoreCase = true)) {
                "A real topic must be supplied before generating video metadata."
            }
            val topicLine = "$topic in category '${request.category}'"
            val categoryMandate = getCategoryPromptGuidance(request.category)
            val lang = request.language
            val targetWords = (request.targetDurationSec * 2.5).toInt().coerceIn(120, 200)

            return """
You are a scriptwriting assistant. Produce a draft that must be reviewed against reliable sources before use.
Topic: $topicLine
Niche: ${request.category}
Language: $lang
Duration: ${request.targetDurationSec} seconds (~$targetWords words)
Style: ${request.style}

$categoryMandate

Write a video package based only on the supplied topic and context. Do not invent sources, events, statistics, quotes, or claims of fact. Use cautious wording when information is uncertain.
Rules:
1. 'narration': Write the complete spoken script in $lang, targeting approximately $targetWords words.
2. 'title': Provide a concise, accurate title in $lang or English (max 100 characters).
3. 'summary': Summarize only information supported by the supplied topic and context.
4. 'description': Write a concise, neutral summary of the planned video.
5. 'story': Describe the video's planned narrative without adding factual claims.

Output ONLY a valid JSON object matching this structure:
{
  "title": "",
  "summary": "",
  "description": "",
  "story": "",
  "narration": ""
}
""".trimIndent()
        }


    }

    init {
        setupWebView()
    }

    suspend fun close() = withContext(Dispatchers.Main) {
        webView?.let { view ->
            view.stopLoading()
            (view.parent as? android.view.ViewGroup)?.removeView(view)
            view.destroy()
        }
    }

    // =========================================================
    // WEBVIEW SETUP
    // =========================================================
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = webView ?: return
        try {
            // Enforce software layer to prevent Mesa GPU rendernode errors
            wv.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.databaseEnabled = true
            wv.settings.loadsImagesAutomatically = true
            wv.settings.allowContentAccess = true
            wv.settings.allowFileAccess = true
            wv.settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.240105.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36"
            try {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
            } catch (e: Throwable) {
                android.util.Log.w("NotrackWebViewClient", "CookieManager setup warning: ${e.message}")
            }
            wv.webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                    android.util.Log.w("NotrackWebViewClient", "Renderer process gone handled safely (didCrash=${detail?.didCrash()})")
                    return true
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("NotrackWebViewClient", "setupWebView failed: ${e.message}")
        }
    }

    // =========================================================
    // METADATA
    // =========================================================
    suspend fun generateMetadata(
        request: VideoRequest
    ): VideoMetadata {
        if (!request.customNarration.isNullOrBlank()) {
            val topic = request.topic.trim()
            require(topic.isNotBlank()) {
                "A real topic must be supplied with custom narration."
            }
            val title = request.customHook?.trim()?.takeIf { it.isNotBlank() } ?: topic
            return VideoMetadata(
                title = title,
                summary = request.customNarration.take(150),
                description = request.customNarration.trim(),
                story = request.customNarration,
                narration = request.customNarration
            )
        }
        val prompt = buildMetadataPrompt(request)
        val jsonObject = askNotrackForJsonObject(prompt, timeout = 90_000)
        return parseMetadata(jsonObject, request)
    }

    // =========================================================
    // VISUAL PROMPTS
    // =========================================================
    suspend fun generateVisualPrompts(
        request: VideoRequest,
        story: String,
        narration: String,
        actualAudioDuration: Double,
        visualCount: Int
    ): List<VisualPrompt> {
        val prompt = buildVisualPromptRequest(
            request = request,
            story = story,
            narration = narration,
            actualAudioDuration = actualAudioDuration,
            visualCount = visualCount
        )
        val jsonArray = askNotrackForJsonArray(prompt, timeout = 35_000)
        return parseVisualPrompts(
            response = jsonArray,
            visualCount = visualCount,
            audioDuration = actualAudioDuration,
            imageDurationSec = request.imageDurationSec
        )
    }

    // =========================================================
    // ASKING NOTRACK WITH PURE DOM AUTOMATION SCRAPER
    // Open URL -> Paste Prompt -> Submit -> Live Check & Fetch
    // =========================================================
    suspend fun askNotrackForJsonObject(
        prompt: String,
        timeout: Long = 45_000
    ): String {
        // Step 1: Open URL
        loadPage("https://notrack.ai/")

        // Step 2: Try fast in-page DOM dispatch or proceed with DOM input
        val fastResult = tryDirectInPageDispatch(prompt, timeout = 25_000)
        if (fastResult.isNotBlank()) {
            val candidate = tryExtractJsonObject(fastResult)
            if (candidate != null) return candidate
            val repaired = tryRepairJsonObject(fastResult)
            if (repaired != null) return repaired
        }

        // Step 3: DOM input element, paste prompt, submit and live check
        waitForInputElement(15_000)
        val oldCount = getResponseCount()
        setPrompt(prompt)
        waitForSendButton()
        clickSend()
        return waitForClosedJsonObject(oldCount = oldCount, timeout = timeout)
    }

    private suspend fun askNotrackForJsonArray(
        prompt: String,
        timeout: Long = 45_000
    ): String {
        // Step 1: Open URL
        loadPage("https://notrack.ai/")

        // Step 2: Try fast in-page DOM dispatch or proceed with DOM input
        val fastResult = tryDirectInPageDispatch(prompt, timeout = 25_000)
        if (fastResult.isNotBlank()) {
            val candidate = tryExtractJsonArray(fastResult)
            if (candidate != null) return candidate
            val repaired = tryRepairJsonArray(fastResult)
            if (repaired != null) return repaired
        }

        // Step 3: DOM input element, paste prompt, submit and live check
        waitForInputElement(25_000)
        val oldCount = getResponseCount()
        setPrompt(prompt)
        waitForSendButton()
        clickSend()
        return waitForClosedJsonArray(oldCount = oldCount, timeout = timeout)
    }

    suspend fun askNotrackForText(
        prompt: String,
        timeout: Long = 60_000
    ): String {
        // Step 1: Open URL
        loadPage("https://notrack.ai/")

        // Step 2: Try fast in-page DOM dispatch or proceed with DOM input
        val fastResult = tryDirectInPageDispatch(prompt, timeout = 25_000)
        if (fastResult.isNotBlank()) {
            return fastResult
        }

        // Step 3: DOM input element, paste prompt, submit and live check
        waitForInputElement(25_000)
        val oldCount = getResponseCount()
        setPrompt(prompt)
        waitForSendButton()
        clickSend()
        return waitForClosedTextResponse(oldCount = oldCount, timeout = timeout)
    }

    private suspend fun tryDirectInPageDispatch(prompt: String, timeout: Long = 40_000): String {
        val safePrompt = JSONObject.quote(prompt)
        eval(
            """
            (() => {
                window.__notrackDone = false;
                window.__notrackResult = null;
                window.__notrackError = null;

                (async () => {
                    try {
                        const resp = await fetch('/api/dispatch', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                user_input: $safePrompt,
                                mode: 'usual',
                                model: 'C',
                                persona: 'default',
                                max_turns: 1,
                                chat_id: '',
                                attachments: [],
                                regenerate: false,
                                edit: false,
                                via: 'typed'
                            })
                        });
                        if (!resp.ok) {
                            window.__notrackError = 'HTTP ' + resp.status;
                            window.__notrackDone = true;
                            return;
                        }
                        const reader = resp.body.getReader();
                        const dec = new TextDecoder();
                        let buf = '';
                        let full = '';
                        while (true) {
                            const { done, value } = await reader.read();
                            if (done) break;
                            buf += dec.decode(value, { stream: true });
                            let idx;
                            while ((idx = buf.indexOf('\n\n')) !== -1) {
                                const block = buf.slice(0, idx);
                                buf = buf.slice(idx + 2);
                                const line = block.split('\n').find(l => l.startsWith('data: '));
                                if (!line) continue;
                                try {
                                    const evt = JSON.parse(line.slice(6));
                                    if (evt.type === 'delta' && evt.chunk) full += evt.chunk;
                                    else if ((evt.type === 'message' || evt.type === 'consensus') && evt.content) full = evt.content;
                                } catch(e) {}
                            }
                        }
                        window.__notrackResult = full;
                    } catch(e) {
                        window.__notrackError = e.message;
                    } finally {
                        window.__notrackDone = true;
                    }
                })();
            })();
            """.trimIndent()
        )

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val done = evalBoolean("(() => window.__notrackDone === true)();")
            if (done) {
                val res = eval("(() => window.__notrackResult || '')();")
                val err = eval("(() => window.__notrackError || '')();")
                if (res.isNotBlank()) {
                    return res
                }
                if (err.isNotBlank()) {
                    android.util.Log.w("NotrackWebViewClient", "Notrack in-page dispatch note: $err")
                    break
                }
            }
            delay(500)
        }
        return ""
    }

    suspend fun askNotrackForViralScriptBreakdown(
        topic: String,
        category: String,
        language: String = "Hindi",
        targetDurationSec: Int = 60,
        globalFallbackEnabled: Boolean = false
    ): com.example.util.ViralScriptBreakdown {
        return try {
            val isHindi = language.contains("Hindi", ignoreCase = true)
            val langInstruction = if (isHindi) "Write all voiceover and hook strictly in natural Hindi (Devanagari script or conversational Hindi)." else "Write strictly in high-energy English."
            val niche = resolveNiche(category)
            val prompt = """
                You are a short-form video scriptwriter for $niche ($category).
                Create an authentic, non-generic, high-retention viral script specifically focused on:
                Topic: "$topic"
                Category: "$category"
                Duration: $targetDurationSec seconds
                Language Rule: $langInstruction

                Strict requirements:
                - Do NOT use generic placeholder or conspiracy templates. Write tailored content specifically explaining, breaking down, and showcasing "$topic".
                - 0-5s Pattern Interrupt Hook: Extreme curiosity.
                - 4 Scene Breakdown: Visual descriptions, spoken voiceover text, SFX cues, and retention angle.
                - End Screen CTA and 8K Photorealistic Visual Prompt.

                Output ONLY a valid JSON object matching this schema:
                {
                  "viralHook": "Short high-energy curiosity hook",
                  "fullNarrationText": "Full cohesive narration voiceover",
                  "endScreenCallToAction": "Call to action question or comment invite",
                  "visualPrompt8k": "8K cinematic prompt for visual generator",
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "timestampRange": "00:00 - 00:08",
                      "visualCue": "Specific visual camera angle and action for $topic",
                      "voiceoverText": "Spoken voiceover for scene 1",
                      "sfxCue": "Sound effect cue",
                      "retentionAngle": "Pattern Interrupt"
                    },
                    {
                      "sceneNumber": 2,
                      "timestampRange": "00:08 - 00:20",
                      "visualCue": "Specific visual camera angle and action for $topic",
                      "voiceoverText": "Spoken voiceover for scene 2",
                      "sfxCue": "Sound effect cue",
                      "retentionAngle": "Intrigue & Deep Dive"
                    },
                    {
                      "sceneNumber": 3,
                      "timestampRange": "00:20 - 00:40",
                      "visualCue": "Specific visual camera angle and action for $topic",
                      "voiceoverText": "Spoken voiceover for scene 3",
                      "sfxCue": "Sound effect cue",
                      "retentionAngle": "Climax Breakthrough"
                    },
                    {
                      "sceneNumber": 4,
                      "timestampRange": "00:40 - 00:60",
                      "visualCue": "Specific visual camera angle and action for $topic",
                      "voiceoverText": "Spoken voiceover for scene 4",
                      "sfxCue": "Sound effect cue",
                      "retentionAngle": "Retention Loop & CTA"
                    }
                  ]
                }
            """.trimIndent()

            val jsonString = askNotrackForJsonObject(prompt, timeout = 40_000)
            val json = JSONObject(jsonString)
            val hook = json.optString("viralHook", "").trim()
            val fullNarration = json.optString("fullNarrationText", "").trim()
            val cta = json.optString("endScreenCallToAction", "").trim()
            val prompt8k = json.optString("visualPrompt8k", "").trim()
            val scenesArray = json.optJSONArray("scenes")

            val sceneList = mutableListOf<com.example.util.SceneBreakdownItem>()
            if (scenesArray != null && scenesArray.length() > 0) {
                for (i in 0 until scenesArray.length()) {
                    val obj = scenesArray.getJSONObject(i)
                    sceneList.add(
                        com.example.util.SceneBreakdownItem(
                            sceneNumber = obj.optInt("sceneNumber", i + 1),
                            timestampRange = obj.optString("timestampRange", "00:00 - 00:15"),
                            visualCue = obj.optString("visualCue", "Cinematic view of $topic"),
                            voiceoverText = obj.optString("voiceoverText", ""),
                            sfxCue = obj.optString("sfxCue", "⚡ Whoosh"),
                            retentionAngle = obj.optString("retentionAngle", "Engagement")
                        )
                    )
                }
            }

            if (hook.isNotBlank() && sceneList.isNotEmpty()) {
                com.example.util.ViralScriptBreakdown(
                    topic = topic,
                    category = category,
                    language = language,
                    viralHook = hook,
                    totalEstimatedDurationSec = targetDurationSec,
                    fullNarrationText = fullNarration.ifBlank { sceneList.joinToString(" ") { it.voiceoverText } },
                    sceneBreakdown = sceneList,
                    endScreenCallToAction = cta,
                    visualPrompt8k = prompt8k
                )
            } else {
                com.example.util.ViralScriptGenerator.generateViralScript(topic, category, language, targetDurationSec)
            }
        } catch (e: Exception) {
            android.util.Log.w("NotrackWebViewClient", "askNotrackForViralScriptBreakdown note: ${e.message}")
            if (!globalFallbackEnabled) throw e
            com.example.util.ViralScriptGenerator.generateViralScript(topic, category, language, targetDurationSec)
        }
    }

    private suspend fun waitForClosedTextResponse(
        oldCount: Int,
        timeout: Long = 60_000
    ): String {
        val start = System.currentTimeMillis()
        var lastValidText = ""
        var stableCount = 0

        while (System.currentTimeMillis() - start < 10_000) {
            val count = getResponseCount()
            if (count > oldCount) break
            delay(400)
        }

        while (System.currentTimeMillis() - start < timeout) {
            val raw = getLatestResponseText().trim()
            if (raw.isNotBlank()) {
                if (raw == lastValidText) {
                    stableCount++
                    if (stableCount >= 2) {
                        return raw
                    }
                } else {
                    lastValidText = raw
                    stableCount = 0
                }
            }
            delay(800)
        }

        if (lastValidText.isNotBlank()) {
            return lastValidText
        }
        val finalRaw = getLatestResponseText().trim()
        if (finalRaw.isNotBlank()) return finalRaw
        throw IllegalStateException("Timed out waiting for text response from Notrack")
    }

    // =========================================================
    // LOAD PAGE
    // =========================================================
    private suspend fun loadPage(
        url: String
    ) {
        val wv = webView ?: throw IllegalStateException("WebView not available")
        val finished = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            try {
                wv.webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                        android.util.Log.w("NotrackWebViewClient", "Renderer process gone in loadPage (didCrash=${detail?.didCrash()})")
                        if (!finished.isCompleted) {
                            finished.complete(false)
                        }
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!finished.isCompleted) {
                            finished.complete(true)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        if (!finished.isCompleted) {
                            finished.complete(true)
                        }
                    }
                }
                wv.loadUrl(url)
            } catch (e: Throwable) {
                if (!finished.isCompleted) finished.complete(false)
            }
        }

        withTimeoutOrNull(45_000) {
            finished.await()
        }
        delay(2000)
    }

    // =========================================================
    // WAIT INPUT ELEMENT
    // =========================================================
    private suspend fun waitForInputElement(timeout: Long) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val found = evalBoolean(
                """
                (() => {
                    const el = document.querySelector('#field') || 
                               document.querySelector('textarea') || 
                               document.querySelector('input[type="text"]') ||
                               document.querySelector('[contenteditable="true"]');
                    return !!el;
                })();
                """.trimIndent()
            )
            if (found) {
                return
            }
            delay(500)
        }
        throw IllegalStateException("Notrack input element not found")
    }

    // =========================================================
    // SET PROMPT
    // =========================================================
    private suspend fun setPrompt(
        prompt: String
    ) {
        val safePrompt = JSONObject.quote(prompt)
        val success = evalBoolean(
            """
            (() => {
                const field = document.querySelector('#field') || 
                              document.querySelector('textarea') || 
                              document.querySelector('input[type="text"]') ||
                              document.querySelector('[contenteditable="true"]');
                if (!field) {
                    return false;
                }
                field.focus();
                
                if (field.tagName === 'TEXTAREA' || field.tagName === 'INPUT') {
                    field.value = $safePrompt;
                } else {
                    field.innerText = $safePrompt;
                }
                field.dispatchEvent(new Event('input', { bubbles: true }));
                field.dispatchEvent(new Event('change', { bubbles: true }));
                field.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true }));
                return true;
            })();
            """.trimIndent()
        )
        if (!success) {
            throw IllegalStateException("Could not enter prompt into Notrack")
        }
    }

    // =========================================================
    // WAIT SEND BUTTON
    // =========================================================
    private suspend fun waitForSendButton() {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 30_000) {
            val ready = evalBoolean(
                """
                (() => {
                    const button = document.querySelector('#goBtn') || 
                                   document.querySelector('button[type="submit"]') ||
                                   document.querySelector('form button') ||
                                   document.querySelector('.send-btn') ||
                                   document.querySelector('button');
                    if (!button) {
                        return false;
                    }
                    return !button.disabled;
                })();
                """.trimIndent()
            )
            if (ready) {
                return
            }
            delay(300)
        }
        throw IllegalStateException("Notrack send button not ready")
    }

    // =========================================================
    // CLICK SEND
    // =========================================================
    private suspend fun clickSend() {
        val clicked = evalBoolean(
            """
            (() => {
                const field = document.querySelector('#field') || document.querySelector('textarea');
                const v = field ? (field.value || field.innerText || '').trim() : '';

                if (typeof startDebate === 'function' && v) {
                    try {
                        startDebate(v, { via: 'typed' });
                        return true;
                    } catch(e) {}
                }

                const button = document.querySelector('#goBtn') || 
                               document.querySelector('button[type="submit"]') ||
                               document.querySelector('form button') ||
                               document.querySelector('.send-btn') ||
                               document.querySelector('button');
                if (button && !button.disabled) {
                    try {
                        button.click();
                        return true;
                    } catch(e) {}
                }

                const form = document.querySelector('#entry') || document.querySelector('form');
                if (form) {
                    try {
                        if (typeof form.requestSubmit === 'function') {
                            form.requestSubmit();
                        } else {
                            form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
                        }
                        return true;
                    } catch(e) {}
                }

                if (field) {
                    field.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                    return true;
                }

                return false;
            })();
            """.trimIndent()
        )
        if (!clicked) {
            throw IllegalStateException("Notrack send button not found")
        }
    }

    // =========================================================
    // RESPONSE COUNT
    // =========================================================
    private suspend fun getResponseCount(): Int {
        return evalInt(
            """
            (() => {
                const nodes = document.querySelectorAll('.message-body') ||
                              document.querySelectorAll('.bubble') ||
                              document.querySelectorAll('.markdown-body') ||
                              document.querySelectorAll('.chat-bubble');
                return nodes.length;
            })();
            """.trimIndent()
        )
    }

    // =========================================================
    // WAIT CLOSED JSON OBJECT (LIVE STREAMING CHECK)
    // =========================================================
    private suspend fun waitForClosedJsonObject(
        oldCount: Int,
        timeout: Long = 90_000
    ): String {
        val start = System.currentTimeMillis()
        var lastValidJson: String? = null
        var stableCount = 0

        // Wait up to 10 seconds for a new response bubble to begin appearing
        while (System.currentTimeMillis() - start < 10_000) {
            val count = getResponseCount()
            if (count > oldCount) break
            delay(400)
        }

        while (System.currentTimeMillis() - start < timeout) {
            val rawResponse = getLatestResponseText().trim()
            if (rawResponse.length > 30) {
                val candidateJson = tryExtractJsonObject(rawResponse)
                if (candidateJson != null) {
                    try {
                        val parsed = JSONObject(candidateJson)
                        val title = parsed.optString("title", "").trim()
                        val narration = parsed.optString("narration", "").trim()
                        val story = parsed.optString("story", "").trim()

                        if (title.isNotBlank() && (narration.isNotBlank() || story.isNotBlank())) {
                            if (candidateJson == lastValidJson) {
                                stableCount++
                                if (stableCount >= 2) {
                                    return candidateJson
                                }
                            } else {
                                lastValidJson = candidateJson
                                stableCount = 0
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            delay(800)
        }

        if (lastValidJson != null) {
            return lastValidJson
        }

        val finalRaw = getLatestResponseText().trim()
        val repaired = tryRepairJsonObject(finalRaw)
        if (repaired != null) {
            return repaired
        }

        throw IllegalStateException("Timed out waiting for closed JSON object from Notrack")
    }

    // =========================================================
    // WAIT CLOSED JSON ARRAY (LIVE STREAMING CHECK)
    // =========================================================
    private suspend fun waitForClosedJsonArray(
        oldCount: Int,
        timeout: Long = 90_000
    ): String {
        val start = System.currentTimeMillis()
        var lastValidArray: String? = null
        var stableCount = 0

        // Wait up to 10 seconds for a new response bubble to begin appearing
        while (System.currentTimeMillis() - start < 10_000) {
            val count = getResponseCount()
            if (count > oldCount) break
            delay(400)
        }

        while (System.currentTimeMillis() - start < timeout) {
            val rawResponse = getLatestResponseText().trim()
            if (rawResponse.length > 20) {
                val candidateArray = tryExtractJsonArray(rawResponse)
                if (candidateArray != null) {
                    try {
                        val parsed = JSONArray(candidateArray)
                        if (parsed.length() > 0) {
                            if (candidateArray == lastValidArray) {
                                stableCount++
                                if (stableCount >= 2) {
                                    return candidateArray
                                }
                            } else {
                                lastValidArray = candidateArray
                                stableCount = 0
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            delay(800)
        }

        if (lastValidArray != null) {
            return lastValidArray
        }

        val finalRaw = getLatestResponseText().trim()
        val repaired = tryRepairJsonArray(finalRaw)
        if (repaired != null) {
            return repaired
        }

        throw IllegalStateException("Timed out waiting for closed JSON array from Notrack")
    }

    // =========================================================
    // GET LATEST RESPONSE
    // =========================================================
    private suspend fun getLatestResponseText(): String {
        return eval(
            """
            (() => {
                let nodes = Array.from(document.querySelectorAll('.message-body'));
                if (!nodes.length) {
                    nodes = Array.from(document.querySelectorAll('.markdown-body'));
                }
                if (!nodes.length) {
                    nodes = Array.from(document.querySelectorAll('.bubble .message-body'));
                }
                if (!nodes.length) {
                    nodes = Array.from(document.querySelectorAll('.response, .chat-response, .ai-message'));
                }
                if (!nodes.length) {
                    return '';
                }
                const last = nodes[nodes.length - 1];
                const clone = last.cloneNode(true);
                clone.querySelectorAll('details.think, .think, .typing, .reasoning-body, .stream-typing').forEach(el => el.remove());
                return (clone.innerText || clone.textContent || '').trim();
            })();
            """.trimIndent()
        )
    }



    // =========================================================
    // VISUAL PROMPT REQUEST
    // =========================================================
    private fun buildVisualPromptRequest(
        request: VideoRequest,
        story: String,
        narration: String,
        actualAudioDuration: Double,
        visualCount: Int
    ): String {
        val topicLine = request.topic.trim()
        require(topicLine.isNotBlank() && !topicLine.startsWith("Auto", ignoreCase = true)) {
            "A real topic must be supplied before generating visual prompts."
        }
        require(actualAudioDuration.isFinite() && actualAudioDuration > 0.0) {
            "A positive audio duration is required before generating visual prompts."
        }
        require(visualCount > 0 && request.imageDurationSec > 0) {
            "A positive scene count and image duration are required before generating visual prompts."
        }
        val step = request.imageDurationSec.toDouble()
        val visualAtmosphere = getCategoryVisualGuidance(request.category)
        return """
You are a professional AI image prompt director and cinematographer.
Create exactly $visualCount consecutive visual scenes for this video.
Each scene must describe a unique, photorealistic visual moment matching the story context and narration script.

TOPIC: $topicLine
CATEGORY: ${request.category}
STYLE: ${request.style}
ASPECT RATIO: ${request.aspect}
TOTAL AUDIO DURATION: ${String.format(Locale.US, "%.2f", actualAudioDuration)} seconds
NUMBER OF SCENES: $visualCount (${request.imageDurationSec} seconds per scene)
STORY CONTEXT: $story
NARRATION SCRIPT: $narration

$visualAtmosphere

Return ONLY a valid JSON array of exactly $visualCount objects.
JSON Format Structure:
[
  {
    "index": 1,
    "startSec": 0.0,
    "endSec": $step,
    "purpose": "Opening Hook Visual",
    "prompt": "Cinematic 8k photograph of scene 1, dramatic lighting, highly detailed masterpiece in ${request.style} style, ${request.aspect} vertical format"
  }
]

Strict Rules:
1. Return EXACTLY $visualCount items in a single valid JSON array starting with '[' and ending with ']'.
2. Write complete, vivid visual descriptions for each scene. Do NOT output template place-holders.
3. No markdown formatting, no commentary outside JSON.
""".trimIndent()
    }

    // =========================================================
    // PARSE METADATA & NARRATION SANITIZER
    // =========================================================
    private fun isPlaceholderText(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.isBlank()) return true
        return lower.contains("full engaging voice-over") ||
               lower.contains("script written entirely in") ||
               lower.contains("with strong hook") ||
               lower.contains("a high-ctr") ||
               lower.contains("click-worthy") ||
               lower.contains("clickable") ||
               lower.contains("viral click") ||
               lower.contains("title with 1 emoji") ||
               lower.contains("short viral") ||
               lower.contains("viral title") ||
               lower.contains("2-sentence") ||
               lower.contains("detailed visual storyboard") ||
               lower.contains("engaging video description") ||
               lower.contains("engaging video description") ||
               lower.contains("insert topic") ||
               lower.contains("placeholder") ||
               lower.contains("word-for-word voiceover script") ||
               lower.contains("complete spoken script") ||
               lower.contains("catchy viral title") ||
               lower.contains("tag1") ||
               lower == "title" ||
               lower == "narration" ||
               lower == "summary" ||
               lower == "description"
    }

    internal fun parseMetadata(
        response: String,
        request: VideoRequest
    ): VideoMetadata {
        val defaultCategory = request.category
        val jsonText = extractJsonObject(response)
        val json = JSONObject(jsonText)
        val rawTitle = json.optString("title", "").trim()
        val rawSummary = json.optString("summary", "").trim()
        val rawDesc = json.optString("description", "").trim()
        val rawStory = json.optString("story", "").trim()
        val rawNarration = json.optString("narration", "").trim()

        val narrationWordCount = rawNarration.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        require(rawTitle.length in 5..100 && !isPlaceholderText(rawTitle)) {
            "The metadata provider returned a missing, invalid, or placeholder title."
        }
        require(rawSummary.isNotBlank() && !isPlaceholderText(rawSummary)) {
            "The metadata provider returned a missing or placeholder summary."
        }
        require(rawDesc.isNotBlank() && !isPlaceholderText(rawDesc)) {
            "The metadata provider returned a missing or placeholder description."
        }
        require(rawStory.isNotBlank() && !isPlaceholderText(rawStory)) {
            "The metadata provider returned a missing or placeholder story."
        }
        require(!isPlaceholderText(rawNarration) && narrationWordCount >= 25) {
            "The metadata provider returned missing, placeholder, or too-short narration."
        }

        return VideoMetadata(
            title = rawTitle,
            summary = rawSummary,
            description = rawDesc,
            story = rawStory,
            narration = rawNarration
        )
    }

    // =========================================================
    // PARSE VISUAL PROMPTS & PROMPT SANITIZER
    // =========================================================
    private fun isPlaceholderPrompt(prompt: String): Boolean {
        val lower = prompt.lowercase().trim()
        return lower.contains("highly detailed visual prompt in") ||
               lower.contains("describing the first scene based on") ||
               lower.contains("describing the next scene") ||
               lower.contains("[first part specific visual scene]") ||
               lower.contains("[next part specific visual scene]") ||
               lower.contains("format example") ||
               lower.length < 15
    }

    internal fun parseVisualPrompts(
        response: String,
        visualCount: Int,
        audioDuration: Double,
        imageDurationSec: Int
    ): List<VisualPrompt> {
        require(audioDuration.isFinite() && audioDuration > 0.0) {
            "A positive audio duration is required to parse visual prompts."
        }
        require(visualCount > 0 && imageDurationSec > 0) {
            "A positive scene count and image duration are required to parse visual prompts."
        }
        val jsonText = extractJsonArray(response)
        val array = JSONArray(jsonText)
        require(array.length() == visualCount) {
            "The visual prompt provider returned ${array.length()} scenes; expected $visualCount."
        }
        val result = mutableListOf<VisualPrompt>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val purpose = item.optString("purpose", "").trim()
            val prompt = item.optString("prompt", "").trim()
            require(purpose.isNotBlank()) {
                "The visual prompt provider returned an empty purpose for scene ${i + 1}."
            }
            require(!isPlaceholderPrompt(prompt)) {
                "The visual prompt provider returned an empty or placeholder prompt for scene ${i + 1}."
            }
            val start = i * imageDurationSec.toDouble()
            val end = minOf(start + imageDurationSec, audioDuration)
            result.add(
                VisualPrompt(
                    index = i + 1,
                    startSec = start,
                    endSec = if (i == visualCount - 1) audioDuration else end,
                    purpose = purpose,
                    prompt = prompt
                )
            )
        }
        return result
    }

    // =========================================================
    // JSON OBJECT EXTRACTION & SAFE CHECKING
    // =========================================================
    private fun tryExtractJsonObject(text: String): String? {
        val clean = cleanJsonText(text)
        var start = clean.indexOf('{')
        while (start >= 0) {
            var depth = 0
            var inString = false
            var escaped = false
            var end = -1
            for (i in start until clean.length) {
                val c = clean[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\' && inString) {
                    escaped = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (inString) continue
                if (c == '{') {
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
            if (end >= 0) {
                val candidate = clean.substring(start, end + 1)
                try {
                    JSONObject(candidate)
                    return candidate
                } catch (_: Exception) {
                    // Keep scanning in case a later `{...}` block is a valid JSON object.
                }
            }
            start = clean.indexOf('{', start + 1)
        }
        return null
    }

    private fun tryExtractJsonArray(text: String): String? {
        val clean = cleanJsonText(text)
        var start = clean.indexOf('[')
        while (start >= 0) {
            var depth = 0
            var inString = false
            var escaped = false
            var end = -1
            for (i in start until clean.length) {
                val c = clean[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\' && inString) {
                    escaped = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (inString) continue
                if (c == '[') {
                    depth++
                } else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
            if (end >= 0) {
                val candidate = clean.substring(start, end + 1)
                try {
                    JSONArray(candidate)
                    return candidate
                } catch (_: Exception) {
                    // Keep scanning in case a later `[...]` block is a valid JSON array.
                }
            }
            start = clean.indexOf('[', start + 1)
        }
        return null
    }

    private fun extractJsonObject(text: String): String {
        return tryExtractJsonObject(text)
            ?: tryRepairJsonObject(text)
            ?: throw IllegalStateException("No valid closed JSON object found in response")
    }

    private fun extractJsonArray(text: String): String {
        return tryExtractJsonArray(text)
            ?: tryRepairJsonArray(text)
            ?: throw IllegalStateException("No valid closed JSON array found in response")
    }

    private fun cleanJsonText(text: String): String {
        return text
            .replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<think>[\\s\\S]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("```[a-zA-Z0-9_-]*\\s*", RegexOption.IGNORE_CASE), "")
            .replace("```", "")
            .trim()
    }

    private fun tryRepairJsonObject(text: String): String? {
        val clean = cleanJsonText(text)
        var start = clean.indexOf('{')
        while (start >= 0) {
            var trimmed = clean.substring(start).trim()
            if (!trimmed.endsWith("}")) {
                val quoteCount = trimmed.count { it == '"' }
                if (quoteCount % 2 != 0) {
                    trimmed += "\""
                }
                trimmed += "\n}"
            }
            try {
                JSONObject(trimmed)
                return trimmed
            } catch (_: Exception) {
                // Try the next candidate start in case a previous `{` belongs to non-JSON prose.
            }
            start = clean.indexOf('{', start + 1)
        }
        return null
    }

    private fun tryRepairJsonArray(text: String): String? {
        val clean = cleanJsonText(text)
        var start = clean.indexOf('[')
        while (start >= 0) {
            var trimmed = clean.substring(start).trim()
            if (!trimmed.endsWith("]")) {
                val quoteCount = trimmed.count { it == '"' }
                if (quoteCount % 2 != 0) {
                    trimmed += "\""
                }
                trimmed += "\n]"
            }
            try {
                JSONArray(trimmed)
                return trimmed
            } catch (_: Exception) {
                // Try the next candidate start in case a previous `[` belongs to non-JSON prose.
            }
            start = clean.indexOf('[', start + 1)
        }
        return null
    }

    private suspend fun eval(
        script: String
    ): String {
        val wv = webView ?: return ""
        val result = CompletableDeferred<String>()
        withContext(Dispatchers.Main) {
            try {
                wv.evaluateJavascript(script) { value ->
                    val decoded = try {
                        if (value == null || value == "null") {
                            ""
                        } else {
                            JSONObject("""{"value":$value}""").getString("value")
                        }
                    } catch (_: Exception) {
                        value ?: ""
                    }
                    if (!result.isCompleted) result.complete(decoded)
                }
            } catch (e: Throwable) {
                if (!result.isCompleted) result.complete("")
            }
        }
        return result.await()
    }

    private suspend fun evalBoolean(
        script: String
    ): Boolean {
        return eval(script).equals("true", ignoreCase = true)
    }

    private suspend fun evalInt(
        script: String
    ): Int {
        return eval(script).toIntOrNull() ?: 0
    }

}
