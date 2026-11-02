package com.example.pipeline

import java.io.File

data class VideoRequest(
    val topic: String,
    val language: String = "Hindi",
    val category: String = "General",
    val targetDurationSec: Int = 60,
    val style: String = "cinematic",
    val aspect: String = "9:16",
    val platform: String = "Vertical short video",
    val audience: String = "General",
    val voiceName: String = "hi-IN-MadhurNeural",
    val imageDurationSec: Int = 3,
    val showCaptions: Boolean = true,
    val captionStyle: String = "hormozi_bold",
    val captionHighlightColor: String = "#FFE600",
    val captionFontFamily: String = "THE_BOLD_FONT",
    val captionStrokeWidth: Float = 12f,
    val captionGlow: Boolean = true,
    val captionWordsPerChunk: Int = 3,
    val captionUppercase: Boolean = true,
    val bgmEnabled: Boolean = true,
    val bgmTrackId: String = "dark_suspense_pulse",
    val bgmVolume: Float = 0.18f,
    val autoDucking: Boolean = true,
    val visualRenderMode: String = "AI_IMAGES",
    val htmlTheme: String = "Cyberpunk Dark",
    val customNarration: String? = null,
    val customHook: String? = null
)

data class VideoMetadata(
    val title: String,
    val summary: String,
    val description: String,
    val story: String,
    val narration: String
)

data class VisualPrompt(
    val index: Int,
    val startSec: Double,
    val endSec: Double,
    val purpose: String,
    val prompt: String
)

data class PipelineResult(
    val projectId: String,
    val video: File,
    val thumbnail: File,
    val metadata: VideoMetadata,
    val audioDuration: Double,
    val visualCount: Int,
    val visualPrompts: List<VisualPrompt>,
    val images: List<File>,
    val captions: File
)
