package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    viewModel: MainViewModel,
    onBackPressed: () -> Unit
) {
    var temperature by remember { mutableStateOf(0.7f) }
    var topP by remember { mutableStateOf(0.9f) }
    var topK by remember { mutableStateOf(40) }
    var maxTokens by remember { mutableStateOf(2048) }
    var contextLength by remember { mutableStateOf(4096) }
    var systemPrompt by remember { mutableStateOf("You are a helpful AI assistant.") }
    var selectedChatFormat by remember { mutableStateOf(viewModel.modelChatFormat) }
    var threadCount by remember { mutableStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a0a0a))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Model Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Temperature Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Temperature: ${String.format("%.2f", temperature)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Controls randomness (0.0 = deterministic, 1.0 = very random)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..2.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4CAF50),
                        activeTrackColor = Color(0xFF4CAF50),
                        inactiveTrackColor = Color(0xFF2E7D32)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Top-p Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Top-p: ${String.format("%.2f", topP)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Controls diversity via nucleus sampling",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = topP,
                    onValueChange = { topP = it },
                    valueRange = 0.0f..1.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3),
                        inactiveTrackColor = Color(0xFF1976D2)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Top-k Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Top-k: $topK",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Limits vocabulary to top k tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = topK.toFloat(),
                    onValueChange = { topK = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 98,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF9800),
                        activeTrackColor = Color(0xFFFF9800),
                        inactiveTrackColor = Color(0xFFF57C00)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Max Tokens
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Max Tokens: $maxTokens",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Maximum tokens to generate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.toInt() },
                    valueRange = 64f..4096f,
                    steps = 63,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE91E63),
                        activeTrackColor = Color(0xFFE91E63),
                        inactiveTrackColor = Color(0xFFC2185B)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Context Length
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Context Length: $contextLength",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Maximum context window size",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = contextLength.toFloat(),
                    onValueChange = { contextLength = it.toInt() },
                    valueRange = 512f..32768f,  // Increased to support Qwen3's 32k context
                    steps = 31,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF9C27B0),
                        activeTrackColor = Color(0xFF9C27B0),
                        inactiveTrackColor = Color(0xFF7B1FA2)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Thread Count
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Thread Count: $threadCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Number of CPU threads to use",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = threadCount.toFloat(),
                    onValueChange = { threadCount = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00BCD4),
                        activeTrackColor = Color(0xFF00BCD4),
                        inactiveTrackColor = Color(0xFF0097A7)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chat Format Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Chat Format",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Select the chat format for the model",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val chatFormats = listOf("QWEN3", "CHATML", "ALPACA", "VICUNA", "LLAMA2", "ZEPHYR")
                chatFormats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedChatFormat == format,
                            onClick = { selectedChatFormat = format },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF4CAF50),
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Text(
                            text = format,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // System Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Prompt",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Define the AI's behavior and capabilities",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    minLines = 3,
                    maxLines = 5
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Thinking Token Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Reasoning/Thinking Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Configure how reasoning models display thinking tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show thinking tokens toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show thinking tokens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = viewModel.showThinkingTokens,
                        onCheckedChange = { viewModel.updateShowThinkingTokens(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00BCD4),
                            checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Thinking token style selection
                Text(
                    text = "Thinking display style:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val thinkingStyles = listOf("COLLAPSIBLE", "ALWAYS_VISIBLE", "HIDDEN")
                thinkingStyles.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.thinkingTokenStyle == style,
                            onClick = { viewModel.updateThinkingTokenStyle(style) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF00BCD4),
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Text(
                            text = when (style) {
                                "COLLAPSIBLE" -> "Collapsible (default)"
                                "ALWAYS_VISIBLE" -> "Always visible"
                                "HIDDEN" -> "Hidden"
                                else -> style
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Save Button
        Button(
            onClick = {
                // Save settings to ViewModel
                viewModel.updateModelSettings(
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    maxTokens = maxTokens,
                    contextLength = contextLength,
                    systemPrompt = systemPrompt,
                    chatFormat = selectedChatFormat,
                    threadCount = threadCount
                )
                onBackPressed()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Save Settings", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reset to Defaults Button
        OutlinedButton(
            onClick = {
                temperature = 0.7f
                topP = 0.9f
                topK = 40
                maxTokens = 2048
                contextLength = 4096
                systemPrompt = "You are a helpful AI assistant."
                selectedChatFormat = "QWEN3"
                threadCount = 4
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                )
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Reset to Defaults", style = MaterialTheme.typography.titleMedium)
        }
    }
} 