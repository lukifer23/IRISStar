package com.nervesparks.iris.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.*
import com.nervesparks.iris.ui.theme.IrisStarTheme
import java.io.File

/**
 * Test screen to showcase all modern components
 */
@Composable
fun ModernTestScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var showModelDropdown by remember { mutableStateOf(false) }
    
    val availableModels = listOf("Llama-3.2-3B-Instruct-Q4_K_L.gguf", "Llama-3.2-1B-Instruct-Q6_K_L.gguf", "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf", "NemoTron-1.5B-Q4_K_M.gguf", "Qwen_Qwen3-0.6B-Q4_K_M.gguf")
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern top app bar
        ModernTopAppBar(
            title = "IRIS Star",
            onMenuClick = { /* TODO: Open menu */ },
            onModelClick = { showModelDropdown = !showModelDropdown },
            currentModel = "No Model Selected",
            availableModels = availableModels,
            showModelDropdown = showModelDropdown,
            onModelDropdownDismiss = { showModelDropdown = false },
            viewModel = viewModel,
            extFilesDir = null
        )
        
        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Modern UI Components",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Testing our new design system",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // User profile component
                UserProfile(
                    onProfileClick = { /* TODO: Edit profile */ },
                    onSettingsClick = { /* TODO: Open settings */ }
                )
            }
        }
        
        // Modern bottom input
        ModernChatInput(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                // TODO: Send message
                messageText = ""
            },
            onAttachmentClick = { /* TODO: Implement attachments */ },
            onVoiceClick = { /* TODO: Implement voice input */ },
            onLatestNews = { viewModel.onLatestNews() },
            onCreateImages = { viewModel.onCreateImages() },
            onCartoonStyle = { viewModel.onCartoonStyle() },
            onCameraClick = { viewModel.onCameraAttachment() },
            onPhotosClick = { viewModel.onPhotosAttachment() },
            onFilesClick = { viewModel.onFilesAttachment() }
        )
    }
}