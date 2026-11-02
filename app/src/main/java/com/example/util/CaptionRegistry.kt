package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Data model for a caption preset loaded from captions_100.json
 */
data class CaptionStyle(
    val id: Int,
    val styleCode: String,
    val styleName: String,
    val category: String,
    val fontName: String,
    val fontSize: Int,
    val primaryColorHex: String,
    val highlightColorHex: String,
    val outlineColorHex: String,
    val boxBackgroundColorHex: String,
    val borderStyle: Int, // 1 = Outline + Shadow, 3 = Box Background / Pill
    val animationStyle: String,
    val description: String,
    val bestSuitedFor: String
)

object CaptionRegistry {

    private const val TAG = "CaptionRegistry"
    private var cachedStyles: List<CaptionStyle>? = null

    /**
     * Loads all 100 caption styles from assets, local file, or fallback memory generator.
     */
    fun getStyles(context: Context? = null): List<CaptionStyle> {
        cachedStyles?.let { return it }

        val loaded = tryLoadFromJson(context)
        val finalStyles = if (loaded.isNotEmpty()) {
            // Expand to 100 items if loaded items are fewer
            if (loaded.size < 100) {
                loaded + generateFallbackStyles().drop(loaded.size)
            } else {
                loaded
            }
        } else {
            generateFallbackStyles()
        }

        cachedStyles = finalStyles
        return finalStyles
    }

