package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Data model for a single scene in the video synthesis pipeline.
 */
data class VideoSceneConfig(
    val index: Int,
    val imagePathOrUrl: String?,
    val animationFramePaths: List<String> = emptyList(),
    val animationFrameRate: Int = 4,
    val durationMs: Long = 3000L,
    val caption: String = "",
    val cameraMotion: String = "Slow push in",
    val transition: String = "random",
    val visualPrompt: String = "",
    val timeframe: String = "",
    val transitionEffect: VideoTransitionEffect? = null,
    val captionStyle: CaptionStyle? = null
)

/**
 * Full configuration for rendering a 9:16 vertical short video.
 */
data class VideoSynthesisConfig(
    val title: String = "Viral 9:16 Short",
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val scenes: List<VideoSceneConfig>,
    val audioPath: String? = null,
    val outputWidth: Int = 1080,
    val outputHeight: Int = 1920,
    val fps: Int = 30,
    val enableKenBurns: Boolean = true,
    val enableVignette: Boolean = true,
    val enableKineticCaptions: Boolean = true,
    val captionHighlightColorHex: String = "#00FF66",
    val voiceModel: String = "Edge TTS Neural",
    val emotion: String = "Energetic",
    val autoRandomTransitions: Boolean = true,
    val transitionCategoryFilter: String = "All",
    val globalCaptionStyle: CaptionStyle? = null,
    val customAssSubtitlePath: String? = null,
    val wordTimestamps: List<WordTimestamp> = emptyList()
)

object FFmpegVideoSynthesizer {

    private const val TAG = "FFmpegVideoSynthesizer"

