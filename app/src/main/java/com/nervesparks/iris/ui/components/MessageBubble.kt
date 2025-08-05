package com.nervesparks.iris.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.model.ChatMessage

@Composable
fun MessageBubble(
    message: ChatMessage,
    viewModel: MainViewModel? = null,
    showThinkingTokens: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    when (message) {
        is ChatMessage.User -> {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .combinedClickable(
                            onLongClick = { onLongClick?.invoke() },
                            onClick = {}
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = 22.sp
                    )

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = Color.White
                        )
                    }
                }

                MessageIcon(iconRes = R.drawable.user_icon, description = "User Icon")
            }
        }
        is ChatMessage.Assistant -> {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                MessageIcon(iconRes = R.drawable.logo, description = "Bot Icon")

                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .combinedClickable(
                            onLongClick = { onLongClick?.invoke() },
                            onClick = {}
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = message.content.removePrefix("```") ,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content.removePrefix("```")))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        is ChatMessage.System -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .combinedClickable(
                        onLongClick = { onLongClick?.invoke() },
                        onClick = {}
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy system message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        is ChatMessage.Code -> {
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content.removePrefix("```")))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = message.content.removePrefix("```"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        is ChatMessage.Reasoning -> {
            if (viewModel != null) {
                ThinkingMessage(
                    message = message.content,
                    viewModel = viewModel,
                    showThinkingTokens = showThinkingTokens,
                    onLongClick = { onLongClick?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun MessageIcon(iconRes: Int, description: String) {
    Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = description,
        modifier = Modifier.size(20.dp)
    )
}

