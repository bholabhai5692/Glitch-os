package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class AppToolDestination(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FloatingToolLauncher(
    destinations: List<AppToolDestination>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { isOpen = true },
        modifier = modifier
            .testTag("floating_tool_launcher"),
        containerColor = NeonGreen,
        contentColor = DarkSurface
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = "Open app tools"
        )
    }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = { isOpen = false },
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "TOOLS",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 560.dp)) {
                    items(destinations, key = { it.route }) { destination ->
                        ListItem(
                            headlineContent = { Text(destination.title) },
                            leadingContent = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                    tint = NeonGreen
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isOpen = false
                                    onNavigate(destination.route)
                                },
                            colors = ListItemDefaults.colors(containerColor = DarkSurface)
                        )
                    }
                }
            }
        }
    }
}