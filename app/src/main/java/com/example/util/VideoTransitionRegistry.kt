package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/**
 * Data model for a transition effect parsed from video_transition_effects_100.json
 */
data class VideoTransitionEffect(
    val id: Int,
    val effectCode: String,
    val effectName: String,
    val category: String,
    val recommendedOrientation: String,
    val description: String,
    val visualBehavior: String,
    val recommendedDurationSec: Double,
    val motionType: String,
    val bestSuitedFor: String,
    val xfadeName: String
)

object VideoTransitionRegistry {

    private const val TAG = "VideoTransitionRegistry"
    private var cachedEffects: List<VideoTransitionEffect>? = null

    /**
     * Map effect codes to FFmpeg xfade transition names.
     */
    fun mapCodeToXfade(effectCode: String): String {
        return when (effectCode.lowercase().trim()) {
            "fade_to_black", "fade_black" -> "fadeblack"
            "fade_to_white", "flash_white" -> "fadewhite"
            "cross_dissolve", "smooth_dissolve", "dissolve" -> "dissolve"
            "zoom_in_push", "zoom_in", "zoom_punch" -> "zoomin"
            "zoom_out_pull", "zoom_out" -> "fade"
            "whip_pan_left", "slide_left", "fast_pan_left" -> "slideleft"
            "whip_pan_right", "slide_right", "fast_pan_right" -> "slideright"
            "whip_pan_up", "slide_up" -> "slideup"
            "whip_pan_down", "slide_down" -> "slidedown"
            "wipe_left" -> "wipeleft"
            "wipe_right" -> "wiperight"
            "wipe_up" -> "wipeup"
            "wipe_down" -> "wipedown"
            "circle_crop", "circle_open", "iris_open" -> "circleopen"
            "circle_close", "iris_close" -> "circleclose"
            "radial_wipe", "clock_wipe" -> "radial"
            "smooth_left" -> "smoothleft"
            "smooth_right" -> "smoothright"
            "smooth_up" -> "smoothup"
            "smooth_down" -> "smoothdown"
            "pixelize", "mosaic_blur", "pixel_dissolve" -> "pixelize"
            "glitch_displacement", "glitch_shift", "digital_noise", "glitch_rgb" -> "diagtl"
            "spin_transition", "vortex_spin", "360_spin" -> "circlecrop"
            "page_turn", "book_flip" -> "diagtr"
            "split_screen_wipe", "vertical_split" -> "vertopen"
            "horizontal_split" -> "horzopen"
            "diagonal_tl" -> "diagtl"
            "diagonal_tr" -> "diagtr"
            "diagonal_bl" -> "diagbl"
            "diagonal_br" -> "diagbr"
            "squeeze_v" -> "squeezev"
            "squeeze_h" -> "squeezeh"
            else -> "fade"
        }
    }

    /**
     * Loads all 100 transition effects from assets, local file, or fallback memory list.
     */
    fun getEffects(context: Context? = null): List<VideoTransitionEffect> {
        cachedEffects?.let { return it }

        val loaded = tryLoadFromJson(context)
        val finalEffects = if (loaded.isNotEmpty()) {
            loaded
        } else {
            generateFallback100Effects()
        }
        cachedEffects = finalEffects
        return finalEffects
    }

