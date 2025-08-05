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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Model Configuration",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingSlider(
            label = "Temperature",
            value = temperature,
            onValueChange = { temperature = it },
            valueRange = 0.0f..2.0f,
            steps = 19,
            infoText = "Controls randomness (0.0 = deterministic, 1.0 = very random)",
            valueFormatter = { String.format("%.2f", it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingSlider(
            label = "Top-p",
            value = topP,
            onValueChange = { topP = it },
            valueRange = 0.0f..1.0f,
            steps = 19,
            infoText = "Controls diversity via nucleus sampling",
            valueFormatter = { String.format("%.2f", it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingSlider(
            label = "Top-k",
            value = topK.toFloat(),
            onValueChange = { topK = it.toInt() },
            valueRange = 1f..100f,
            steps = 98,
            infoText = "Limits vocabulary to top k tokens",
            valueFormatter = { it.toInt().toString() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingSlider(
            label = "Max Tokens",
            value = maxTokens.toFloat(),
            onValueChange = { maxTokens = it.toInt() },
            valueRange = 1f..8192f,
            steps = 8190,
            infoText = "Maximum tokens to generate",
            valueFormatter = { it.toInt().toString() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingSlider(
            label = "Context Length",
            value = contextLength.toFloat(),
            onValueChange = { contextLength = it.toInt() },
            valueRange = 512f..32768f,
            steps = 32255,
            infoText = "Maximum context window size",
            valueFormatter = { it.toInt().toString() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingSlider(
            label = "Thread Count",
            value = threadCount.toFloat(),
            onValueChange = { threadCount = it.toInt() },
            valueRange = 1f..16f,
            steps = 14,
            infoText = "Number of CPU threads to use",
            valueFormatter = { it.toInt().toString() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // System Prompt Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Prompt",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Text(
                    text = "Initial system message for the model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSecondary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chat Format Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Chat Format",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Text(
                    text = "Select the chat format for the model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
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
                                selectedColor = MaterialTheme.colorScheme.tertiary,
                                unselectedColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                            )
                        )
                        Text(
                            text = format,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Apply Button
        Button(
            onClick = {
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("Apply Settings", color = MaterialTheme.colorScheme.onTertiary)
        }
    }
} 