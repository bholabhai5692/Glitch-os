package com.example.pipeline.dom

/**
 * Advanced Notrack-Powered Story & Animation Prompt Engine.
 * Formulates high-retention multi-stage Chain-of-Thought prompts specifically tuned
 * for the Notrack scraper (`notrack.js`) to generate hyper-detailed 10-scene emotional stories
 * with production-grade HTML5 Canvas, CSS3 keyframe animations, and physics-based JavaScript loops.
 */
object AdvancedStoryPromptEngine {

    fun buildAdvancedNotrackPrompt(topic: String, sceneNumber: Int, totalScenes: Int = 10): String {
        return """
            You are an elite short-form video content and animation specialist.
            Generate a hyper-detailed, emotionally captivating animation script and HTML5/CSS3/JS code for Scene #$sceneNumber of $totalScenes.
            Topic/Theme: "$topic"
            
            Requirements:
            - Return ONLY valid JSON containing:
              {
                "scene_number": $sceneNumber,
                "title": "Cinematic Scene Title with Emojis",
                "duration_seconds": 15,
                "narration_hindi": "Deep emotional Hindi narration for scene $sceneNumber",
                "narration_english": "English translation voiceover",
                "visual_prompt": "Cinematic 9:16 vertical lighting, color grading, particle effects",
                "camera_motion": "ZOOM_IN_SLOW",
                "weather_atmosphere": "RAIN_NEON",
                "audio_sfx": "CINEMATIC_WHOOSH",
                "html_canvas_code": "<!DOCTYPE html><html><head><style>/* Hyper-detailed CSS3 keyframes, particle shaders, glowing ambient light */</style></head><body><canvas id='c'></canvas><script>/* 60FPS Animation Loop with physics */</script></body></html>"
              }
            - Strict closed JSON format. No markdown wrappers.
        """.trimIndent()
    }

    fun buildAdvancedStoryOverviewPrompt(topic: String): String {
        return """
            Generate an advanced 10-scene cinematic breakdown for: "$topic".
            Return strict closed JSON with 10 detailed scenes, titles, narrations, visual prompts, and camera motions. No markdown.
        """.trimIndent()
    }
}
