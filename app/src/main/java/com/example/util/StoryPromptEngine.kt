package com.example.util

import org.json.JSONObject

object StoryPromptEngine {

    fun build10SceneVideoMasterPrompt(
        topic: String,
        category: String,
        language: String,
        durationSeconds: Int,
        aspectRatio: String
    ): String {
        require(topic.isNotBlank()) { "Enter a topic before generating a video." }
        require(durationSeconds in 15..300) { "Video duration must be between 15 and 300 seconds." }
        require(language.isNotBlank()) { "Choose a narration language before generating a video." }
        require(category.isNotBlank()) { "Choose a video category before generating a video." }

        val sceneDurations = List(10) { durationSeconds / 10 + if (it < durationSeconds % 10) 1 else 0 }
        return """
You are the script, storyboard, and production-planning engine for a real video render.
Create an original, coherent, exactly 10-scene video package from the user's topic.

USER CONFIGURATION
- Topic: ${JSONObject.quote(topic.trim())}
- Video category: ${JSONObject.quote(category.trim())}
- Narration language: ${JSONObject.quote(language.trim())}
- Total target duration: $durationSeconds seconds
- Aspect ratio: $aspectRatio
- Scene durations in order: ${sceneDurations.joinToString(", ")} seconds (their sum must be exactly $durationSeconds)

CONTENT AND ACCURACY RULES
- Write all narration in $language. Do not provide an alternate-language narration.
- Keep the story and visuals relevant to the supplied topic and selected category.
- Do not invent real-world sources, statistics, quotes, events, or factual claims. Clearly frame fiction as fiction.
- Do not include engagement, virality, CTR, or performance guarantees.
- Every scene must move the story forward and have distinct visual direction.
- Do not output HTML, CSS, JavaScript, markdown, commentary, or placeholder text in this stage.

Return only one valid JSON object with exactly these top-level fields:
{
  "title": "Accurate video title, at most 100 characters",
  "english_title": "Concise English rendering of the title",
  "theme": "Short creative theme",
  "moral": "Short closing thought, or empty string if not applicable",
  "total_scenes": 10,
  "total_duration_seconds": $durationSeconds,
  "scenes": [
    {
      "scene_number": 1,
      "scene_title": "Scene title in $language",
      "duration_seconds": ${sceneDurations[0]},
      "narration": "Spoken narration in $language",
      "visual_description": "Specific visual composition, environment, subject, lighting, and mood",
      "camera_motion": "A concrete camera movement",
      "weather_atmosphere": "Lighting or atmosphere, or NONE",
      "audio_sfx": "Relevant sound effect cue, or NONE"
    }
  ]
}
Include all ten numbered scene objects. Set each duration to the corresponding value in the configured list above, so the total equals $durationSeconds.
""".trimIndent()
    }

    fun build10SceneEmotionalPrompt(customTopic: String): String {
        require(customTopic.isNotBlank()) { "Enter a topic before requesting a story." }
        val topicPart = "TOPIC / CORE THEME: ${JSONObject.quote(customTopic.trim())}"

        return """
You are an award-winning cinematic screenwriter and emotional storytelling director.
Generate a deeply emotional, tear-jerking 10-scene short story (similar in emotional depth to "आखिरी चिट्ठी — The Last Letter").

$topicPart

Return ONLY a strictly valid, balanced, closed JSON object. Do NOT wrap with markdown backticks (no ```json). Do NOT add conversational commentary.

Exact JSON structure required:
{
  "title": "Heart-touching Hindi Title (e.g., आखिरी चिट्ठी)",
  "english_title": "English Title",
  "theme": "Sacrifice, Regret, and Unconditional Love",
  "total_scenes": 10,
  "total_duration_seconds": 150,
  "moral": "Deep emotional moral lesson in 1-2 lines",
  "scenes": [
    {
      "scene_number": 1,
      "scene_title": "Descriptive Scene Name",
      "narration_hindi": "हिंदी में गहरा और भावुक संवाद या नैरेशन...",
      "narration_english": "English voiceover narration line...",
      "visual_description": "Cinematic visual silhouette lighting, room, shadow, or weather description...",
      "camera_motion": "ZOOM_IN / SLOW_PAN_LEFT / STATIC / SHAKE / FADE_OUT",
      "weather_atmosphere": "RAIN / LIGHTNING / SEPIA / NIGHT / WARM_SUN / NONE",
      "audio_sfx": "RAIN_AMBIENCE / THUNDER / PHONE_RING / CLOCK_TICK / TEAR_DROP",
      "duration_seconds": 12
    },
    {
      "scene_number": 2,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 14
    },
    {
      "scene_number": 3,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 15
    },
    {
      "scene_number": 4,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 14
    },
    {
      "scene_number": 5,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 16
    },
    {
      "scene_number": 6,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 15
    },
    {
      "scene_number": 7,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 18
    },
    {
      "scene_number": 8,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 16
    },
    {
      "scene_number": 9,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 15
    },
    {
      "scene_number": 10,
      "scene_title": "...",
      "narration_hindi": "...",
      "narration_english": "...",
      "visual_description": "...",
      "camera_motion": "...",
      "weather_atmosphere": "...",
      "audio_sfx": "...",
      "duration_seconds": 15
    }
  ]
}
""".trimIndent()
    }
}
