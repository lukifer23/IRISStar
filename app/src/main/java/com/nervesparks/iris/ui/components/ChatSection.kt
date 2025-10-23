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
import androidx.compose.material.icons.filled.Check
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
import com.nervesparks.iris.ui.theme.ComponentStyles


@Composable
fun ChatMessageList(chatViewModel: com.nervesparks.iris.viewmodel.ChatViewModel, viewModel: MainViewModel, scrollState: LazyListState) {
    val messages = chatViewModel.messages
    val context = LocalContext.current

    // Memoize the message list to avoid recreating it on every recomposition
    val displayMessages = remember(messages) {
        messages.filter { message ->
            (message["role"] as? String) != "system"
        }
    }

    // Memoize supportsReasoning to avoid unnecessary recompositions
    val supportsReasoning = chatViewModel.supportsReasoning

    LazyColumn(state = scrollState) {
        itemsIndexed(
            items = displayMessages,
            key = { idx: Int, messageMap: Map<String, Any> ->
                // Use a stable key based on content hash to avoid unnecessary recompositions
                val role = (messageMap["role"] as? String) ?: ""
                val content = (messageMap["content"] as? String) ?: ""
                "$idx-$role-${content.hashCode()}"
            },
            contentType = { _: Int, messageMap: Map<String, Any> ->
                // Use content type for better item reuse
                (messageMap["role"] as? String) ?: "unknown"
            }
        ) { idx: Int, messageMap: Map<String, Any> ->
            val role = (messageMap["role"] as? String) ?: ""
            val content = ((messageMap["content"] as? String) ?: "").trimEnd()
            if (role != "system") {
                when (role) {
                    "codeBlock" -> CodeBlockMessage(content)
                    "assistant" -> {
                        // Memoize the expensive parsing operation
                        val parsedContent = remember(content, supportsReasoning) {
                            ReasoningParser.parse(content, supportsReasoning)
                        }
                        val (reasoningContent, _) = parsedContent
                        if (supportsReasoning && reasoningContent.isNotEmpty()) {
                            ThinkingMessage(
                                message = content,
                                chatViewModel = chatViewModel,
                                showThinkingTokens = chatViewModel.showThinkingTokens,
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
                            // Memoize the grouping calculations to avoid expensive lookups
                            // Note: idx here corresponds to the filtered displayMessages list
                            val groupingInfo = remember(displayMessages, idx, role) {
                                val prevRole = displayMessages.getOrNull(idx - 1)?.get("role")
                                val nextRole = displayMessages.getOrNull(idx + 1)?.get("role")
                                Pair(prevRole == role, nextRole == role)
                            }
                            val (groupedPrev, groupedNext) = groupingInfo
                            MessageBubble(
                                message = content,
                                isUser = false,
                                isGroupedPrev = groupedPrev,
                                isGroupedNext = groupedNext,
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
                    else -> {
                        val prevRole = displayMessages.getOrNull(idx - 1)?.get("role")
                        val nextRole = displayMessages.getOrNull(idx + 1)?.get("role")
                        val groupedPrev = prevRole == role
                        val groupedNext = nextRole == role
                        MessageBubble(
                            message = content,
                            isUser = true,
                            isGroupedPrev = groupedPrev,
                            isGroupedNext = groupedNext,
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
        }
        item { Spacer(modifier = Modifier.height(ComponentStyles.smallPadding).fillMaxWidth()) }
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
            .padding(ComponentStyles.smallPadding)
    ) {
        if (role == "assistant") MessageIcon(iconRes = R.drawable.logo, description = "Bot Icon")

        Box(
            modifier = Modifier
                .padding(horizontal = ComponentStyles.smallPadding)
                .background(
                    color = if (role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = ComponentStyles.pillShape
                )
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {}
                )
                .padding(ComponentStyles.defaultPadding)
        ) {
            Text(
                text = message.removePrefix("```"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.removePrefix("```")))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(ComponentStyles.defaultIconSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Copy message",
                    tint = if (role == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(horizontal = ComponentStyles.defaultSpacing, vertical = ComponentStyles.smallPadding)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = ComponentStyles.smallCardShape)
            .fillMaxWidth()
    ) {
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(content.removePrefix("```")))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.align(Alignment.TopEnd).size(ComponentStyles.defaultIconSize)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
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
            modifier = Modifier.padding(ComponentStyles.defaultPadding)
        )
    }
}

@Composable
private fun MessageIcon(iconRes: Int, description: String) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = description,
        modifier = Modifier.size(ComponentStyles.defaultIconSize)
    )
}

