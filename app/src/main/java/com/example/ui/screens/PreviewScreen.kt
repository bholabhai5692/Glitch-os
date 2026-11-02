package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeonButton
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodels.MainViewModel
import com.example.util.SocialShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    videoUrl: String,
    title: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val decodedUrl = remember(videoUrl) {
        runCatching { java.net.URLDecoder.decode(videoUrl, "UTF-8") }.getOrDefault(videoUrl)
    }
    val decodedTitle = remember(title) {
        runCatching { java.net.URLDecoder.decode(title, "UTF-8") }.getOrDefault(title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = decodedTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("preview_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        SocialShareHelper.shareVideo(context, decodedUrl, decodedTitle, "Made with Glitch OS")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share video", tint = CyberCyan)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VideoPlayerView(
                videoUri = Uri.parse(decodedUrl),
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(
                    text = "Share video",
                    color = NeonGreen,
                    onClick = {
                        SocialShareHelper.shareVideo(context, decodedUrl, decodedTitle, "Made with Glitch OS")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