    private fun tryLoadFromJson(context: Context?): List<VideoTransitionEffect> {
        val result = mutableListOf<VideoTransitionEffect>()
        var jsonString: String? = null

        // 1. Try reading from Android context assets
        if (context != null) {
            try {
                jsonString = context.assets.open("video_transition_effects_100.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.d(TAG, "Assets load notice: ${e.message}")
            }
        }

        // 2. Try reading from root filesystem paths
        if (jsonString.isNullOrBlank()) {
            val potentialPaths = listOf(
                "video_transition_effects_100.json",
                "/video_transition_effects_100.json",
                "app/src/main/assets/video_transition_effects_100.json"
            )
            for (p in potentialPaths) {
                val f = File(p)
                if (f.exists() && f.length() > 0) {
                    try {
                        jsonString = f.readText()
                        break
                    } catch (e: Exception) {
                        Log.d(TAG, "File read notice for $p: ${e.message}")
                    }
                }
            }
        }

        if (jsonString.isNullOrBlank()) return emptyList()

        try {
            val root = JSONObject(jsonString)
            val effectsArr = root.optJSONArray("effects") ?: return emptyList()
            for (i in 0 until effectsArr.length()) {
                val item = effectsArr.getJSONObject(i)
                val id = item.optInt("id", i + 1)
                val code = item.optString("effect_code", "effect_$id")
                val name = item.optString("effect_name", "Effect $id")
                val category = item.optString("category", "General")
                val orientation = item.optString("recommended_orientation", "universal")
                val desc = item.optString("description", "")
                
                val details = item.optJSONObject("effect_details")
                val visualBehavior = details?.optString("visual_behavior", desc) ?: desc
                val durationSec = details?.optDouble("recommended_duration_sec", 0.6) ?: 0.6

                val motionObj = item.optJSONObject("image_motion_and_transformation")
                val motionType = motionObj?.optString("motion_type", "Smooth Transition") ?: "Smooth Transition"

                val contextObj = item.optJSONObject("context_and_use_case")
                val bestSuitedFor = contextObj?.optString("best_suited_for", "Viral Shorts & Stories") ?: "Viral Shorts & Stories"

                val xfade = mapCodeToXfade(code)

                result.add(
                    VideoTransitionEffect(
                        id = id,
                        effectCode = code,
                        effectName = name,
                        category = category,
                        recommendedOrientation = orientation,
                        description = desc,
                        visualBehavior = visualBehavior,
                        recommendedDurationSec = durationSec,
                        motionType = motionType,
                        bestSuitedFor = bestSuitedFor,
                        xfadeName = xfade
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing transition JSON: ${e.message}", e)
        }

        return result
    }

    /**
     * Selects a random transition effect from the 100 effects database.
     */
    fun getRandomEffect(context: Context? = null): VideoTransitionEffect {
        val list = getEffects(context)
        return list.randomOrNull() ?: generateFallback100Effects().first()
    }

    /**
     * Automatically assigns a distinct random transition effect for each scene boundary.
     */
    fun getRandomTransitionsForScenes(
        sceneCount: Int,
        categoryFilter: String? = null,
        context: Context? = null
    ): List<VideoTransitionEffect> {
        val allEffects = getEffects(context)
        val filtered = if (!categoryFilter.isNullOrBlank() && categoryFilter != "All") {
            allEffects.filter { it.category.contains(categoryFilter, ignoreCase = true) || it.effectName.contains(categoryFilter, ignoreCase = true) }
                .ifEmpty { allEffects }
        } else {
            allEffects
        }

        val transitionsCount = (sceneCount - 1).coerceAtLeast(1)
        val shuffledPool = filtered.shuffled()

        val results = mutableListOf<VideoTransitionEffect>()
        for (i in 0 until transitionsCount) {
            results.add(shuffledPool[i % shuffledPool.size])
        }
        return results
    }

    /**
     * Search effects by keyword, category, or motion type.
     */
    fun searchEffects(query: String, category: String? = null, context: Context? = null): List<VideoTransitionEffect> {
        val all = getEffects(context)
        return all.filter { effect ->
            val matchQuery = query.isBlank() ||
                    effect.effectName.contains(query, ignoreCase = true) ||
                    effect.effectCode.contains(query, ignoreCase = true) ||
                    effect.description.contains(query, ignoreCase = true) ||
                    effect.visualBehavior.contains(query, ignoreCase = true) ||
                    effect.category.contains(query, ignoreCase = true)

            val matchCategory = category.isNullOrBlank() || category == "All" ||
                    effect.category.contains(category, ignoreCase = true)

            matchQuery && matchCategory
        }
    }

    /**
     * List all unique categories available in the 100 transitions dataset.
     */
    fun getCategories(context: Context? = null): List<String> {
        val all = getEffects(context)
        val cats = mutableSetOf("All")
        all.forEach {
            it.category.split(",").forEach { c ->
                val trimmed = c.trim()
                if (trimmed.isNotEmpty()) cats.add(trimmed)
            }
        }
        return cats.toList()
    }

    /**
     * Renders high-fidelity visual transition between two frames on Android Hardware Canvas.
     * Supports Glitch Displacement, Whip Pans, Zoom Push/Pulls, 360 Spins, Crossfades, Flash White/Black, etc.
     */
    fun renderCanvasTransition(
        canvas: Canvas,
        currentBmp: Bitmap,
        nextBmp: Bitmap,
        transitionRatio: Float, // 0.0f (start of transition) to 1.0f (end of transition)
        width: Int,
        height: Int,
        effectCode: String
    ) {
        val t = transitionRatio.coerceIn(0f, 1f)
        val code = effectCode.lowercase().trim()

        val baseScaleNext = maxOf(width.toFloat() / nextBmp.width.toFloat(), height.toFloat() / nextBmp.height.toFloat())
        val nextMatrix = Matrix().apply {
            postScale(baseScaleNext, baseScaleNext)
            postTranslate((width - nextBmp.width * baseScaleNext) / 2f, (height - nextBmp.height * baseScaleNext) / 2f)
        }

        when {
            // 1. Glitch Displacement / Cyberpunk RGB Shift
            code.contains("glitch") || code.contains("noise") || code.contains("distortion") -> {
                // Draw next scene with increasing alpha
                val paintNext = Paint().apply { alpha = (t * 255).toInt() }
                canvas.drawBitmap(nextBmp, nextMatrix, paintNext)

                // Render erratic glitch slice bands & RGB displacement
                val sliceCount = (4..8).random()
                val glitchPaint = Paint().apply {
                    color = if (Random.nextBoolean()) Color.argb(120, 0, 255, 255) else Color.argb(120, 255, 0, 102)
                }
                for (s in 0 until sliceCount) {
                    val sliceY = Random.nextInt(0, height)
                    val sliceH = Random.nextInt(10, 50)
                    val shiftX = Random.nextInt(-40, 40).toFloat()
                    val sliceRect = Rect(0, sliceY, width, (sliceY + sliceH).coerceAtMost(height))
                    val dstRect = RectF(shiftX, sliceY.toFloat(), width.toFloat() + shiftX, (sliceY + sliceH).toFloat())
                    canvas.drawRect(dstRect, glitchPaint)
                }
            }

            // 2. Whip Pan Left / Slide Left
            code.contains("left") -> {
                val offsetCurrent = -width * t
                val offsetNext = width * (1f - t)

                val mNext = Matrix().apply {
                    postScale(baseScaleNext, baseScaleNext)
                    postTranslate((width - nextBmp.width * baseScaleNext) / 2f + offsetNext, (height - nextBmp.height * baseScaleNext) / 2f)
                }
                canvas.drawBitmap(nextBmp, mNext, null)
            }

            // 3. Whip Pan Right / Slide Right
            code.contains("right") -> {
                val offsetNext = -width * (1f - t)
                val mNext = Matrix().apply {
                    postScale(baseScaleNext, baseScaleNext)
                    postTranslate((width - nextBmp.width * baseScaleNext) / 2f + offsetNext, (height - nextBmp.height * baseScaleNext) / 2f)
                }
                canvas.drawBitmap(nextBmp, mNext, null)
            }

            // 4. Whip Pan Up / Slide Up
            code.contains("up") -> {
                val offsetNext = height * (1f - t)
                val mNext = Matrix().apply {
                    postScale(baseScaleNext, baseScaleNext)
                    postTranslate((width - nextBmp.width * baseScaleNext) / 2f, (height - nextBmp.height * baseScaleNext) / 2f + offsetNext)
                }
                canvas.drawBitmap(nextBmp, mNext, null)
            }

            // 5. Whip Pan Down / Slide Down
            code.contains("down") -> {
                val offsetNext = -height * (1f - t)
                val mNext = Matrix().apply {
                    postScale(baseScaleNext, baseScaleNext)
                    postTranslate((width - nextBmp.width * baseScaleNext) / 2f, (height - nextBmp.height * baseScaleNext) / 2f + offsetNext)
                }
                canvas.drawBitmap(nextBmp, mNext, null)
            }

            // 6. Zoom In Push / Punch
            code.contains("zoom_in") || code.contains("push") -> {
                val scale = 0.8f + (0.2f * t)
                val alpha = (t * 255).toInt()
                val mNext = Matrix().apply {
                    val s = baseScaleNext * scale
                    postScale(s, s)
                    postTranslate((width - nextBmp.width * s) / 2f, (height - nextBmp.height * s) / 2f)
                }
                val p = Paint().apply { this.alpha = alpha }
                canvas.drawBitmap(nextBmp, mNext, p)
            }

            // 7. Zoom Out Pull
            code.contains("zoom_out") || code.contains("pull") -> {
                val scale = 1.3f - (0.3f * t)
                val alpha = (t * 255).toInt()
                val mNext = Matrix().apply {
                    val s = baseScaleNext * scale
                    postScale(s, s)
                    postTranslate((width - nextBmp.width * s) / 2f, (height - nextBmp.height * s) / 2f)
                }
                val p = Paint().apply { this.alpha = alpha }
                canvas.drawBitmap(nextBmp, mNext, p)
            }

            // 8. 360 Spin / Vortex
            code.contains("spin") || code.contains("vortex") || code.contains("rotat") -> {
                val degrees = (1f - t) * 180f
                val scale = 0.5f + (0.5f * t)
                val alpha = (t * 255).toInt()
                val mNext = Matrix().apply {
                    val s = baseScaleNext * scale
                    postScale(s, s)
                    postRotate(degrees, (nextBmp.width * s) / 2f, (nextBmp.height * s) / 2f)
                    postTranslate((width - nextBmp.width * s) / 2f, (height - nextBmp.height * s) / 2f)
                }
                val p = Paint().apply { this.alpha = alpha }
                canvas.drawBitmap(nextBmp, mNext, p)
            }

            // 9. Fade to Black / Dip to Black
            code.contains("black") -> {
                if (t < 0.5f) {
                    val blackAlpha = (t * 2 * 255).toInt()
                    val p = Paint().apply { color = Color.argb(blackAlpha, 0, 0, 0) }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
                } else {
                    val nextAlpha = ((t - 0.5f) * 2 * 255).toInt()
                    val p = Paint().apply { this.alpha = nextAlpha }
                    canvas.drawBitmap(nextBmp, nextMatrix, p)
                }
            }

            // 10. Flash White / Light Leak
            code.contains("white") || code.contains("flash") || code.contains("leak") -> {
                if (t < 0.4f) {
                    val whiteAlpha = (t / 0.4f * 240).toInt()
                    val p = Paint().apply { color = Color.argb(whiteAlpha, 255, 255, 255) }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
                } else {
                    val p = Paint().apply { this.alpha = (t * 255).toInt() }
                    canvas.drawBitmap(nextBmp, nextMatrix, p)
                    val flashRemain = ((1f - t) / 0.6f * 180).toInt().coerceAtLeast(0)
                    val flashPaint = Paint().apply { color = Color.argb(flashRemain, 255, 255, 255) }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
                }
            }

            // 11. Circle / Iris Wipe
            code.contains("circle") || code.contains("iris") -> {
                canvas.save()
                val path = Path().apply {
                    val maxRadius = kotlin.math.hypot(width.toDouble(), height.toDouble()).toFloat() / 2f
                    addCircle(width / 2f, height / 2f, maxRadius * t, Path.Direction.CW)
                }
                canvas.clipPath(path)
                canvas.drawBitmap(nextBmp, nextMatrix, null)
                canvas.restore()
            }

            // Default: Smooth Cross Dissolve
            else -> {
                val p = Paint().apply { alpha = (t * 255).toInt() }
                canvas.drawBitmap(nextBmp, nextMatrix, p)
            }
        }
    }

    private fun generateFallback100Effects(): List<VideoTransitionEffect> {
        val effectTemplates = listOf(
            Triple("fade_to_black", "Fade Out/In Black", "Portrait, Landscape"),
            Triple("cross_dissolve", "Cross Dissolve", "Nature, Vlog"),
            Triple("zoom_in_push", "Zoom In Push", "Action, Tech"),
            Triple("zoom_out_pull", "Zoom Out Pull", "Landscape, Corporate"),
            Triple("whip_pan_left", "Whip Pan Left", "Sports, Energy"),
            Triple("whip_pan_right", "Whip Pan Right", "Travel, Vlogs"),
            Triple("spin_transition", "360 Spin", "Music Videos, Reels"),
            Triple("glitch_displacement", "Glitch Shift", "Gaming, Tech"),
            Triple("page_turn", "3D Page Turn", "Education, Storytelling"),
            Triple("light_leak_flash", "Warm Light Leak", "Cinematic, Wedding"),
            Triple("split_screen_wipe", "Split Screen Wipe", "Comparison, Commercials"),
            Triple("radial_clock_wipe", "Clockwork Radial Wipe", "Documentary, Mystery"),
            Triple("film_roll_jitter", "Vintage Film Roll Jitter", "Retro, Nostalgia"),
            Triple("smooth_swish_up", "Smooth Swish Pan Up", "Real Estate, Fashion"),
            Triple("smooth_swish_down", "Smooth Swish Pan Down", "Architecture, Urban"),
            Triple("kaleidoscope_fractal", "Kaleidoscopic Shatter", "Trippy, Abstract"),
            Triple("luma_fade_bright", "High Key Luma Fade", "Dreamy, Heavenly"),
            Triple("burn_film_leader", "16mm Film Burn Exposure", "Music Videos, Grunge"),
            Triple("digital_pixel_scatter", "Quantum Pixel Scatter", "Sci-Fi, Cyberpunk"),
            Triple("cube_spin_3d", "3D Geometric Cube Flip", "Motion Graphics, Promos"),
            Triple("blur_stretch_directional", "Anamorphic Motion Blur", "High Speed, Racing"),
            Triple("ink_drop_splatter", "Watercolor Ink Bleed", "Artistic, History"),
            Triple("glass_shatter_prism", "Glass Prism Refraction", "Luxury, High-End Fashion"),
            Triple("tv_static_burst", "Analog CRT Static Burst", "Horror, Mystery"),
            Triple("elastic_bounce_snap", "Elastic Snapping Pop", "Kids, Cartoon, Comedy"),
            Triple("double_exposure_blend", "Double Exposure Luma Blend", "Poetry, Deep Thoughts"),
            Triple("vortex_tunnel_warp", "Hyperspace Vortex Warp", "Space, Science Fiction"),
            Triple("flash_bulb_strobe", "Paparazzi Flash Strobe", "Red Carpet, Celebrity"),
            Triple("liquid_melt_morph", "Thermal Liquid Dripping", "Trippy, Abstract"),
            Triple("rotational_zoom_streak", "Rotational Zoom Streak", "High Energy, Music")
        )

        val result = mutableListOf<VideoTransitionEffect>()
        var idCounter = 1

        for (i in 1..100) {
            val template = effectTemplates[(i - 1) % effectTemplates.size]
            val code = if (i <= effectTemplates.size) template.first else "${template.first}_v${i / effectTemplates.size + 1}"
            val name = if (i <= effectTemplates.size) template.second else "${template.second} #$i"
            val category = template.third

            result.add(
                VideoTransitionEffect(
                    id = idCounter++,
                    effectCode = code,
                    effectName = name,
                    category = category,
                    recommendedOrientation = if (i % 3 == 0) "vertical (9:16)" else "universal (16:9 & 9:16)",
                    description = "Dynamic cinematic visual transition ($name) for viral video synthesis.",
                    visualBehavior = "Applies high-impact $name transition with smooth motion trajectory.",
                    recommendedDurationSec = 0.5 + ((i % 4) * 0.1),
                    motionType = "Cinematic ${template.second} Motion",
                    bestSuitedFor = "Vertical short videos, tech, and cyberpunk scenes",
                    xfadeName = mapCodeToXfade(code)
                )
            )
        }
        return result
    }
}
