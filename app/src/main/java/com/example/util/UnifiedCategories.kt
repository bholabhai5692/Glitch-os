package com.example.util

/**
 * Unified single source of truth for all content categories across:
 * 1. Settings (Content Category Niche & AI Agent Profile)
 * 2. Video generation categories
 * 3. Main Screen (Home Auto-Pilot & Scheduler)
 */
object UnifiedCategories {
    val ALL = listOf(
        "Technology",
        "AI & Futuristic",
        "Science & Facts",
        "Business & Finance",
        "Gaming",
        "Entertainment",
        "Lifestyle & Vlogging",
        "Motivation & Fitness",
        "Horror & Thriller",
        "Mystery & Crime",
        "History & Culture"
    )

    fun getCategoryId(category: String): String {
        return when (category.trim().lowercase()) {
            "technology", "ai & futuristic", "tech & education" -> "28"
            "science & facts", "science" -> "28"
            "gaming" -> "20"
            "entertainment", "horror & thriller", "mystery & crime" -> "24"
            "business & finance", "business & news" -> "25"
            "motivation & fitness", "lifestyle & vlogging" -> "22"
            "history & culture" -> "27"
            else -> "28"
        }
    }
}
