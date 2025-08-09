package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.ui.theme.ComponentStyles

@Composable
fun MessageBubble(
    message: String,
    isUser: Boolean,
    timestamp: Long? = null,
    isGroupedPrev: Boolean = false,
    isGroupedNext: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentStyles.defaultPadding, vertical = ComponentStyles.smallPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            var menuExpanded by remember { mutableStateOf(false) }
            val clipboard = LocalClipboardManager.current

            val topStart = if (isGroupedPrev) 8 else 16
            val topEnd = if (isGroupedPrev) 8 else 16
            val bottomStart = if (isGroupedNext && !isUser) 8 else 16
            val bottomEnd = if (isGroupedNext && isUser) 8 else 16

            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
            Card(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            onLongClick?.invoke()
                            menuExpanded = true
                        }
                    ),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart),
                elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.smallElevation)
            ) {
                Column(
                    modifier = Modifier
                        .padding(ComponentStyles.defaultPadding)
                        .animateContentSize()
                ) {
                    Text(
                        text = message,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (timestamp != null) {
                        Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                        Text(
                            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(timestamp)),
                            color = textColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Copy") }, onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(message))
                    menuExpanded = false
                })
                DropdownMenuItem(text = { Text("Quote") }, onClick = {
                    // noop: placeholder hook for future quote action, menu closes
                    menuExpanded = false
                })
            }
        }
    }
}

@Composable
fun SystemMessageBubble(
    message: String,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentStyles.defaultPadding, vertical = ComponentStyles.smallPadding)
            .clickable { onLongClick?.invoke() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = ComponentStyles.defaultCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.smallElevation)
    ) {
        Row(
            modifier = Modifier.padding(ComponentStyles.defaultSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
} 