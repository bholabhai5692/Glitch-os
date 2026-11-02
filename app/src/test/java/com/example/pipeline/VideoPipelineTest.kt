package com.example.pipeline

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoPipelineTest {

    @Test
    fun metadataProviderPromptDisallowsUnsupportedClaims() {
        val prompt = NotrackWebViewClient.buildMetadataPrompt(
            VideoRequest(topic = "Urban gardening", category = "Education")
        )

        org.junit.Assert.assertTrue(prompt.contains("Do not invent sources, events, statistics"))
        org.junit.Assert.assertTrue(prompt.contains("reviewed against reliable sources"))
    }

    @Test
    fun metadataPromptRejectsPlaceholderTopics() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            NotrackWebViewClient.buildMetadataPrompt(
                VideoRequest(topic = "Auto: Education", category = "Education")
            )
        }
    }

    @Test
    fun metadataParserRejectsMissingProviderFieldsInsteadOfSubstitutingTemplates() {
        val client = NotrackWebViewClient(null)
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            client.parseMetadata(
                """{"title":"","summary":"","description":"","story":"","narration":""}""",
                VideoRequest(topic = "Urban gardening", category = "Education")
            )
        }

        org.junit.Assert.assertTrue(error.message.orEmpty().contains("title"))
    }

    @Test
    fun metadataParserPreservesValidatedProviderOutput() {
        val narration = "This draft discusses urban gardening in small spaces. It outlines possible container choices, light conditions, watering routines, and ways to observe plant growth. Check local climate and reliable horticultural sources before applying any recommendations or publishing factual details."
        val client = NotrackWebViewClient(null)
        val result = client.parseMetadata(
            """
            {
              "title":"Urban gardening in small spaces",
              "summary":"An introduction to planning a small urban garden.",
              "description":"A draft overview of urban gardening that should be checked against the finished video.",
              "story":"The video introduces space planning, containers, and plant care.",
              "narration":"$narration"
            }
            """.trimIndent(),
            VideoRequest(topic = "Urban gardening", category = "Education", language = "English")
        )

        org.junit.Assert.assertEquals(narration, result.narration)
        org.junit.Assert.assertEquals("Urban gardening in small spaces", result.title)
        org.junit.Assert.assertEquals(
            "A draft overview of urban gardening that should be checked against the finished video.",
            result.description
        )
    }

    @Test
    fun customNarrationMetadataDoesNotAddPerformanceClaims() = runTest {
        val narration = "User supplied narration text."
        val result = NotrackWebViewClient(null).generateMetadata(
            VideoRequest(
                topic = "Urban gardening",
                category = "Education",
                language = "English",
                customNarration = narration,
                customHook = "Urban gardening: user draft"
            )
        )

        org.junit.Assert.assertEquals(narration, result.description)
        org.junit.Assert.assertEquals("Urban gardening: user draft", result.title)
        org.junit.Assert.assertEquals(narration.take(150), result.summary)
        org.junit.Assert.assertFalse(result.description.contains("#Shorts"))
        org.junit.Assert.assertFalse(result.description.contains("#Viral"))
    }

    @Test
    fun visualPromptParserRejectsMissingProviderScenes() {
        val client = NotrackWebViewClient(null)

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            client.parseVisualPrompts(
                response = """[{"purpose":"Opening","prompt":"A city garden on a sunny rooftop"}]""",
                visualCount = 2,
                audioDuration = 8.0,
                imageDurationSec = 4
            )
        }
    }

    @Test
    fun visualPromptParserRejectsPlaceholderScenes() {
        val client = NotrackWebViewClient(null)

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            client.parseVisualPrompts(
                response = """[{"purpose":"Opening","prompt":"Describing the first scene based on the narration"}]""",
                visualCount = 1,
                audioDuration = 8.0,
                imageDurationSec = 4
            )
        }
    }

    @Test
    fun visualPromptParserUsesRequestedSceneDurationAndAudioLength() {
        val client = NotrackWebViewClient(null)
        val prompts = client.parseVisualPrompts(
            response = """[
                {"purpose":"Opening","prompt":"A rooftop garden at sunrise with people tending plants"},
                {"purpose":"Harvest","prompt":"A close view of fresh vegetables gathered from planters"}
            ]""",
            visualCount = 2,
            audioDuration = 7.5,
            imageDurationSec = 4
        )

        org.junit.Assert.assertEquals(listOf(0.0, 4.0), prompts.map { it.startSec })
        org.junit.Assert.assertEquals(listOf(4.0, 7.5), prompts.map { it.endSec })
    }

    @Test
    fun testJsonPayloadExtraction() {
        val sampleResponse = """
            Here is the generated viral storyboard:
            ```json
            {
              "title": "5 Mind-Blowing AI Breakthroughs",
              "description": "Discover incredible new AI innovations!",
              "tags": ["AI", "Technology", "Future"],
              "hashtags": ["#AI", "#Tech", "#Shorts"],
              "visualCount": 3,
              "scenes": [
                {
                  "sceneNumber": 1,
                  "narration": "Artificial intelligence is accelerating faster than ever before.",
                  "visualPrompt": "Futuristic glowing cybernetic neural network in dark void, 8k cinematic",
                  "durationSec": 4
                },
                {
                  "sceneNumber": 2,
                  "narration": "Robots are now learning dexterity directly from human simulations.",
                  "visualPrompt": "Sleek metallic humanoid robot working in ultra-modern laboratory",
                  "durationSec": 4
                },
                {
                  "sceneNumber": 3,
                  "narration": "Subscribe for daily groundbreaking tech insights!",
                  "visualPrompt": "Glowing holographic subscribe button hovering in digital cyberspace",
                  "durationSec": 3
                }
              ]
            }
            ```
            Hope this helps!
        """.trimIndent()

        val jsonStartIndex = sampleResponse.indexOf("{")
        val jsonEndIndex = sampleResponse.lastIndexOf("}")
        val jsonString = sampleResponse.substring(jsonStartIndex, jsonEndIndex + 1)

        val json = JSONObject(jsonString)
        assertEquals("5 Mind-Blowing AI Breakthroughs", json.getString("title"))
        assertEquals(3, json.getInt("visualCount"))
        val scenes = json.getJSONArray("scenes")
        assertEquals(3, scenes.length())

        val scene1 = scenes.getJSONObject(0)
        assertEquals(1, scene1.getInt("sceneNumber"))
        assertNotNull(scene1.getString("visualPrompt"))
    }
}
