package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object SocialShareHelper {

    /**
     * Prepares video URI safely using FileProvider for sharing.
     */
    fun getShareableUri(context: Context, videoPath: String): Uri? {
        if (videoPath.isBlank()) return null
        return try {
            val uri = Uri.parse(videoPath)
            if (uri.scheme == "content") {
                uri
            } else if (uri.scheme == "http" || uri.scheme == "https") {
                uri
            } else {
                val filePath = uri.path ?: videoPath
                val file = File(filePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    uri
                }
            }
        } catch (e: Exception) {
            try {
                val file = File(videoPath)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    Uri.parse(videoPath)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * General share intent launcher using FileProvider for secure video sharing across apps.
     */
    fun shareVideo(
        context: Context,
        videoPath: String,
        title: String = "",
        caption: String = "",
        hashtags: String = ""
    ) {
        val fullCaption = formatMetadataCaption(title, caption, hashtags)
        if (fullCaption.isNotBlank()) {
            copyToClipboard(context, fullCaption, "Video Title & Caption")
            Toast.makeText(
                context,
                "Title & Caption copied to clipboard! Long-press in text box to Paste.",
                Toast.LENGTH_LONG
            ).show()
        }

        val videoUri = getShareableUri(context, videoPath)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            if (videoUri != null) {
                putExtra(Intent.EXTRA_STREAM, videoUri)
                clipData = ClipData.newRawUri("video", videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (fullCaption.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, fullCaption)
                putExtra("caption", fullCaption)
            }
            if (title.isNotBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
            }
        }

        val chooser = Intent.createChooser(shareIntent, "Share Video").apply {
            if (videoUri != null) {
                clipData = ClipData.newRawUri("video", videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch share menu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats caption metadata with title, topic, hashtags.
     */
    fun formatMetadataCaption(title: String, caption: String = "", hashtags: String = ""): String {
        val sb = StringBuilder()
        if (title.isNotBlank()) {
            sb.append(title.trim())
        }
        if (caption.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(caption.trim())
        }
        if (hashtags.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(hashtags.trim())
        }
        return sb.toString().trim()
    }

    /**
     * Copies text to clipboard for easy paste in case app restricts auto-text injection.
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Reel Caption") {
        if (text.isNotBlank()) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText(label, text)
                clipboard?.setPrimaryClip(clip)
            } catch (_: Exception) {}
        }
    }

}
