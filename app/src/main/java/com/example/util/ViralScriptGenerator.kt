package com.example.util

import com.example.pipeline.NotrackWebViewClient.Companion.VideoNiche

data class SceneBreakdownItem(
    val sceneNumber: Int,
    val timestampRange: String,
    val visualCue: String,
    val voiceoverText: String,
    val sfxCue: String,
    val retentionAngle: String
)

data class ViralScriptBreakdown(
    val topic: String,
    val category: String,
    val language: String,
    val viralHook: String,
    val totalEstimatedDurationSec: Int,
    val fullNarrationText: String,
    val sceneBreakdown: List<SceneBreakdownItem>,
    val endScreenCallToAction: String,
    val visualPrompt8k: String
)

object ViralScriptGenerator {

    /**
     * Generates a short-form video script, hook, and scene-by-scene breakdown.
     * Fully autonomous, zero manual click needed.
     */
    fun generateViralScript(
        topic: String,
        category: String,
        language: String = "Hindi",
        targetDurationSec: Int = 60
    ): ViralScriptBreakdown {
        val cleanTopic = topic.trim()
        require(cleanTopic.isNotBlank()) {
            "A real topic must be supplied before generating a script."
        }

        val isHindi = language.contains("Hindi", ignoreCase = true)
        val niche = com.example.pipeline.NotrackWebViewClient.resolveNiche(category)

        val hook = when (niche) {
            VideoNiche.TECHNOLOGY, VideoNiche.AI_FUTURISTIC -> {
                if (isHindi) "रुकिए! $cleanTopic ने टेक इंडस्ट्री में हलचल मचा दी है! क्या यह हमारी दुनिया को हमेशा के लिए बदलने वाला है?"
                else "STOP SCROLLING! $cleanTopic is revolutionizing modern technology faster than anyone expected!"
            }
            VideoNiche.BUSINESS_FINANCE -> {
                if (isHindi) "रुकिए! क्या आप जानते हैं कि $cleanTopic के पीछे का फाइनेंशियल सीक्रेट क्या है? बड़े इन्वेस्टर्स इसे छुपा रहे हैं!"
                else "STOP SCROLLING! The money and power dynamics behind $cleanTopic are blowing minds worldwide!"
            }
            VideoNiche.SCIENCE_SPACE -> {
                if (isHindi) "रुकिए! विज्ञान की दुनिया में $cleanTopic पर एक ऐसा खुलासा हुआ है जिसने वैज्ञानिकों को भी सोचने पर मजबूर कर दिया है!"
                else "HOLD ON! A groundbreaking discovery about $cleanTopic is challenging our understanding of reality!"
            }
            VideoNiche.GAMING -> {
                if (isHindi) "गेमर्स ध्यान दें! $cleanTopic के बारे में यह हिडन गेमप्ले सीक्रेट 99% लोग नहीं जानते!"
                else "LISTEN UP GAMERS! This secret about $cleanTopic is something 99% of players completely missed!"
            }
            VideoNiche.FITNESS_HEALTH -> {
                if (isHindi) "रुकिए! $cleanTopic के बारे में यह हेल्थ हैक आपकी लाइफस्टाइल और एनर्जी को 10 गुना बढ़ा सकता है!"
                else "STOP SCROLLING! This evidence-based fact about $cleanTopic will transform how you approach your fitness!"
            }
            VideoNiche.MOTIVATION_FACTS, VideoNiche.PSYCHOLOGY_SECRETS -> {
                if (isHindi) "एक कड़वा सच! $cleanTopic को समझने वाला इंसान किसी भी फील्ड में कभी असफल नहीं हो सकता!"
                else "A PSYCHOLOGICAL TRUTH: Once you understand $cleanTopic, your entire perspective shifts forever!"
            }
            else -> {
                if (isHindi) "रुकिए! क्या आप जानते हैं कि $cleanTopic के पीछे का असली सच क्या है? जो अगले 60 सेकंड में आप जानेंगे, वह दंग कर देगा!"
                else "STOP SCROLLING! What you are about to discover about $cleanTopic will completely shatter what you thought you knew!"
            }
        }

        val cta = when (niche) {
            VideoNiche.TECHNOLOGY, VideoNiche.AI_FUTURISTIC -> {
                if (isHindi) "क्या आप इस नई टेक्नोलॉजी को अपनाने के लिए तैयार हैं? कमेंट में बताएं और लेटेस्ट टेक वीडियोज़ के लिए सब्सक्राइब करें!"
                else "Are you ready for this technological leap? Let us know in the comments and subscribe for future tech insights!"
            }
            else -> {
                if (isHindi) "क्या आप इस पर विश्वास करते हैं? अपनी राय कमेंट में बताएं और अगली एक्सक्लूसिव वीडियो के लिए चैनल को अभी सब्सक्राइब करें!"
                else "Do you agree with this? Drop your thoughts in the comments and subscribe right now for daily viral deep dives!"
            }
        }

        val scenes = mutableListOf<SceneBreakdownItem>()

        if (isHindi) {
            when (niche) {
                VideoNiche.TECHNOLOGY, VideoNiche.AI_FUTURISTIC -> {
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 1,
                            timestampRange = "00:00 - 00:08",
                            visualCue = "Dynamic futuristic 4K macro zoom on high-tech circuits and holographic interface for $cleanTopic.",
                            voiceoverText = "रुकिए! क्या आप जानते हैं कि $cleanTopic टेक की दुनिया को कैसे हमेशा के लिए बदल रहा है?",
                            sfxCue = "⚡ High-Tech Digital Glitch + Power Surge",
                            retentionAngle = "High-Impact Hook"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 2,
                            timestampRange = "00:08 - 00:20",
                            visualCue = "Sleek split-screen demonstrating rapid automated innovation, code matrices, and engineering breakthroughs.",
                            voiceoverText = "यह सिर्फ कोई साधारण अपग्रेड नहीं है, बल्कि एक ऐसा तकनीकी चमत्कार है जो स्पीड, एक्यूरेसी और वर्कफ्लो को पूरी तरह री-डिफाइन करता है।",
                            sfxCue = "🖥️ Digital Data Stream + Sub Pulse",
                            retentionAngle = "Value Breakdown"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 3,
                            timestampRange = "00:20 - 00:40",
                            visualCue = "High-contrast cinematic reveal of real-world results, ultra-fast performance benchmarks and futuristic automation.",
                            voiceoverText = "दुनिया भर के टॉप एक्सपर्ट्स का मानना है कि जो लोग $cleanTopic को समय रहते समझ लेंगे, वे अपने काम में 10 गुना आगे निकल जाएंगे।",
                            sfxCue = "💥 Cinematic Impact Whoosh",
                            retentionAngle = "Game-Changing Insight"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 4,
                            timestampRange = "00:40 - 00:60",
                            visualCue = "Sleek glowing tech outro with animated subscribe icon and futuristic backdrop.",
                            voiceoverText = "क्या आप इस क्रांति का हिस्सा बनने के लिए तैयार हैं? नीचे कमेंट में अपनी राय लिखें और डेली टेक अपडेट्स के लिए तुरंत सब्सक्राइब करें!",
                            sfxCue = "🔔 Subtle Bell Chime + Tech Bass Outro",
                            retentionAngle = "Audience Retention & Subscribe CTA"
                        )
                    )
                }
                else -> {
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 1,
                            timestampRange = "00:00 - 00:08",
                            visualCue = "Extreme macro zoom in on subject ($cleanTopic), dramatic dark vignette, fast zoom cut, high-contrast lighting.",
                            voiceoverText = "रुकिए! $cleanTopic के बारे में जो जानकारी सामने आई है, उसने सबको सोचने पर मजबूर कर दिया है!",
                            sfxCue = "⚡ Deep Sub-Bass Boom + Whoosh Transition",
                            retentionAngle = "Pattern Interrupt Hook"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 2,
                            timestampRange = "00:08 - 00:20",
                            visualCue = "Fast-paced visual sequence displaying factual evidence, research charts, and detailed dynamic visuals.",
                            voiceoverText = "जब इस विषय की गहराई से पड़ताल की गई, तो ऐसे महत्वपूर्ण तथ्य सामने आए जो आमतौर पर नजरअंदाज कर दिए जाते हैं।",
                            sfxCue = "🔍 Focused Ambient Rhythm",
                            retentionAngle = "Evidence & Intrigue"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 3,
                            timestampRange = "00:20 - 00:40",
                            visualCue = "High-contrast climax visual, sudden light flare, intense subject reveal with slow pan.",
                            voiceoverText = "यह कोई इत्तेफाक नहीं है, बल्कि एक ऐसा गेम-चेंजर पहलू है जो $cleanTopic के पूरे समीकरण को बदल देता है।",
                            sfxCue = "💥 Sudden Impact Riser + Metal Clatter",
                            retentionAngle = "Climax Revelation"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 4,
                            timestampRange = "00:40 - 00:60",
                            visualCue = "Cinematic slow zoom out into dark horizon, call-to-action banner text pulse.",
                            voiceoverText = "क्या आप इससे सहमत हैं? अपनी राय नीचे कमेंट्स में लिखें और ऐसे ही एक्सक्लूसिव वीडियोज़ के लिए चैनल को अभी सब्सक्राइब करें!",
                            sfxCue = "🔔 Subtle Subscribe Bell Ring + Bass Outro",
                            retentionAngle = "Subtle Retention Loop & CTA"
                        )
                    )
                }
            }
        } else {
            when (niche) {
                VideoNiche.TECHNOLOGY, VideoNiche.AI_FUTURISTIC -> {
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 1,
                            timestampRange = "00:00 - 00:08",
                            visualCue = "Macro 4K visual of quantum processor and neural architecture representing $cleanTopic.",
                            voiceoverText = "STOP SCROLLING! $cleanTopic is redefining everything we know about tech and innovation!",
                            sfxCue = "⚡ Cybernetic Pulse + Impact Whoosh",
                            retentionAngle = "Pattern Interrupt Hook"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 2,
                            timestampRange = "00:08 - 00:20",
                            visualCue = "Fast animated workflow showcasing exponential efficiency and benchmark breakthroughs.",
                            voiceoverText = "This breakthrough eliminates traditional limitations, delivering unprecedented speed and precision in real time.",
                            sfxCue = "🖥️ Digital Synthesizer Riser",
                            retentionAngle = "Core Breakdown"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 3,
                            timestampRange = "00:20 - 00:40",
                            visualCue = "Cinematic slow pan across futuristic landscape powered by autonomous systems.",
                            voiceoverText = "Top engineers and founders agree: mastering $cleanTopic today is the ultimate unfair advantage.",
                            sfxCue = "💥 Dynamic Impact Drop",
                            retentionAngle = "High Stakes Revelation"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 4,
                            timestampRange = "00:40 - 00:60",
                            visualCue = "Polished cinematic outro with subscribe graphic and next-gen visual gradient.",
                            voiceoverText = "Are you adopting this tech? Drop your perspective in the comments and subscribe for cutting-edge updates!",
                            sfxCue = "🔔 Crisp Bell Chime + Outro Fade",
                            retentionAngle = "Conversion Call to Action"
                        )
                    )
                }
                else -> {
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 1,
                            timestampRange = "00:00 - 00:08",
                            visualCue = "Extreme macro zoom on subject ($cleanTopic), dark vignetting, rapid zoom-cut, high-contrast flash.",
                            voiceoverText = "STOP SCROLLING! The real facts behind $cleanTopic are finally coming to light!",
                            sfxCue = "⚡ Sub-Bass Impact + Cinematic Whoosh",
                            retentionAngle = "Pattern Interrupt Hook"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 2,
                            timestampRange = "00:08 - 00:20",
                            visualCue = "Rapid sequence of detailed evidence, verified data points, and atmospheric lighting shifts.",
                            voiceoverText = "When experts analyzed the underlying metrics, they uncovered astonishing patterns that change the narrative.",
                            sfxCue = "🔍 Low Suspense Pulse Drone",
                            retentionAngle = "Intrigue & Evidence"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 3,
                            timestampRange = "00:20 - 00:40",
                            visualCue = "Climax high-contrast scene, slow motion cinematic pan on key dramatic focal point.",
                            voiceoverText = "This breakthrough demonstrates why understanding $cleanTopic will redefine your entire strategy.",
                            sfxCue = "💥 Cinematic Anvil Riser + Distant Thunder",
                            retentionAngle = "Climax Revelation"
                        )
                    )
                    scenes.add(
                        SceneBreakdownItem(
                            sceneNumber = 4,
                            timestampRange = "00:40 - 00:60",
                            visualCue = "Dramatic slow pull-back into darkness, fading into a clean closing title card.",
                            voiceoverText = "What is your theory? Share your thoughts and keep exploring this topic!",
                            sfxCue = "🔔 Subtle Bell Chime + Outro Fade",
                            retentionAngle = "Conversion Call to Action"
                        )
                    )
                }
            }
        }

        val fullText = scenes.joinToString(" ") { it.voiceoverText }
        val visualPrompt =
            "Cinematic vertical video scene about $cleanTopic in the $category category, " +
                "detailed composition, expressive lighting, realistic visual storytelling, no text."

        return ViralScriptBreakdown(
            topic = cleanTopic,
            category = category,
            language = language,
            viralHook = hook,
            totalEstimatedDurationSec = targetDurationSec,
            fullNarrationText = fullText,
            sceneBreakdown = scenes,
            endScreenCallToAction = cta,
            visualPrompt8k = visualPrompt
        )
    }
}
