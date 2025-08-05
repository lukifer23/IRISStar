package com.nervesparks.iris.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.PerformanceMonitor
import com.nervesparks.iris.ui.model.ChatMessage
import com.nervesparks.iris.llm.ReasoningParser

@Composable
fun ChatMessageList(viewModel: MainViewModel, scrollState: LazyListState) {
    val messages = viewModel.messages
    val context = LocalContext.current

    Column {
        LazyColumn(state = scrollState) {
            item { PerformanceMonitor(viewModel = viewModel) }

            itemsIndexed(messages.drop(3)) { _, messageMap ->
                val role = messageMap["role"] ?: ""
                val content = (messageMap["content"] ?: "").trimEnd()

                val chatMessage = when (role) {
                    "user" -> ChatMessage.User(content)
                    "assistant" -> {
                        val (reasoning, _) = ReasoningParser.parse(content)
                        if (reasoning.isNotEmpty()) ChatMessage.Reasoning(content) else ChatMessage.Assistant(content)
                    }
                    "system" -> ChatMessage.System(content)
                    "codeBlock" -> ChatMessage.Code(content)
                    else -> ChatMessage.Assistant(content)
                }

                MessageBubble(
                    message = chatMessage,
                    viewModel = viewModel,
                    showThinkingTokens = viewModel.showThinkingTokens,
                    onLongClick = {
                        if (viewModel.getIsSending()) {
                            Toast.makeText(context, "Wait till generation is done!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.toggler = true
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(1.dp).fillMaxWidth()) }
        }
    }
}

