package com.nervesparks.iris.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.nervesparks.iris.ui.components.PerformanceMonitor
import com.nervesparks.iris.ui.components.ThinkingMessage
import com.nervesparks.iris.llm.ReasoningParser


@Composable
fun ChatMessageList(viewModel: MainViewModel, scrollState: LazyListState) {
    val messages = viewModel.messages
    val context = LocalContext.current

    Column {
        
        LazyColumn(state = scrollState) {
            // Add performance monitor at the top
            item {
                if (viewModel.showPerformanceMonitor) {
                    PerformanceMonitor(viewModel = viewModel)
                }
            }
            
            itemsIndexed(messages.drop(3)) { index, messageMap ->
                val role = messageMap["role"] ?: ""
                val content = (messageMap["content"] ?: "").trimEnd()

                if (role != "system") {
                    when (role) {
                        "codeBlock" -> CodeBlockMessage(content)
                        "assistant" -> {
                            val (reasoning, _) = ReasoningParser.parse(content)

                            if (reasoning.isNotEmpty()) {
                                ThinkingMessage(
                                    message = content,
                                    viewModel = viewModel,
                                    showThinkingTokens = viewModel.showThinkingTokens,
                                    onLongClick = {
                                        if (viewModel.getIsSending()) {
                                            Toast.makeText(
                                                context,
                                                "Wait till generation is done!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            viewModel.toggler = true
                                        }
                                    }
                                )
                            } else {
                                UserOrAssistantMessage(
                                    role = role,
                                    message = content,
                                    onLongClick = {
                                        if (viewModel.getIsSending()) {
                                            Toast.makeText(
                                                context,
                                                "Wait till generation is done!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            viewModel.toggler = true
                                        }
                                    }
                                )
                            }
                        }
                        else -> UserOrAssistantMessage(
                            role = role,
                            message = content,
                            onLongClick = {
                                if (viewModel.getIsSending()) {
                                    Toast.makeText(
                                        context,
                                        "Wait till generation is done!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewModel.toggler = true
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(1.dp).fillMaxWidth()) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserOrAssistantMessage(role: String, message: String, onLongClick: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (role == "assistant") MessageIcon(iconRes = R.drawable.logo, description = "Bot Icon")

        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .background(
                    color = if (role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {}
                )
                .padding(16.dp)
        ) {
            Text(
                text = message.removePrefix("```"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.removePrefix("```")))
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

        if (role == "user") MessageIcon(iconRes = R.drawable.user_icon, description = "User Icon")
    }
}

@Composable
private fun CodeBlockMessage(content: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
            .fillMaxWidth()
    ) {
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(content.removePrefix("```")))
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
            text = content.removePrefix("```"),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun MessageIcon(iconRes: Int, description: String) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = description,
        modifier = Modifier.size(20.dp)
    )
}