    /**
     * Checks if the ffmpeg binary is available on the host/device.
     */
    fun isFFmpegAvailable(): Boolean {
        return try {
            val p = ProcessBuilder("ffmpeg", "-version").start()
            val exit = p.waitFor()
            exit == 0
        } catch (e: Exception) {
            try {
                val p = ProcessBuilder("/usr/bin/ffmpeg", "-version").start()
                val exit = p.waitFor()
                exit == 0
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun getFFmpegPath(): String {
        return if (File("/usr/bin/ffmpeg").exists()) "/usr/bin/ffmpeg" else "ffmpeg"
    }

    /**
     * Builds the complete FFmpeg command and standalone bash script for the project without needing context.
     * Incorporates automatic random transitions from the 100 transition effects library.
     */
    fun generateFFmpegScript(config: VideoSynthesisConfig): String {
        val scriptBuilder = StringBuilder()
        val allEffects = VideoTransitionRegistry.getEffects()
        
        scriptBuilder.append("#!/bin/bash\n")
        scriptBuilder.append("# ========================================================\n")
        scriptBuilder.append("# GlitchCut Viral 9:16 Shorts - Automated FFmpeg Pipeline\n")
        scriptBuilder.append("# Project: ${config.title}\n")
        scriptBuilder.append("# Total Scenes: ${config.scenes.size} | Resolution: ${config.outputWidth}x${config.outputHeight} @ ${config.fps}fps\n")
        scriptBuilder.append("# 100 Video Transitions Engine: Active (Randomized Multi-Input xfade)\n")
        scriptBuilder.append("# ========================================================\n\n")

        scriptBuilder.append("# Step 1: Render individual scenes with Ken Burns (ZoomPan) & 9:16 Scaling\n")
        config.scenes.forEachIndexed { i, sc ->
            val durationSec = (sc.durationMs / 1000.0).coerceAtLeast(1.0)
            val frames = (durationSec * config.fps).toInt()
            val zoomExpr = when {
                sc.cameraMotion.contains("push in", ignoreCase = true) || sc.cameraMotion.contains("zoom in", ignoreCase = true) ->
                    "z='min(zoom+0.0018,1.20)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                sc.cameraMotion.contains("rapid", ignoreCase = true) || sc.cameraMotion.contains("fast zoom", ignoreCase = true) ->
                    "z='min(zoom+0.0035,1.35)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                sc.cameraMotion.contains("pan down", ignoreCase = true) ->
                    "z='1.15':x='iw/2-(iw/zoom/2)':y='if(lte(on,1),0,min(ih/zoom,y+1.5))'"
                else -> "z='1.10':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
            }
            scriptBuilder.append("ffmpeg -y -loop 1 -t $durationSec -i scene_${i+1}.jpg -vf \"scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,zoompan=$zoomExpr:d=$frames:s=1080x1920:fps=${config.fps},format=yuv420p\" -c:v libx264 -preset ultrafast scene_${i+1}.mp4\n")
        }

        scriptBuilder.append("\n# Step 2: Transition Effects Map (Selected from 100 FX Dataset)\n")
        val sceneTransitions = mutableListOf<VideoTransitionEffect>()
        for (i in 0 until (config.scenes.size - 1)) {
            val sc = config.scenes[i]
            val effect = sc.transitionEffect ?: if (sc.transition == "random" || sc.transition.isBlank()) {
                allEffects[(i * 7 + 3) % allEffects.size]
            } else {
                allEffects.find { it.effectCode.equals(sc.transition, ignoreCase = true) } ?: allEffects.random()
            }
            sceneTransitions.add(effect)
            scriptBuilder.append("# Transition Scene ${i + 1} -> ${i + 2}: [FX-${String.format(Locale.US, "%03d", effect.id)}] ${effect.effectName} (${effect.effectCode} | xfade: ${effect.xfadeName}) - ${effect.visualBehavior}\n")
        }

        scriptBuilder.append("\n# Step 3: Concatenate all segments with ASS Kinetic Subtitles & Audio Track\n")
        val audioParam = if (config.audioPath != null) "-i audio_voiceover.mp3 -c:a aac -shortest" else "-f lavfi -i anullsrc=r=44100:cl=stereo -c:a aac -shortest"
        
        if (config.scenes.size >= 2) {
            // Build xfade filter_complex command
            val filterComplexSb = StringBuilder()
            var currentOffset = 0.0
            
            for (i in 0 until (config.scenes.size - 1)) {
                val sc = config.scenes[i]
                val durationSec = (sc.durationMs / 1000.0).coerceAtLeast(1.0)
                val transEffect = sceneTransitions[i]
                val transDuration = transEffect.recommendedDurationSec.coerceIn(0.3, 1.0)
                currentOffset += (durationSec - transDuration).coerceAtLeast(0.5)
                
                val in1 = if (i == 0) "[0:v]" else "[v$i]"
                val in2 = "[${i + 1}:v]"
                val outLabel = if (i == config.scenes.size - 2) "[vout]" else "[v${i + 1}]"
                filterComplexSb.append("${in1}${in2}xfade=transition=${transEffect.xfadeName}:duration=${String.format(Locale.US, "%.2f", transDuration)}:offset=${String.format(Locale.US, "%.2f", currentOffset)}${outLabel};")
            }
            
            val inputs = config.scenes.indices.joinToString(" ") { "-i scene_${it + 1}.mp4" }
            scriptBuilder.append("ffmpeg -y $inputs $audioParam -filter_complex \"${filterComplexSb.toString().removeSuffix(";")}\" -map \"[vout]\" -map ${config.scenes.size}:a -c:v libx264 -pix_fmt yuv420p -b:v 8M output_viral_short.mp4\n")
        } else {
            scriptBuilder.append("ffmpeg -y -f concat -safe 0 -i segments.txt $audioParam -vf \"subtitles=captions.ass\" -c:v libx264 -pix_fmt yuv420p -b:v 8M output_viral_short.mp4\n")
        }

        return scriptBuilder.toString()
    }

    /**
     * Builds the complete FFmpeg command and standalone bash script for the project.
     * Useful for running via CLI or copying into desktop terminals.
     */
    fun generateFFmpegScript(
        context: Context,
        config: VideoSynthesisConfig,
        outputFile: File
    ): String {
        val scriptBuilder = StringBuilder()
        scriptBuilder.append("#!/bin/bash\n")
        scriptBuilder.append("# ========================================================\n")
        scriptBuilder.append("# GlitchCut Viral 9:16 Shorts - Automated FFmpeg Pipeline\n")
        scriptBuilder.append("# Project: ${config.title}\n")
        scriptBuilder.append("# Total Scenes: ${config.scenes.size} | Resolution: ${config.outputWidth}x${config.outputHeight} @ ${config.fps}fps\n")
        scriptBuilder.append("# ========================================================\n\n")

        val workDir = File(context.cacheDir, "ffmpeg_work_${System.currentTimeMillis()}").apply { mkdirs() }
        val assSubtitleFile = File(workDir, "captions.ass")
        generateAssSubtitles(config, assSubtitleFile)

        scriptBuilder.append("# Step 1: Subtitles ASS file generated at ${assSubtitleFile.name}\n\n")
        scriptBuilder.append("# Step 2: Render individual scene segments with Ken Burns & smart 9:16 aspect ratio\n")

        val segmentFiles = mutableListOf<String>()
        val concatListFile = File(workDir, "segments.txt")
        val concatSb = StringBuilder()

        config.scenes.forEachIndexed { i, scene ->
            val segName = String.format(Locale.US, "scene_%03d.mp4", i + 1)
            val segPath = File(workDir, segName).absolutePath
            segmentFiles.add(segPath)
            concatSb.append("file '$segPath'\n")

            val durationSec = (scene.durationMs / 1000.0).coerceAtLeast(1.0)
            val frames = (durationSec * config.fps).toInt()

            val zoomExpr = when {
                scene.cameraMotion.contains("push in", ignoreCase = true) || scene.cameraMotion.contains("zoom in", ignoreCase = true) ->
                    "z='min(zoom+0.0018,1.20)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                scene.cameraMotion.contains("rapid", ignoreCase = true) || scene.cameraMotion.contains("fast zoom", ignoreCase = true) ->
                    "z='min(zoom+0.0035,1.35)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                scene.cameraMotion.contains("pan down", ignoreCase = true) ->
                    "z='1.15':x='iw/2-(iw/zoom/2)':y='if(lte(on,1),0,min(ih/zoom,y+1.5))'"
                scene.cameraMotion.contains("tilt", ignoreCase = true) || scene.cameraMotion.contains("pan up", ignoreCase = true) ->
                    "z='1.15':x='iw/2-(iw/zoom/2)':y='if(lte(on,1),ih/zoom,max(0,y-1.5))'"
                scene.cameraMotion.contains("tracking", ignoreCase = true) || scene.cameraMotion.contains("pan right", ignoreCase = true) ->
                    "z='1.15':x='if(lte(on,1),0,min(iw/zoom,x+1.5))':y='ih/2-(ih/zoom/2)'"
                scene.cameraMotion.contains("zoom out", ignoreCase = true) || scene.cameraMotion.contains("pull back", ignoreCase = true) ->
                    "z='if(lte(zoom,1.0),1.20,max(1.001,zoom-0.0018))':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                else ->
                    "z='1.02+0.015*sin(2*PI*on/90)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
            }

            val imgPath = scene.imagePathOrUrl ?: "/tmp/placeholder_${i + 1}.jpg"

            scriptBuilder.append("# Render Scene ${i + 1}: ${scene.cameraMotion}\n")
            scriptBuilder.append(
                "${getFFmpegPath()} -y -loop 1 -i \"$imgPath\" -vf " +
                    "\"scale=${config.outputWidth}:${config.outputHeight}:force_original_aspect_ratio=increase,crop=${config.outputWidth}:${config.outputHeight}," +
                    "zoompan=$zoomExpr:d=$frames:s=${config.outputWidth}x${config.outputHeight}:fps=${config.fps}," +
                    "format=yuv420p\" -c:v libx264 -preset ultrafast -vframes $frames \"$segPath\"\n\n"
            )
        }

        concatListFile.writeText(concatSb.toString())

        scriptBuilder.append("# Step 3: Concatenate scenes and mix master audio track with kinetic ASS subtitles\n")
        val audioParam = if (!config.audioPath.isNullOrBlank() && File(config.audioPath).exists()) {
            "-i \"${config.audioPath}\" -c:a aac -b:a 192k -shortest"
        } else {
            "-f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -c:a aac -shortest"
        }

        scriptBuilder.append(
            "${getFFmpegPath()} -y -f concat -safe 0 -i \"${concatListFile.absolutePath}\" $audioParam " +
                "-vf \"ass='${assSubtitleFile.absolutePath}'\" -c:v libx264 -preset fast -crf 20 \"${outputFile.absolutePath}\"\n\n"
        )
        scriptBuilder.append("echo \"✅ 9:16 Shorts Video Created: ${outputFile.absolutePath}\"\n")

        return scriptBuilder.toString()
    }

    /**
     * Generates a valid Advanced SubStation Alpha (.ass) subtitle file using CaptionRegistry styles.
     */
    fun generateAssSubtitles(config: VideoSynthesisConfig, outputFile: File) {
        if (!config.customAssSubtitlePath.isNullOrBlank()) {
            val customFile = File(config.customAssSubtitlePath)
            if (customFile.exists() && customFile.length() > 0) {
                customFile.copyTo(outputFile, overwrite = true)
                return
            }
        }

        val globalStyle = config.globalCaptionStyle ?: CaptionRegistry.getStyles().first()
        val highlightColorAss = CaptionRegistry.hexToAssColor(globalStyle.highlightColorHex)
        val primaryColorAss = CaptionRegistry.hexToAssColor(globalStyle.primaryColorHex)
        val outlineColorAss = CaptionRegistry.hexToAssColor(globalStyle.outlineColorHex)
        val boxBgColorAss = CaptionRegistry.hexToAssColor(globalStyle.boxBackgroundColorHex)

        val sb = StringBuilder()
        sb.append("[Script Info]\n")
        sb.append("Title: Viral 9:16 Kinetic Captions - ${globalStyle.styleName}\n")
        sb.append("ScriptType: v4.00+\n")
        sb.append("PlayResX: ${config.outputWidth}\n")
        sb.append("PlayResY: ${config.outputHeight}\n")
        sb.append("WrapStyle: 0\n\n")

        sb.append("[V4+ Styles]\n")
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n")
        // Style: Custom font, primary color, outline, box background, bottom-center aligned (Alignment=2)
        sb.append("Style: Default,${globalStyle.fontName},${globalStyle.fontSize},$primaryColorAss,$highlightColorAss,$outlineColorAss,$boxBgColorAss,1,0,0,0,100,100,0,0,${globalStyle.borderStyle},4,3,2,60,60,260,1\n\n")

        sb.append("[Events]\n")
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n")

        var currentMs = 0L
        config.scenes.forEach { scene ->
            val startMs = currentMs
            val endMs = currentMs + scene.durationMs
            currentMs = endMs

            val caption = scene.caption.trim()
            if (caption.isNotBlank()) {
                val startStr = formatAssTime(startMs)
                val endStr = formatAssTime(endMs)
                val sceneStyle = scene.captionStyle ?: globalStyle
                val scHighlightAss = CaptionRegistry.hexToAssColor(sceneStyle.highlightColorHex)
                val scPrimaryAss = CaptionRegistry.hexToAssColor(sceneStyle.primaryColorHex)

                val words = caption.split("\\s+".toRegex())
                val styledText = if (words.size > 2) {
                    val highlighted = words.take(2).joinToString(" ")
                    val remaining = words.drop(2).joinToString(" ")
                    "{\\c$scHighlightAss}$highlighted {\\c$scPrimaryAss}$remaining"
                } else {
                    "{\\c$scHighlightAss}$caption"
                }
                sb.append("Dialogue: 0,$startStr,$endStr,Default,,0,0,0,,$styledText\n")
            }
        }

        outputFile.writeText(sb.toString())
    }

    private fun hexToAssColor(hex: String): String {
        // ASS uses &HAABBGGRR in hex
        val clean = hex.removePrefix("#")
        return if (clean.length == 6) {
            val r = clean.substring(0, 2)
            val g = clean.substring(2, 4)
            val b = clean.substring(4, 6)
            "&H00$b$g$r&"
        } else {
            "&H0066FF00&"
        }
    }

    private fun formatAssTime(ms: Long): String {
        val totalSec = ms / 1000
        val centis = (ms % 1000) / 10
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, centis)
    }

    /**
     * Primary entry point to synthesize the final 9:16 short video.
     * Tries FFmpeg process first. If FFmpeg is unavailable, falls back to the native hardware encoder.
     */
    suspend fun synthesizeFinalVideo(
        context: Context,
        config: VideoSynthesisConfig,
        onProgress: (progress: Float, status: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "glitchcut_exports").apply { if (!exists()) mkdirs() }
        val timestamp = System.currentTimeMillis().toString().takeLast(6)
        val finalOutputFile = File(exportDir, "viral_short_${timestamp}.mp4")

        onProgress(0.05f, "🚀 Initializing 9:16 Video Synthesis Engine...")

        // Check if FFmpeg is available on the host/device
        val hasFfmpeg = isFFmpegAvailable()
        Log.i(TAG, "FFmpeg availability check: $hasFfmpeg")

        var generationSuccess = false
        if (hasFfmpeg) {
            try {
                onProgress(0.10f, "🎬 Executing FFmpeg Render with Ken Burns & Transitions...")
                val ffmpegResult = executeFFmpegRender(context, config, finalOutputFile, onProgress)
                if (ffmpegResult && finalOutputFile.exists() && finalOutputFile.length() > 1000) {
                    generationSuccess = true
                    Log.i(TAG, "FFmpeg render succeeded! Size: ${finalOutputFile.length()} bytes")
                }
            } catch (e: Exception) {
                Log.w(TAG, "FFmpeg render failed, falling back to Native Hardware Codec: ${e.message}")
            }
        }

        // Fallback: Native Hardware MediaCodec + Canvas Frame Synthesizer
        if (!generationSuccess) {
            try {
                onProgress(0.15f, "⚡ Using High-Speed Hardware MediaCodec Engine...")
                val nativeSuccess = renderWithNativeHardwareCodec(context, config, finalOutputFile, onProgress)
                if (nativeSuccess && finalOutputFile.exists() && finalOutputFile.length() > 1000) {
                    generationSuccess = true
                    Log.i(TAG, "Native Hardware Codec render succeeded! Size: ${finalOutputFile.length()} bytes")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Native Hardware Codec render error: ${e.message}", e)
                return@withContext Result.failure(Exception("Video synthesis failed: ${e.message}"))
            }
        }

        if (!generationSuccess || !finalOutputFile.exists() || finalOutputFile.length() <= 0) {
            return@withContext Result.failure(Exception("Could not generate valid MP4 video file."))
        }

        // Copy to Public Movies directory and register with MediaStore
        saveToPublicGallery(context, finalOutputFile)

        onProgress(1.0f, "🎉 9:16 Viral Shorts Video Complete! (${finalOutputFile.name})")
        Result.success(finalOutputFile)
    }

    /**
     * Executes the FFmpeg rendering pipeline using segment-by-segment encoding and final concat.
     */
    private suspend fun executeFFmpegRender(
        context: Context,
        config: VideoSynthesisConfig,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "ffmpeg_run_${System.currentTimeMillis()}").apply { mkdirs() }
        val ffmpegBin = getFFmpegPath()

        val segmentFiles = mutableListOf<File>()
        val concatListFile = File(workDir, "concat.txt")
        val concatSb = StringBuilder()

        val totalScenes = config.scenes.size
        for (i in 0 until totalScenes) {
            val scene = config.scenes[i]
            val segFile = File(workDir, String.format(Locale.US, "seg_%03d.mp4", i + 1))
            val durationSec = (scene.durationMs / 1000.0).coerceAtLeast(1.0)
            val frames = (durationSec * config.fps).toInt()

            val progressRatio = 0.10f + (0.60f * (i.toFloat() / totalScenes.toFloat()))
            onProgress(progressRatio, "🎬 Rendering Scene #${i + 1}/$totalScenes (${scene.cameraMotion})...")

            val imgFile = prepareSceneImage(context, scene.imagePathOrUrl, i + 1)

            val zoomExpr = when {
                scene.cameraMotion.contains("push in", ignoreCase = true) || scene.cameraMotion.contains("zoom in", ignoreCase = true) ->
                    "z='min(zoom+0.0018,1.20)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                scene.cameraMotion.contains("rapid", ignoreCase = true) || scene.cameraMotion.contains("fast zoom", ignoreCase = true) ->
                    "z='min(zoom+0.0035,1.35)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                scene.cameraMotion.contains("pan down", ignoreCase = true) ->
                    "z='1.15':x='iw/2-(iw/zoom/2)':y='if(lte(on,1),0,min(ih/zoom,y+1.5))'"
                scene.cameraMotion.contains("tilt", ignoreCase = true) || scene.cameraMotion.contains("pan up", ignoreCase = true) ->
                    "z='1.15':x='iw/2-(iw/zoom/2)':y='if(lte(on,1),ih/zoom,max(0,y-1.5))'"
                scene.cameraMotion.contains("tracking", ignoreCase = true) || scene.cameraMotion.contains("pan right", ignoreCase = true) ->
                    "z='1.15':x='if(lte(on,1),0,min(iw/zoom,x+1.5))':y='ih/2-(ih/zoom/2)'"
                scene.cameraMotion.contains("zoom out", ignoreCase = true) ->
                    "z='if(lte(zoom,1.0),1.20,max(1.001,zoom-0.0018))':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                else ->
                    "z='1.02+0.015*sin(2*PI*on/90)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
            }

            val cmd = mutableListOf(ffmpegBin, "-y")
            if (scene.animationFramePaths.isNotEmpty()) {
                require(scene.animationFramePaths.all { File(it).exists() }) {
                    "Animation frames are missing for scene ${scene.index}."
                }
                cmd.addAll(listOf("-stream_loop", "-1", "-framerate", scene.animationFrameRate.toString()))
                cmd.addAll(listOf("-i", scene.animationFramePaths.first().substringBeforeLast('/') + "/animation_%02d.jpg"))
                cmd.addAll(listOf("-t", String.format(Locale.US, "%.3f", durationSec)))
                cmd.addAll(listOf(
                    "-vf",
                    "scale=${config.outputWidth}:${config.outputHeight}:force_original_aspect_ratio=increase,crop=${config.outputWidth}:${config.outputHeight},fps=${config.fps},format=yuv420p"
                ))
            } else {
                cmd.addAll(listOf("-loop", "1", "-i", imgFile.absolutePath))
                cmd.addAll(listOf(
                    "-vf",
                    "scale=${config.outputWidth}:${config.outputHeight}:force_original_aspect_ratio=increase,crop=${config.outputWidth}:${config.outputHeight},zoompan=$zoomExpr:d=$frames:s=${config.outputWidth}x${config.outputHeight}:fps=${config.fps},format=yuv420p"
                ))
            }
            cmd.addAll(listOf(
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-vframes", frames.toString(),
                "-an",
                segFile.absolutePath
            ))

            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exit = p.waitFor()
            if (exit == 0 && segFile.exists() && segFile.length() > 0) {
                segmentFiles.add(segFile)
                concatSb.append("file '${segFile.absolutePath}'\n")
            } else {
                Log.w(TAG, "FFmpeg failed on scene $i with exit $exit")
                return@withContext false
            }
        }

        concatListFile.writeText(concatSb.toString())

        onProgress(0.75f, "📝 Generating ASS Subtitles & Mixing Voiceover...")
        val assFile = File(workDir, "subtitles.ass")
        generateAssSubtitles(config, assFile)

        val finalCmd = mutableListOf(
            ffmpegBin, "-y",
            "-f", "concat",
            "-safe", "0",
            "-i", concatListFile.absolutePath
        )

        val hasAudio = !config.audioPath.isNullOrBlank() && File(config.audioPath).exists()
        val targetDuration = config.scenes.sumOf { it.durationMs } / 1000.0
        if (hasAudio) {
            finalCmd.add("-i")
            finalCmd.add(config.audioPath!!)
            finalCmd.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
        } else {
            finalCmd.addAll(listOf("-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100", "-c:a", "aac"))
        }

        // Add subtitles filter if libass is available
        val filter = "ass='${assFile.absolutePath}'"
        finalCmd.addAll(
            listOf(
                "-vf", filter,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "20",
                "-t", String.format(Locale.US, "%.3f", targetDuration),
                outputFile.absolutePath
            )
        )

        val finalProcess = ProcessBuilder(finalCmd).redirectErrorStream(true).start()
        val finalExit = finalProcess.waitFor()

        if (finalExit != 0) {
            // Retry without subtitles filter in case libass was missing
            Log.w(TAG, "FFmpeg with ASS subtitles exited with $finalExit, retrying with raw concat...")
            val fallbackCmd = mutableListOf(
                ffmpegBin, "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatListFile.absolutePath
            )
            if (hasAudio) {
                fallbackCmd.add("-i")
                fallbackCmd.add(config.audioPath!!)
                fallbackCmd.addAll(listOf("-c:v", "copy", "-c:a", "aac"))
            } else {
                fallbackCmd.addAll(listOf("-c:v", "copy", "-an"))
            }
            fallbackCmd.addAll(listOf("-t", String.format(Locale.US, "%.3f", targetDuration), outputFile.absolutePath))
            val fbProc = ProcessBuilder(fallbackCmd).redirectErrorStream(true).start()
            val fbExit = fbProc.waitFor()
            return@withContext fbExit == 0
        }

        true
    }

    /**
     * 100% Native Hardware Video Encoder using Android's MediaCodec + MediaMuxer + Canvas.
     * Guaranteed to work on ANY Android device without requiring external binaries or root permissions.
     */
    private suspend fun renderWithNativeHardwareCodec(
        context: Context,
        config: VideoSynthesisConfig,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val width = config.outputWidth
        val height = config.outputHeight
        val fps = config.fps
        val frameDurationUs = (1_000_000L / fps)

        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000) // 6 Mbps high quality
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val tempRawVideoFile = File(outputFile.parentFile ?: context.cacheDir, "temp_raw_video_${System.currentTimeMillis()}.mp4")
        val muxer = MediaMuxer(tempRawVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val totalScenes = config.scenes.size
        var currentPresentationTimeUs = 0L

        // Preload/generate scene bitmaps
        val loadedBitmaps = mutableListOf<Bitmap>()
        config.scenes.forEachIndexed { idx, sc ->
            loadedBitmaps.add(loadSceneBitmap(sc.imagePathOrUrl, idx + 1))
        }

        // Paints for rendering
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 2f, 4f, Color.BLACK)
        }
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.captionHighlightColorHex)
            textSize = 54f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 2f, 4f, Color.BLACK)
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 0, 255, 102)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f, height / 2f,
                (width * 0.9f),
                intArrayOf(Color.TRANSPARENT, Color.argb(60, 0, 0, 0), Color.argb(190, 0, 0, 0)),
                floatArrayOf(0.4f, 0.75f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }

        val allEffects = VideoTransitionRegistry.getEffects(context)
        val sceneTransitionEffects = config.scenes.mapIndexed { idx, sc ->
            sc.transitionEffect ?: if (sc.transition == "random" || sc.transition.isBlank()) {
                allEffects[(idx * 7 + 3) % allEffects.size]
            } else {
                allEffects.find { it.effectCode.equals(sc.transition, ignoreCase = true) } ?: allEffects.random()
            }
        }

        try {
            for (sceneIdx in 0 until totalScenes) {
                val scene = config.scenes[sceneIdx]
                val currentBmp = loadedBitmaps[sceneIdx]
                val animationBitmaps = scene.animationFramePaths.mapNotNull { path ->
                    BitmapFactory.decodeFile(path)
                }
                if (animationBitmaps.size != scene.animationFramePaths.size) {
                    animationBitmaps.forEach { it.recycle() }
                    throw IllegalStateException(
                        "One or more animation frames for scene ${scene.index} could not be decoded."
                    )
                }
                val nextBmp = loadedBitmaps.getOrNull(sceneIdx + 1)
                val transEffect = sceneTransitionEffects.getOrNull(sceneIdx) ?: allEffects.first()
                val sceneFrames = ((scene.durationMs / 1000.0) * fps).toInt().coerceAtLeast(15)

                val progress = 0.15f + (0.65f * (sceneIdx.toFloat() / totalScenes.toFloat()))
                onProgress(progress, "🎬 Processing Scene #${sceneIdx + 1}/$totalScenes (${scene.cameraMotion} | [FX-${String.format(Locale.US, "%03d", transEffect.id)}] ${transEffect.effectName})...")

                try {
                    for (frameIdx in 0 until sceneFrames) {
                        val frameRatio = frameIdx.toFloat() / sceneFrames.toFloat()
                        val animationFrame = if (animationBitmaps.isEmpty()) {
                            currentBmp
                        } else {
                            val sampleFramesPerOutputFrame = (fps / scene.animationFrameRate).coerceAtLeast(1)
                            animationBitmaps[(frameIdx / sampleFramesPerOutputFrame) % animationBitmaps.size]
                        }

                        val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            inputSurface.lockHardwareCanvas()
                        } else {
                            inputSurface.lockCanvas(null)
                        }

                        try {
                            canvas.drawColor(Color.BLACK)

                            val matrix = Matrix()
                            val motion = scene.cameraMotion.lowercase()
                            val scale = when {
                                motion.contains("push in") || motion.contains("zoom in") -> 1.0f + 0.18f * frameRatio
                                motion.contains("rapid") -> 1.0f + 0.35f * frameRatio
                                motion.contains("zoom out") -> 1.20f - 0.18f * frameRatio
                                else -> 1.03f + 0.02f * kotlin.math.sin(frameRatio * Math.PI * 2).toFloat()
                            }
                            val transX = when {
                                motion.contains("tracking") || motion.contains("pan right") -> (width * 0.08f) * (frameRatio - 0.5f)
                                else -> 0f
                            }
                            val transY = when {
                                motion.contains("pan down") -> (height * 0.06f) * (frameRatio - 0.5f)
                                motion.contains("tilt") || motion.contains("pan up") -> (height * 0.06f) * (0.5f - frameRatio)
                                else -> 0f
                            }
                            val bmpScale = maxOf(
                                width.toFloat() / animationFrame.width.toFloat(),
                                height.toFloat() / animationFrame.height.toFloat()
                            ) * scale
                            val dx = (width - animationFrame.width * bmpScale) / 2f + transX
                            val dy = (height - animationFrame.height * bmpScale) / 2f + transY
                            matrix.postScale(bmpScale, bmpScale)
                            matrix.postTranslate(dx, dy)
                            canvas.drawBitmap(animationFrame, matrix, null)

                            val transitionDurationSec = transEffect.recommendedDurationSec.coerceIn(0.3, 1.0)
                            val transitionFrames = (transitionDurationSec * fps).toInt().coerceAtLeast(6)
                            if (nextBmp != null && frameIdx >= (sceneFrames - transitionFrames)) {
                                val tRatio = (frameIdx - (sceneFrames - transitionFrames)).toFloat() / transitionFrames.toFloat()
                                VideoTransitionRegistry.renderCanvasTransition(
                                    canvas = canvas,
                                    currentBmp = animationFrame,
                                    nextBmp = nextBmp,
                                    transitionRatio = tRatio,
                                    width = width,
                                    height = height,
                                    effectCode = transEffect.effectCode
                                )
                            }

                            if (config.enableVignette) {
                                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
                            }

                            if (config.enableKineticCaptions && scene.caption.isNotBlank()) {
                                val activeStyle = scene.captionStyle
                                    ?: config.globalCaptionStyle
                                    ?: CaptionRegistry.getStyles().first()
                                CaptionRegistry.renderCanvasKineticCaption(
                                    canvas,
                                    scene.caption,
                                    width,
                                    height,
                                    activeStyle,
                                    progressInScene = frameRatio
                                )
                            }

                        } finally {
                            inputSurface.unlockCanvasAndPost(canvas)
                        }

                        currentPresentationTimeUs += frameDurationUs
                        drainEncoder(encoder, muxer, bufferInfo, isEos = false) { isStarted, trackIdx ->
                            muxerStarted = isStarted
                            videoTrackIndex = trackIdx
                        }
                    }
                } finally {
                    animationBitmaps.forEach { it.recycle() }
                }
            }

            // Signal End of Stream
            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, isEos = true) { isStarted, trackIdx ->
                muxerStarted = isStarted
                videoTrackIndex = trackIdx
            }

        } finally {
            try {
                encoder.stop()
                encoder.release()
                inputSurface.release()
                if (muxerStarted) {
                    muxer.stop()
                    muxer.release()
                }
                loadedBitmaps.forEach { it.recycle() }
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up hardware encoder: ${e.message}")
            }
        }

        // Merge Raw Encoded Video with Master Voiceover Audio Track
        onProgress(0.92f, "🎵 Synchronizing Voiceover Audio Track & Finalizing MP4...")
        mergeVideoAndAudio(context, tempRawVideoFile, config.audioPath, outputFile)
        try { tempRawVideoFile.delete() } catch (_: Exception) {}

        true
    }

    private fun ensureAacAudio(context: Context, audioPath: String): String {
        val file = File(audioPath)
        if (!file.exists() || file.length() == 0L) return audioPath

        var isAac = false
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(audioPath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (mime == MediaFormat.MIMETYPE_AUDIO_AAC || mime == "audio/mp4a-latm") {
                        isAac = true
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio format probe error: ${e.message}")
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }

        if (isAac) return audioPath

        val aacOutputFile = File(context.cacheDir, "transcoded_aac_${System.currentTimeMillis()}.m4a")
        val success = transcodeAudioToAac(audioPath, aacOutputFile)
        return if (success && aacOutputFile.exists() && aacOutputFile.length() > 1000L) {
            aacOutputFile.absolutePath
        } else {
            audioPath
        }
    }

    private fun transcodeAudioToAac(inputPath: String, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val sampleRate = 44100
            val channelCount = 1

            val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            if (inputPath.endsWith(".wav", ignoreCase = true)) {
                val fileInputStream = java.io.FileInputStream(inputPath)
                if (inputPath.endsWith(".wav", ignoreCase = true)) {
                    fileInputStream.skip(44)
                }

                val pcmBuffer = ByteArray(4096)
                var presentationTimeUs = 0L
                var isEos = false

                while (!isEos) {
                    val inIdx = encoder.dequeueInputBuffer(10000L)
                    if (inIdx >= 0) {
                        val bytesRead = fileInputStream.read(pcmBuffer)
                        if (bytesRead > 0) {
                            val inputBuf = encoder.getInputBuffer(inIdx)
                            inputBuf?.clear()
                            inputBuf?.put(pcmBuffer, 0, bytesRead)
                            encoder.queueInputBuffer(inIdx, 0, bytesRead, presentationTimeUs, 0)
                            presentationTimeUs += (bytesRead.toLong() * 1_000_000L) / (sampleRate * channelCount * 2)
                        } else {
                            encoder.queueInputBuffer(inIdx, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        }
                    }

                    var outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                    while (outIdx >= 0) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            if (!muxerStarted) {
                                audioTrackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        } else if (bufferInfo.size > 0 && muxerStarted) {
                            val encodedData = encoder.getOutputBuffer(outIdx)
                            if (encodedData != null) {
                                muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        outIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }
                fileInputStream.close()

                var drainIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                while (drainIdx >= 0) {
                    if (muxerStarted && bufferInfo.size > 0) {
                        val encodedData = encoder.getOutputBuffer(drainIdx)
                        if (encodedData != null) {
                            muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(drainIdx, false)
                    drainIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                }
            } else {
                extractor = MediaExtractor().apply { setDataSource(inputPath) }
                var trackIndex = -1
                var inputFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        trackIndex = i
                        inputFormat = format
                        break
                    }
                }

                if (trackIndex < 0 || inputFormat == null) return false
                extractor.selectTrack(trackIndex)

                val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
                decoder = MediaCodec.createDecoderByType(inputMime)
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()

                var isDecoderEos = false
                var isEncoderEos = false
                val decInfo = MediaCodec.BufferInfo()

                while (!isEncoderEos) {
                    if (!isDecoderEos) {
                        val inIdx = decoder.dequeueInputBuffer(10000L)
                        if (inIdx >= 0) {
                            val sampleSize = extractor.readSampleData(decoder.getInputBuffer(inIdx)!!, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isDecoderEos = true
                            } else {
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val decOutIdx = decoder.dequeueOutputBuffer(decInfo, 10000L)
                    if (decOutIdx >= 0) {
                        val pcmBuf = decoder.getOutputBuffer(decOutIdx)
                        if (pcmBuf != null && decInfo.size > 0) {
                            val encInIdx = encoder.dequeueInputBuffer(10000L)
                            if (encInIdx >= 0) {
                                val encBuf = encoder.getInputBuffer(encInIdx)
                                encBuf?.clear()
                                encBuf?.put(pcmBuf)
                                val flags = if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                encoder.queueInputBuffer(encInIdx, 0, decInfo.size, decInfo.presentationTimeUs, flags)
                            }
                        }
                        decoder.releaseOutputBuffer(decOutIdx, false)
                        if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isEncoderEos = true
                        }
                    }

                    var encOutIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    while (encOutIdx >= 0) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            if (!muxerStarted) {
                                audioTrackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        } else if (bufferInfo.size > 0 && muxerStarted) {
                            val encodedData = encoder.getOutputBuffer(encOutIdx)
                            if (encodedData != null) {
                                muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(encOutIdx, false)
                        encOutIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }
            }

            if (muxerStarted) {
                muxer.stop()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error transcoding audio to AAC: ${e.message}", e)
            return false
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun mergeVideoAndAudio(context: Context, videoFile: File, rawAudioPath: String?, outputFile: File) {
        if (rawAudioPath.isNullOrBlank() || !File(rawAudioPath).exists() || File(rawAudioPath).length() == 0L) {
            videoFile.copyTo(outputFile, overwrite = true)
            return
        }

        val audioPath = ensureAacAudio(context, rawAudioPath)

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }

            var videoTrackInVideo = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackInVideo = i
                    videoFormat = format
                    break
                }
            }

            var audioTrackInAudio = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackInAudio = i
                    audioFormat = format
                    break
                }
            }

            if (videoTrackInVideo < 0 || videoFormat == null) {
                videoFile.copyTo(outputFile, overwrite = true)
                return
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = if (audioTrackInAudio >= 0 && audioFormat != null) {
                muxer.addTrack(audioFormat)
            } else -1

            muxer.start()

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Copy Video Track Samples
            videoExtractor.selectTrack(videoTrackInVideo)
            while (true) {
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags
                bufferInfo.offset = 0
                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Copy Audio Track Samples
            if (muxerAudioTrack >= 0 && audioTrackInAudio >= 0) {
                audioExtractor.selectTrack(audioTrackInAudio)
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    bufferInfo.flags = audioExtractor.sampleFlags
                    bufferInfo.offset = 0
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error merging video and audio tracks: ${e.message}", e)
            videoFile.copyTo(outputFile, overwrite = true)
        } finally {
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        isEos: Boolean,
        onMuxerReady: (Boolean, Int) -> Unit
    ) {
        var muxerStarted = false
        var videoTrackIdx = -1

        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!isEos) break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                videoTrackIdx = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
                onMuxerReady(true, videoTrackIdx)
            } else if (encoderStatus >= 0) {
                val encodedData = encoder.getOutputBuffer(encoderStatus) ?: continue
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(videoTrackIdx.coerceAtLeast(0), encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(encoderStatus, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun muxAudioTrack(audioPath: String, muxer: MediaMuxer) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(audioPath)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            if (audioTrackIndex >= 0) {
                val maxBufferSize = 512 * 1024
                val buffer = ByteBuffer.allocate(maxBufferSize)
                val info = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    info.size = sampleSize
                    info.presentationTimeUs = extractor.sampleTime
                    info.flags = extractor.sampleFlags
                    info.offset = 0

                    muxer.writeSampleData(audioTrackIndex, buffer, info)
                    extractor.advance()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio muxing warning: ${e.message}")
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
        }
    }

    private fun drawKineticCaptions(
        canvas: Canvas,
        caption: String,
        width: Int,
        height: Int,
        textPaint: Paint,
        highlightPaint: Paint,
        pillPaint: Paint,
        pillStrokePaint: Paint
    ) {
        val words = caption.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return

        // Take up to 7 words per card chunk to ensure large viral typography
        val displayWords = words.take(7)
        val textStr = displayWords.joinToString(" ")

        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(textStr, 0, textStr.length, textBounds)

        val pillWidth = (textBounds.width() + 80f).coerceAtMost(width - 60f)
        val pillHeight = (textBounds.height() + 50f)
        val pillLeft = (width - pillWidth) / 2f
        val pillTop = (height * 0.74f) - (pillHeight / 2f)

        val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
        canvas.drawRoundRect(pillRect, 20f, 20f, pillPaint)
        canvas.drawRoundRect(pillRect, 20f, 20f, pillStrokePaint)

        // Draw caption text centered
        val textY = pillTop + (pillHeight / 2f) + (textBounds.height() / 2f) - 4f
        val textX = width / 2f
        canvas.drawText(textStr, textX, textY, textPaint)
    }

    private fun downloadOrLoadBitmap(pathOrUrl: String?): Bitmap? {
        if (pathOrUrl.isNullOrBlank()) return null
        return try {
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                val url = URL(pathOrUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 15000
                connection.doInput = true
                connection.connect()
                if (connection.responseCode in 200..299) {
                    connection.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else null
            } else if (File(pathOrUrl).exists() && File(pathOrUrl).length() > 0) {
                BitmapFactory.decodeFile(pathOrUrl)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load bitmap from $pathOrUrl: ${e.message}")
            null
        }
    }

    private fun prepareSceneImage(context: Context, pathOrUrl: String?, sceneIndex: Int): File {
        val cacheDir = File(context.cacheDir, "scene_images").apply { if (!exists()) mkdirs() }
        val targetFile = File(cacheDir, "scene_img_${sceneIndex}.jpg")
        val bitmap = loadSceneBitmap(pathOrUrl, sceneIndex)
        try {
            val saved = FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            check(saved) { "Unable to save scene $sceneIndex image for rendering." }
        } finally {
            bitmap.recycle()
        }
        return targetFile
    }

    private fun loadSceneBitmap(pathOrUrl: String?, sceneIndex: Int): Bitmap =
        checkNotNull(downloadOrLoadBitmap(pathOrUrl)) {
            "Scene $sceneIndex image is missing or could not be decoded; generate or select a real image before rendering."
    }

    private fun saveToPublicGallery(context: Context, videoFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/GlitchCut_Viral_Shorts")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        videoFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetDir = File(moviesDir, "GlitchCut_Viral_Shorts").apply { if (!exists()) mkdirs() }
                val targetFile = File(targetDir, videoFile.name)
                videoFile.copyTo(targetFile, overwrite = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice: MediaStore insertion error: ${e.message}")
        }
    }
}
