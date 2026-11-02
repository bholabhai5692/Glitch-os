package com.example.ui.keyboard

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.net.URLEncoder
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Service to fetch single-word auto-suggestions from Google Suggest API.
 * Ensures strictly 1-word clean suggestions (no multi-word phrases or sentences).
 */
object GoogleSuggestService {
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(900, TimeUnit.MILLISECONDS)
        .build()
    private val wordBoundary = Regex("[\\s\\p{Punct}]+")

    suspend fun fetchGoogleSuggestions(query: String, lang: String = "en", maxResults: Int = 4): List<WordSuggestion> {
        val trimmed = query.trim()
        if (trimmed.length < 2 || trimmed.contains(" ")) return emptyList()

        val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
        val request = Request.Builder()
            .url("https://suggestqueries.google.com/complete/search?client=firefox&hl=$lang&q=$encodedQuery")
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resume(emptyList())
                }

                override fun onResponse(call: Call, response: Response) {
                    val results = try {
                        response.use {
                            if (it.isSuccessful) {
                                parseSuggestions(it.body?.string().orEmpty(), trimmed, maxResults)
                            } else emptyList()
                        }
                    } catch (_: IOException) {
                        emptyList()
                    }
                    if (continuation.isActive) continuation.resume(results)
                }
            })
        }
    }

    private fun parseSuggestions(responseText: String, query: String, maxResults: Int): List<WordSuggestion> {
        return try {
            val rootArray = JSONArray(responseText)
            if (rootArray.length() < 2) return emptyList()

            val wordSet = linkedSetOf<String>()
            val suggestionsArray = rootArray.getJSONArray(1)
            for (index in 0 until suggestionsArray.length()) {
                val candidate = suggestionsArray.getString(index).trim()
                val word = wordBoundary.split(candidate).firstOrNull { it.isNotBlank() } ?: continue
                if (word.length >= query.length && word.startsWith(query, ignoreCase = true)) {
                    wordSet.add(word)
                    if (wordSet.size >= maxResults) break
                }
            }
            wordSet.map { word ->
                WordSuggestion(word = word, isAutoCorrect = word.equals(query, ignoreCase = true))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
