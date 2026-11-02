package com.example.ui.navigation

sealed class NavRoute(val route: String) {
    object Home : NavRoute("home")
    object Create : NavRoute("create")
    object Progress : NavRoute("progress/{jobId}") {
        fun createRoute(jobId: String) = "progress/$jobId"
    }
    object Preview : NavRoute("preview/{videoUrl}/{title}") {
        fun createRoute(videoUrl: String, title: String): String {
            val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "preview/$encodedUrl/$encodedTitle"
        }
    }
    object Library : NavRoute("library")
    object Details : NavRoute("details/{projectId}") {
        fun createRoute(projectId: String) = "details/$projectId"
    }
    object Settings : NavRoute("settings")
    object Logs : NavRoute("logs")
    object KeyboardPlayground : NavRoute("keyboard_playground")
    object VpnShield : NavRoute("vpn_shield")
    object Recovery : NavRoute("recovery")
    object CreatorAssistant : NavRoute("creator_assistant")
    object Location : NavRoute("location")
}
