package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.ui.theme.ComponentStyles

@Composable
fun MessageBubble(
    message: String,
    isUser: Boolean,
    timestamp: Long? = null,
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
            Card(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clickable { onLongClick?.invoke() },
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = RoundedCornerShape(
                    topStart = ComponentStyles.extraLargeCardShape.topStart,
                    topEnd = ComponentStyles.extraLargeCardShape.topEnd,
                    bottomStart = if (isUser) ComponentStyles.smallCardShape.bottomStart else ComponentStyles.extraLargeCardShape.bottomStart,
                    bottomEnd = if (isUser) ComponentStyles.extraLargeCardShape.bottomEnd else ComponentStyles.smallCardShape.bottomEnd
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.smallElevation)
            ) {
                Column(
                    modifier = Modifier.padding(ComponentStyles.defaultPadding)
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