package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateJobRequest(
    @Json(name = "topic") val topic: String,
    @Json(name = "language") val language: String = "English",
    @Json(name = "category") val category: String = "Technology",
    @Json(name = "durationSec") val durationSec: Int = 60,
    @Json(name = "style") val style: String = "Cinematic",
    @Json(name = "aspectRatio") val aspectRatio: String = "9:16",
    @Json(name = "platform") val platform: String = "Vertical short video",
    @Json(name = "audience") val audience: String = "General",
    @Json(name = "instructions") val instructions: String = "",
    @Json(name = "voiceName") val voiceName: String = "hi-IN-MadhurNeural",
    @Json(name = "imageDurationSec") val imageDurationSec: Int = 3,
    @Json(name = "showCaptions") val showCaptions: Boolean = true,
    @Json(name = "captionStyle") val captionStyle: String = "hormozi_bold",
    @Json(name = "captionHighlightColor") val captionHighlightColor: String = "#FFE600",
    @Json(name = "captionFontFamily") val captionFontFamily: String = "THE_BOLD_FONT",
    @Json(name = "captionStrokeWidth") val captionStrokeWidth: Float = 12f,
    @Json(name = "captionGlow") val captionGlow: Boolean = true,
    @Json(name = "captionWordsPerChunk") val captionWordsPerChunk: Int = 3,
    @Json(name = "captionUppercase") val captionUppercase: Boolean = true,
    @Json(name = "bgmEnabled") val bgmEnabled: Boolean = true,
    @Json(name = "bgmTrackId") val bgmTrackId: String = "dark_suspense_pulse",
    @Json(name = "bgmVolume") val bgmVolume: Float = 0.18f,
    @Json(name = "autoDucking") val autoDucking: Boolean = true,
    @Json(name = "customNarration") val customNarration: String? = null,
    @Json(name = "customHook") val customHook: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateJobResponse(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String,
    @Json(name = "step") val step: String? = null
)

@JsonClass(generateAdapter = true)
data class JobStatusResponse(
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String,
    @Json(name = "progress") val progress: Int = 0,
    @Json(name = "step") val step: String? = null,
    @Json(name = "currentImage") val currentImage: Int = 0,
    @Json(name = "totalImages") val totalImages: Int = 0,
    @Json(name = "error") val error: JobError? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "output") val output: JobOutput? = null
)

@JsonClass(generateAdapter = true)
data class JobError(
    @Json(name = "message") val message: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class JobOutput(
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "visualCount") val visualCount: Int? = null,
    @Json(name = "audioDurationSec") val audioDurationSec: Double? = null
)

@JsonClass(generateAdapter = true)
data class ProjectSummary(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "topic") val topic: String = "",
    @Json(name = "language") val language: String = "English",
    @Json(name = "category") val category: String = "Technology",
    @Json(name = "duration") val duration: Int = 0,
    @Json(name = "style") val style: String = "Cinematic",
    @Json(name = "aspectRatio") val aspectRatio: String = "9:16",
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "status") val status: String = "completed",
    @Json(name = "video") val video: String = "",
    @Json(name = "thumbnail") val thumbnail: String = ""
)

@JsonClass(generateAdapter = true)
data class ProjectListResponse(
    @Json(name = "projects") val projects: List<ProjectSummary> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VisualPromptItem(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "startSec") val startSec: Double = 0.0,
    @Json(name = "endSec") val endSec: Double = 0.0,
    @Json(name = "purpose") val purpose: String = "Scene Visual",
    @Json(name = "prompt") val prompt: String = ""
)

@JsonClass(generateAdapter = true)
data class ImageItem(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "fileName") val fileName: String = "",
    @Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class ProjectProductionSettings(
    @Json(name = "category") val category: String = "",
    @Json(name = "language") val language: String = "",
    @Json(name = "voice") val voice: String = "",
    @Json(name = "aspectRatio") val aspectRatio: String = "",
    @Json(name = "captions") val captions: Boolean = false,
    @Json(name = "durationSeconds") val durationSeconds: Int = 0
)

@JsonClass(generateAdapter = true)
data class StoryContent(
    @Json(name = "title") val title: String? = null,
    @Json(name = "topic") val topic: String? = null,
    @Json(name = "topicAngle") val topicAngle: String? = null,
    @Json(name = "summary100") val summary100: String? = null,
    @Json(name = "description500") val description500: String? = null,
    @Json(name = "story") val story: String? = null,
    @Json(name = "narration") val narration: String? = null,
    @Json(name = "selectedTitle") val selectedTitle: String? = null,
    @Json(name = "productionSettings") val productionSettings: ProjectProductionSettings? = null
)

@JsonClass(generateAdapter = true)
data class ProjectDetailsResponse(
    @Json(name = "project") val project: ProjectSummary? = null,
    @Json(name = "story") val story: StoryContent? = null,
    @Json(name = "visualPrompts") val visualPrompts: List<VisualPromptItem> = emptyList(),
    @Json(name = "images") val images: List<ImageItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "service") val service: String = "",
    @Json(name = "version") val version: String = "",
    @Json(name = "headless") val headless: Boolean = true,
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class LogsResponse(
    @Json(name = "logs") val logs: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SimpleSuccessResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null
)
