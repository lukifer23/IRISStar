package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.nervesparks.iris.viewmodel.ChatSettings
import com.nervesparks.iris.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerChatSettingsBottomSheet(
    chatViewModel: ChatViewModel,
    chatId: Long?,
    currentSettings: ChatSettings,
    onDismiss: () -> Unit,
    onSave: (ChatSettings) -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hasValidChatId = chatId != null && chatId > 0L

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(600.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (!hasValidChatId) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text("Create or open a chat to edit per-chat settings.")
                        },
                        enabled = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Model Selection
                OutlinedTextField(
                    value = settings.modelName ?: "",
                    onValueChange = { settings = settings.copy(modelName = it.ifBlank { null }) },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature
                OutlinedTextField(
                    value = settings.temperature?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(temperature = it.toFloatOrNull())
                    },
                    label = { Text("Temperature (0.0-2.0)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top P
                OutlinedTextField(
                    value = settings.topP?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(topP = it.toFloatOrNull())
                    },
                    label = { Text("Top P (0.0-1.0)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top K
                OutlinedTextField(
                    value = settings.topK?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(topK = it.toIntOrNull())
                    },
                    label = { Text("Top K (1-1000)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Max Tokens
                OutlinedTextField(
                    value = settings.maxTokens?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(maxTokens = it.toIntOrNull())
                    },
                    label = { Text("Max Tokens (1-32768)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Context Length
                OutlinedTextField(
                    value = settings.contextLength?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(contextLength = it.toIntOrNull())
                    },
                    label = { Text("Context Length (512-131072)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // System Prompt
                OutlinedTextField(
                    value = settings.systemPrompt ?: "",
                    onValueChange = { settings = settings.copy(systemPrompt = it.ifBlank { null }) },
                    label = { Text("System Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chat Format
                OutlinedTextField(
                    value = settings.chatFormat ?: "",
                    onValueChange = { settings = settings.copy(chatFormat = it.ifBlank { null }) },
                    label = { Text("Chat Format") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("CHATML, QWEN3, etc.") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Thread Count
                OutlinedTextField(
                    value = settings.threadCount?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(threadCount = it.toIntOrNull())
                    },
                    label = { Text("Thread Count (1-32)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GPU Layers
                OutlinedTextField(
                    value = settings.gpuLayers?.toString() ?: "",
                    onValueChange = {
                        settings = settings.copy(gpuLayers = it.toIntOrNull())
                    },
                    label = { Text("GPU Layers (-1 for auto)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Backend
                OutlinedTextField(
                    value = settings.backend ?: "",
                    onValueChange = { settings = settings.copy(backend = it.ifBlank { null }) },
                    label = { Text("Backend") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("cpu, vulkan, opencl") }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (!hasValidChatId) {
                                return@Button
                            }
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    chatViewModel.updateChatSettings(
                                        chatId = chatId,
                                        modelName = settings.modelName,
                                        temperature = settings.temperature,
                                        topP = settings.topP,
                                        topK = settings.topK,
                                        maxTokens = settings.maxTokens,
                                        contextLength = settings.contextLength,
                                        systemPrompt = settings.systemPrompt,
                                        chatFormat = settings.chatFormat,
                                        threadCount = settings.threadCount,
                                        gpuLayers = settings.gpuLayers,
                                        backend = settings.backend
                                    )
                                    onSave(settings)
                                    onDismiss()
                                } catch (e: Exception) {
                                    // Error already handled in ViewModel
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && hasValidChatId
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Settings")
                        }
                    }
                }
            }
        }
    }
}
