package com.example.pipeline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.ceil

class VideoPipeline(
    private val context: Context,
    private val notrack: NotrackWebViewClient,
    private val deepAi: DeepAIWebViewClient,
    private val tts: VoiceGenerator,
    private val whisper: WhisperTranscriber,
    private val ffmpeg: FFmpegRenderer,
    private val pollinationsEnabled: Boolean = true,
    private val globalFallbackEnabled: Boolean = false
) {
    private fun isNetworkAvailable(context: Context): Boolean {
        return com.example.util.NetworkMonitor.isCurrentlyOnline(context)
    }

    private suspend fun <T> safeNetworkCall(
        progressPercentage: Int,
        onProgress: (Int, String) -> Unit,
        maxRetries: Int = 3,
        block: suspend () -> T
    ): T {
        var attempts = 0
        while (true) {
            attempts++
            waitForInternet(context, progressPercentage, onProgress)
            try {
                return block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cooperative coroutine cancellation must always be rethrown immediately
                throw e
            } catch (e: Exception) {
                if (!isNetworkError(e)) throw e
                if (attempts <= maxRetries) {
                    onProgress(
                        progressPercentage,
                        "Network request failed: ${e.localizedMessage?.take(50) ?: "connection error"}. Retrying on the current connection ($attempts/$maxRetries)."
                    )
                    delay(100)
                } else {
                    throw e
                }
            }
        }
    }

    private fun isNetworkError(e: Exception): Boolean {
        if (e is kotlinx.coroutines.CancellationException) return false
        if (!isNetworkAvailable(context)) return true
        if (e is java.net.UnknownHostException ||
            e is java.net.ConnectException ||
            e is java.net.NoRouteToHostException) {
            return true
        }
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("no internet") ||
               msg.contains("unable to resolve host") ||
               msg.contains("failed to connect to") ||
               msg.contains("network is unreachable")
    }

    private fun jsonToMetadata(json: org.json.JSONObject): VideoMetadata {
        return VideoMetadata(
            title = json.optString("title", ""),
            summary = json.optString("summary", ""),
            description = json.optString("description", ""),
            story = json.optString("story", ""),
            narration = json.optString("narration", "")
        )
    }

    private suspend fun waitForInternet(context: Context, progressPercentage: Int, onProgress: (Int, String) -> Unit) {
        var loggedWaiting = false
        while (!isNetworkAvailable(context)) {
            if (!loggedWaiting) {
                onProgress(
                    progressPercentage,
                    "[PAUSED_OFFLINE] ⚠️ No active internet. Video pipeline paused. Waiting to reconnect..."
                )
                loggedWaiting = true
            }
            delay(2500)
        }
        if (loggedWaiting) {
            onProgress(
                progressPercentage,
                "[ONLINE_RESUMED] 🟢 Internet restored! Resuming current video generation seamlessly..."
            )
            delay(1000)
        }
    }

    suspend fun generate(
        request: VideoRequest,
        onProgress: (Int, String) -> Unit
    ): PipelineResult = withContext(Dispatchers.IO) {
        require(request.topic.isNotBlank() && !request.topic.startsWith("Auto", ignoreCase = true)) {
            "Enter a real topic before starting video generation."
        }
        require(request.targetDurationSec in 15..300) {
            "Video duration must be between 15 and 300 seconds."
        }
        require(request.imageDurationSec > 0) {
            "Scene duration must be greater than zero seconds."
        }
        val projectId = "proj_${UUID.randomUUID()}"

        val projectDir = File(context.filesDir, "projects/$projectId")
        val imageDir = File(projectDir, "images")
        imageDir.mkdirs()

        /*
         * STEP 1: AI METADATA
         */
        val metadataFile = File(projectDir, "metadata.json")
        onProgress(5, "Step 1/8: Initializing AI DOM Engine for ${request.category}...")
        val metadata = safeNetworkCall(5, onProgress) {
            val result = notrack.generateMetadata(request)
            if (result.title.isBlank() || result.narration.isBlank()) {
                throw IllegalStateException("AI Engine returned empty or invalid metadata.")
            }
            result
        }
        metadataFile.writeText(metadataToJson(metadata).toString(2))

        onProgress(10, "=== METADATA GENERATED ===")
        onProgress(11, "[TITLE] ${metadata.title}")
        onProgress(12, "[SUMMARY] ${metadata.summary.take(120)}...")
        onProgress(13, "[NARRATION] ${metadata.narration.take(150)}...")

        /*
         * STEP 2: VOICEOVER
         */
        val audioFile = File(projectDir, "narration.wav")
        onProgress(16, "Generating voiceover (${request.voiceName})...")
        safeNetworkCall(16, onProgress) {
            tts.generate(
                text = metadata.narration,
                language = request.language,
                output = audioFile,
                voiceName = request.voiceName,
                targetDurationSec = request.targetDurationSec
            )
        }

        /*
         * STEP 3: AUDIO DURATION & SCENE PLANNING
         */
        onProgress(22, "Measuring audio duration and locking requested target duration (${request.targetDurationSec}s)...")
        val rawAudioDuration = ffmpeg.getDuration(audioFile)
        val targetSec = request.targetDurationSec.toDouble()
        // Honor the requested duration while allowing longer generated narration to extend the render.
        val audioDuration = if (rawAudioDuration >= targetSec * 0.85) {
            maxOf(rawAudioDuration, targetSec)
        } else {
            targetSec
        }
        val visualCount = ceil(audioDuration / request.imageDurationSec.toDouble()).toInt().coerceIn(3, 40)

        /*
         * STEP 4: VISUAL PROMPTS
         */
        val promptsFile = File(projectDir, "visual-prompts.json")
        onProgress(25, "Generating $visualCount visual prompts (${request.imageDurationSec} sec per scene)...")
        val visualPrompts = safeNetworkCall(25, onProgress) {
            val result = notrack.generateVisualPrompts(
                request = request,
                story = metadata.story,
                narration = metadata.narration,
                actualAudioDuration = audioDuration,
                visualCount = visualCount
            )
            require(result.size == visualCount) {
                "Expected $visualCount visual prompts, got ${result.size}"
            }
            result
        }

        promptsFile.writeText(
            JSONArray().apply {
                visualPrompts.forEach {
                    put(
                        JSONObject()
                            .put("index", it.index)
                            .put("startSec", it.startSec)
                            .put("endSec", it.endSec)
                            .put("purpose", it.purpose)
                            .put("prompt", it.prompt)
                    )
                }
            }.toString(2)
        )

        onProgress(28, "=== 3-SECOND VISUAL PROMPTS ($visualCount SCENES) ===")
        visualPrompts.forEach { vp ->
            onProgress(29, "[VISUAL #${vp.index}] (${vp.startSec}s - ${vp.endSec}s) [${vp.purpose}]: ${vp.prompt}")
        }

        /*
         * STEP 5: VISUAL ASSETS GENERATION (AI IMAGES vs HTML KINETIC MOTION ANIMATION)
         */
        val generatedImages = mutableListOf<File>()

        if (request.visualRenderMode.equals("HTML_CAPTION_ANIMATION", ignoreCase = true)) {
            onProgress(35, "🚀 [HTML_ANIMATION_MODE] Generating Cyberpunk HTML5/CSS3 Kinetic Motion Cards...")
            val motionPkg = com.example.util.HtmlMotionCardGenerator.generateMotionSlides(
                topic = metadata.title.ifBlank { request.topic },
                category = request.category,
                narrationText = metadata.narration,
                durationSec = audioDuration.toInt().coerceAtLeast(10),
                theme = request.htmlTheme
            )

            val htmlFile = File(projectDir, "motion_cards.html")
            try {
                htmlFile.writeText(motionPkg.htmlContent)
                onProgress(40, "Generated HTML Motion Template: ${htmlFile.name}")
            } catch (e: Exception) {
                android.util.Log.w("VideoPipeline", "Failed saving motion_cards.html: ${e.message}")
            }

            // Generate slide cards matching the aspect ratio
            val (targetWidth, targetHeight) = when (request.aspect) {
                "16:9" -> 1920 to 1080
                "1:1" -> 1080 to 1080
                else -> 1080 to 1920 // 9:16
            }

            motionPkg.slides.forEachIndexed { index, slide ->
                val progress = 42 + (((index + 1).toDouble() / motionPkg.slides.size) * 35).toInt()
                val output = File(imageDir, String.format("%03d.jpg", index + 1))
                onProgress(progress, "Rendering HTML Kinetic Motion Card ${index + 1}/${motionPkg.slides.size} [${slide.badgeText}]")

                val bmp = com.example.util.HtmlMotionCardGenerator.createSlideBitmap(
                    slide = slide,
                    width = targetWidth,
                    height = targetHeight,
                    theme = request.htmlTheme
                )

                java.io.FileOutputStream(output).use { fos ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, fos)
                }

                if (output.exists() && output.length() > 500) {
                    onProgress(progress, "[IMAGE_PREVIEW] ${index + 1}/$visualCount ${output.absolutePath}")
                }
                generatedImages.add(output)
            }
        } else {
            visualPrompts.forEachIndexed { index, visual ->
                val progress = 30 + (((index + 1).toDouble() / visualCount) * 45).toInt()
                val output = File(imageDir, String.format("%03d.jpg", index + 1))

                if (output.exists() && output.length() > 5000) {
                    onProgress(progress, "Step 5/8: Reusing already generated image ${index + 1}/$visualCount")
                } else {
                    onProgress(progress, "Generating image ${index + 1}/$visualCount")
                    safeNetworkCall(progress, onProgress) {
                        val aspectSuffix = when (request.aspect) {
                            "9:16" -> "vertical 9:16 portrait orientation full screen mobile aspect ratio"
                            "16:9" -> "horizontal 16:9 landscape widescreen orientation"
                            "1:1" -> "square 1:1 aspect ratio orientation"
                            else -> "vertical 9:16 portrait orientation"
                        }
                        val basePrompt = if (visual.prompt.contains("orientation", ignoreCase = true) || visual.prompt.contains("aspect ratio", ignoreCase = true)) {
                            visual.prompt
                        } else {
                            "${visual.prompt}, $aspectSuffix"
                        }
                        val promptWithAspect = "STRICTLY GENERATE AN IMAGE. DO NOT WRITE TEXT. Master prompt: Create a visually stunning, high-quality image for the following description. No logos, no text, no watermarks. Description: $basePrompt"
                        deepAi.generateImage(
                            prompt = promptWithAspect,
                            output = output,
                            pollinationsEnabled = pollinationsEnabled,
                            globalFallbackEnabled = globalFallbackEnabled,
                            onStepLog = { stepMsg ->
                                onProgress(progress, stepMsg)
                            }
                        )
                        if (!output.exists() || output.length() <= 500) {
                            throw IllegalStateException("Image file missing or too small after generation.")
                        }
                    }
                }

                if (output.exists() && output.length() > 500) {
                    onProgress(progress, "[IMAGE_PREVIEW] ${index + 1}/$visualCount ${output.absolutePath}")
                }
                generatedImages.add(output)
            }
        }

        /*
         * STEP 6: WHISPER CAPTIONS
         */
        val srtFile = File(projectDir, "captions.srt")
        if (srtFile.exists() && srtFile.length() > 10) {
            onProgress(80, "Step 6/8: Reusing existing synced captions...")
        } else {
            onProgress(80, "Creating captions...")
            safeNetworkCall(80, onProgress) {
                whisper.transcribe(
                    audio = audioFile,
                    outputSrt = srtFile,
                    language = languageCode(request.language),
                    narrationText = metadata.narration
                )
            }
        }

        /*
         * STEP 7: FFMPEG RENDER WITH BGM MIXING
         */
        val masterAudioFile = if (request.bgmEnabled && request.bgmTrackId.isNotBlank()) {
            onProgress(88, "Mixing royalty-free background music track...")
            val mixedAudio = File(projectDir, "master_audio_mixed.wav")
            try {
                com.example.util.FreeMusicManager.mixNarrationAndBgm(
                    context = context,
                    narrationFile = audioFile,
                    bgmTrackId = request.bgmTrackId,
                    bgmVolume = request.bgmVolume,
                    autoDucking = request.autoDucking,
                    outputFile = mixedAudio
                )
            } catch (_: Exception) {
                audioFile
            }
        } else {
            audioFile
        }

        onProgress(90, "Rendering final video with synced images, master audio, and captions...")
        val finalVideo = File(projectDir, "final.mp4")
        ffmpeg.render(
            images = generatedImages,
            audio = masterAudioFile,
            captions = srtFile,
            output = finalVideo,
            duration = audioDuration,
            aspect = request.aspect,
            imageDurationSec = request.imageDurationSec,
            showCaptions = request.showCaptions,
            captionStyle = request.captionStyle,
            captionHighlightColor = request.captionHighlightColor,
            captionFontFamily = request.captionFontFamily,
            captionStrokeWidth = request.captionStrokeWidth,
            captionGlow = request.captionGlow,
            captionWordsPerChunk = request.captionWordsPerChunk,
            captionUppercase = request.captionUppercase
        )

        /*
         * STEP 8: FINAL VALIDATION & THUMBNAIL
         */
        onProgress(96, "Validating final video output...")
        if (!finalVideo.exists() || finalVideo.length() < 1000L) {
            throw IllegalStateException("Final video rendering failed or output file is empty.")
        }

        val thumbnail = File(projectDir, "thumbnail.jpg")
        ffmpeg.createThumbnail(
            video = finalVideo,
            output = thumbnail
        )
        if (!thumbnail.exists() || thumbnail.length() == 0L) {
            generatedImages.firstOrNull()?.copyTo(thumbnail, overwrite = true)
        }

        val project = JSONObject()
            .put("projectId", projectId)
            .put("topic", request.topic)
            .put("duration", audioDuration)
            .put("visualCount", visualCount)
            .put("video", finalVideo.absolutePath)
            .put("thumbnail", thumbnail.absolutePath)
        File(projectDir, "project.json").writeText(
            project.toString(2)
        )

        onProgress(100, "Video completed")
        PipelineResult(
            projectId = projectId,
            video = finalVideo,
            thumbnail = thumbnail,
            metadata = metadata,
            audioDuration = audioDuration,
            visualCount = visualCount,
            visualPrompts = visualPrompts,
            images = generatedImages,
            captions = srtFile
        )
    }

    private fun languageCode(language: String): String? {
        val value = language.lowercase()
        return when {
            value.startsWith("hi") || value.contains("hindi") -> "hi"
            value.startsWith("en") || value.contains("english") -> "en"
            else -> null
        }
    }

    private fun metadataToJson(m: VideoMetadata): JSONObject {
        return JSONObject().apply {
            put("title", m.title)
            put("summary", m.summary)
            put("description", m.description)
            put("story", m.story)
            put("narration", m.narration)
        }
    }
}