    private fun tryLoadFromJson(context: Context?): List<CaptionStyle> {
        val result = mutableListOf<CaptionStyle>()
        var jsonString: String? = null

        // 1. Try reading from Android context assets
        if (context != null) {
            try {
                jsonString = context.assets.open("captions_100.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.d(TAG, "Assets load notice for captions_100.json: ${e.message}")
            }
        }

        // 2. Try reading from root filesystem paths
        if (jsonString.isNullOrBlank()) {
            val potentialPaths = listOf(
                "captions_100.json",
                "/captions_100.json",
                "app/src/main/assets/captions_100.json"
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
            val arr = root.optJSONArray("styles") ?: return emptyList()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val id = item.optInt("id", i + 1)
                val code = item.optString("style_code", "style_$id")
                val name = item.optString("style_name", "Caption Style $id")
                val category = item.optString("category", "Viral Shorts")
                val fontName = item.optString("font_name", "Arial Black")
                val fontSize = item.optInt("font_size", 58)
                val primaryColor = item.optString("primary_color_hex", "#FFFFFF")
                val highlightColor = item.optString("highlight_color_hex", "#00FF66")
                val outlineColor = item.optString("outline_color_hex", "#000000")
                val boxBgColor = item.optString("box_bg_color_hex", "#CC000000")
                val borderStyle = item.optInt("border_style", 3)
                val animStyle = item.optString("animation_style", "Word Highlight Bounce")
                val desc = item.optString("description", "")
                val bestFor = item.optString("best_suited_for", "Viral Shorts & Reels")

                result.add(
                    CaptionStyle(
                        id = id,
                        styleCode = code,
                        styleName = name,
                        category = category,
                        fontName = fontName,
                        fontSize = fontSize,
                        primaryColorHex = primaryColor,
                        highlightColorHex = highlightColor,
                        outlineColorHex = outlineColor,
                        boxBackgroundColorHex = boxBgColor,
                        borderStyle = borderStyle,
                        animationStyle = animStyle,
                        description = desc,
                        bestSuitedFor = bestFor
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing captions_100.json: ${e.message}", e)
        }

        return result
    }

    /**
     * Pick a random caption style from the 100 database.
     */
    fun getRandomStyle(context: Context? = null): CaptionStyle {
        val list = getStyles(context)
        return list.randomOrNull() ?: generateFallbackStyles().first()
    }

    /**
     * Automatically pick distinct random caption styles for multiple scenes.
     */
    fun getRandomStylesForScenes(
        sceneCount: Int,
        categoryFilter: String? = null,
        context: Context? = null
    ): List<CaptionStyle> {
        val all = getStyles(context)
        val filtered = if (!categoryFilter.isNullOrBlank() && categoryFilter != "All") {
            all.filter { it.category.contains(categoryFilter, ignoreCase = true) || it.styleName.contains(categoryFilter, ignoreCase = true) }
                .ifEmpty { all }
        } else {
            all
        }

        val count = sceneCount.coerceAtLeast(1)
        val shuffled = filtered.shuffled()
        val result = mutableListOf<CaptionStyle>()
        for (i in 0 until count) {
            result.add(shuffled[i % shuffled.size])
        }
        return result
    }

    /**
     * Search caption styles by keyword or category.
     */
    fun searchStyles(query: String, category: String? = null, context: Context? = null): List<CaptionStyle> {
        val all = getStyles(context)
        return all.filter { style ->
            val matchQuery = query.isBlank() ||
                    style.styleName.contains(query, ignoreCase = true) ||
                    style.styleCode.contains(query, ignoreCase = true) ||
                    style.description.contains(query, ignoreCase = true) ||
                    style.animationStyle.contains(query, ignoreCase = true)

            val matchCategory = category.isNullOrBlank() || category == "All" ||
                    style.category.contains(category, ignoreCase = true)

            matchQuery && matchCategory
        }
    }

    /**
     * Get unique categories available in captions_100.json
     */
    fun getCategories(context: Context? = null): List<String> {
        val all = getStyles(context)
        val cats = mutableSetOf("All")
        all.forEach {
            cats.add(it.category.trim())
        }
        return cats.toList()
    }

    /**
     * Converts a Hex color (#RRGGBB or #AARRGGBB) to ASS color syntax &HAABBGGRR.
     */
    fun hexToAssColor(hex: String): String {
        val clean = hex.removePrefix("#")
        return when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2)
                val g = clean.substring(2, 4)
                val b = clean.substring(4, 6)
                "&H00$b$g$r&"
            }
            8 -> {
                val a = clean.substring(0, 2)
                val r = clean.substring(2, 4)
                val g = clean.substring(4, 6)
                val b = clean.substring(6, 8)
                "&H$a$b$g$r&"
            }
            else -> "&H0066FF00&"
        }
    }

    /**
     * Renders kinetic caption on a Hardware Canvas with dynamic position shifts,
     * background card color variations, and word-by-word pop animations.
     */
    fun renderCanvasKineticCaption(
        canvas: Canvas,
        caption: String,
        width: Int,
        height: Int,
        style: CaptionStyle,
        progressInScene: Float = 0.5f
    ) {
        val words = caption.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return

        // Calculate active word index based on scene progress (0.0 to 1.0)
        val activeWordIndex = ((words.size * progressInScene.coerceIn(0f, 0.99f)).toInt()).coerceIn(0, words.size - 1)

        // Divide words into punchy 3-word viral chunks
        val chunkSize = 3
        val currentChunkIndex = activeWordIndex / chunkSize
        val chunkStart = currentChunkIndex * chunkSize
        val chunkWords = words.subList(chunkStart, (chunkStart + chunkSize).coerceAtMost(words.size))
        val activeIndexInChunk = activeWordIndex - chunkStart

        val typeface = when {
            style.fontName.contains("Trebuchet", ignoreCase = true) || style.fontName.contains("Impact", ignoreCase = true) -> Typeface.DEFAULT_BOLD
            style.fontName.contains("Mono", ignoreCase = true) -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT_BOLD
        }

        // Fixed stable position in lower third (standard Shorts / Reels position)
        val yTargetRatio = 0.72f

        // Dynamic background pill card styles
        val cardThemes = listOf(
            Pair(Color.argb(220, 10, 15, 25), Color.parseColor("#00FF66")),  // Cyber Dark + Green
            Pair(Color.argb(220, 35, 8, 15), Color.parseColor("#FFE600")),   // Crimson + Gold
            Pair(Color.argb(220, 15, 10, 35), Color.parseColor("#00E5FF")),  // Purple + Cyan
            Pair(Color.argb(230, 5, 5, 10), Color.parseColor("#FF007F"))     // Obsidian + Hot Pink
        )
        val (boxBgColor, highlightColor) = cardThemes[currentChunkIndex % cardThemes.size]
        val primaryColor = parseColorSafely(style.primaryColorHex, Color.WHITE)
        val outlineColor = Color.BLACK

        val textSizePx = (style.fontSize * (height / 1920f)).coerceAtLeast(42f)

        val basePaint = Paint().apply {
            textSize = textSizePx
            this.typeface = typeface
            isAntiAlias = true
        }

        val outlinePaint = Paint(basePaint).apply {
            color = outlineColor
            this.style = Paint.Style.STROKE
            strokeWidth = (textSizePx * 0.16f).coerceAtLeast(6f)
        }

        val primaryPaint = Paint(basePaint).apply {
            color = primaryColor
            this.style = Paint.Style.FILL
        }

        val highlightPaint = Paint(basePaint).apply {
            color = highlightColor
            textSize = textSizePx * 1.25f // 25% scale pop animation for active spoken word
            this.style = Paint.Style.FILL
            setShadowLayer(22f, 0f, 0f, highlightColor)
        }

        // Measure widths of each word in chunk
        var totalChunkWidth = 0f
        val wordWidths = FloatArray(chunkWords.size)
        val spaceWidth = basePaint.measureText(" ")

        for (i in chunkWords.indices) {
            val isHighlighted = (i == activeIndexInChunk)
            val p = if (isHighlighted) highlightPaint else primaryPaint
            wordWidths[i] = p.measureText(chunkWords[i])
            totalChunkWidth += wordWidths[i] + (if (i < chunkWords.size - 1) spaceWidth else 0f)
        }

        val pillWidth = (totalChunkWidth + 100f).coerceAtMost(width - 40f)
        val pillHeight = (textSizePx * 2.5f)
        val pillLeft = (width - pillWidth) / 2f
        val pillTop = (height * yTargetRatio) - (pillHeight / 2f)

        val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)

        val pillPaint = Paint().apply {
            color = boxBgColor
            isAntiAlias = true
            this.style = Paint.Style.FILL
        }

        val pillStrokePaint = Paint().apply {
            color = highlightColor
            isAntiAlias = true
            this.style = Paint.Style.STROKE
            strokeWidth = 4.5f
        }

        // Draw Pill/Card Background
        canvas.drawRoundRect(pillRect, 28f, 28f, pillPaint)
        canvas.drawRoundRect(pillRect, 28f, 28f, pillStrokePaint)

        val baselineY = pillTop + (pillHeight / 2f) + (textSizePx / 3f)
        var currentX = (width - totalChunkWidth) / 2f

        // Draw words one by one with kinetic highlight on activeWordIndex
        var activeEmoji: String? = null
        for (i in chunkWords.indices) {
            val word = chunkWords[i]
            val isHighlighted = (i == activeIndexInChunk)

            if (isHighlighted) {
                activeEmoji = getMatchedEmoji(word)
            }

            // Draw outline
            outlinePaint.textSize = if (isHighlighted) textSizePx * 1.25f else textSizePx
            canvas.drawText(word, currentX, baselineY, outlinePaint)

            // Draw text
            if (isHighlighted) {
                canvas.drawText(word, currentX, baselineY, highlightPaint)
            } else {
                canvas.drawText(word, currentX, baselineY, primaryPaint)
            }

            currentX += wordWidths[i] + spaceWidth
        }

        // Draw floating keyword emoji badge on top of card if matched
        if (activeEmoji != null) {
            val emojiPaint = Paint().apply {
                textSize = textSizePx * 1.30f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val badgeY = pillTop - 18f
            val badgeX = width / 2f
            canvas.drawText(activeEmoji, badgeX, badgeY, emojiPaint)
        }
    }

    private fun getMatchedEmoji(word: String): String? {
        val clean = word.lowercase().replace("[^a-z0-9]".toRegex(), "")
        return when {
            clean.contains("secret") || clean.contains("hidden") || clean.contains("truth") -> "🤫"
            clean.contains("money") || clean.contains("cash") || clean.contains("rich") || clean.contains("dollar") -> "💰"
            clean.contains("fire") || clean.contains("hot") || clean.contains("viral") -> "🔥"
            clean.contains("shock") || clean.contains("scary") || clean.contains("dark") || clean.contains("mystery") -> "😱"
            clean.contains("ai") || clean.contains("bot") || clean.contains("cyber") || clean.contains("tech") -> "🤖"
            clean.contains("future") || clean.contains("space") || clean.contains("rocket") -> "🚀"
            clean.contains("brain") || clean.contains("mind") || clean.contains("think") -> "🧠"
            clean.contains("warning") || clean.contains("danger") || clean.contains("stop") -> "🚨"
            clean.contains("time") || clean.contains("history") || clean.contains("ancient") -> "⏳"
            clean.contains("idea") || clean.contains("light") || clean.contains("power") || clean.contains("electric") -> "⚡"
            else -> null
        }
    }

    private fun parseColorSafely(hex: String, defaultColor: Int): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            defaultColor
        }
    }

    /**
     * Generates a list of 100 complete caption styles as fallback generator.
     */
    private fun generateFallbackStyles(): List<CaptionStyle> {
        val templates = listOf(
            Tuple10("hormozi_yellow_pill", "Hormozi Viral Yellow Pill", "Viral Shorts", "Arial Black", 60, "#FFFFFF", "#FFE600", "#000000", "#CC000000", 3),
            Tuple10("cyberpunk_neon_green", "Cyberpunk Matrix Green", "Cyberpunk", "Trebuchet MS", 58, "#00FF66", "#00FFFF", "#003311", "#E6051A0E", 3),
            Tuple10("mrbeast_pop_red", "MrBeast High Octane Red", "Gaming", "Impact", 64, "#FFFFFF", "#FF0033", "#000000", "#FF000000", 1),
            Tuple10("minimal_typewriter", "Minimalist Typewriter Mono", "Minimalist", "Monospace", 52, "#F0F0F0", "#00FFCC", "#111111", "#80000000", 3),
            Tuple10("sunset_gradient_pop", "Sunset Gold Gradient", "Cinematic", "Verdana", 56, "#FF9900", "#FF0066", "#220011", "#B3110008", 3),
            Tuple10("glitch_digital_noise", "Digital Glitch Shift", "Glitch", "Trebuchet MS", 58, "#00FFFF", "#FF0055", "#000000", "#DD000A14", 3),
            Tuple10("comic_book_yellow", "Comic Book Hero Yellow", "Comic", "Arial Black", 62, "#FFFF00", "#FF3300", "#000000", "#00000000", 1),
            Tuple10("neon_blue_glow", "Neon Cyan Lightbar", "Neon", "Arial Black", 56, "#E0FFFF", "#00E5FF", "#002233", "#CC00101A", 3),
            Tuple10("retro_synthwave_pink", "80s Synthwave Magenta", "Retro", "Trebuchet MS", 58, "#FF00AA", "#00FFFF", "#220022", "#D218001F", 3),
            Tuple10("podcast_clean_white", "Podcast Pro Clean Subtitle", "Podcast", "Verdana", 54, "#FFFFFF", "#00FF88", "#000000", "#B3000000", 3)
        )

        val result = mutableListOf<CaptionStyle>()
        for (i in 1..100) {
            val tmpl = templates[(i - 1) % templates.size]
            val code = if (i <= templates.size) tmpl.c1 else "${tmpl.c1}_v${i / templates.size + 1}"
            val name = if (i <= templates.size) tmpl.c2 else "${tmpl.c2} #${i}"

            result.add(
                CaptionStyle(
                    id = i,
                    styleCode = code,
                    styleName = name,
                    category = tmpl.c3,
                    fontName = tmpl.c4,
                    fontSize = tmpl.c5 + (i % 3) * 2,
                    primaryColorHex = tmpl.c6,
                    highlightColorHex = tmpl.c7,
                    outlineColorHex = tmpl.c8,
                    boxBackgroundColorHex = tmpl.c9,
                    borderStyle = tmpl.c10,
                    animationStyle = "Word-by-Word Highlight Bounce",
                    description = "High-retention viral 9:16 caption style $name with dynamic color accents.",
                    bestSuitedFor = "Short-form videos"
                )
            )
        }
        return result
    }

    private data class Tuple10(
        val c1: String, val c2: String, val c3: String, val c4: String, val c5: Int,
        val c6: String, val c7: String, val c8: String, val c9: String, val c10: Int
    )
}
