package com.nervesparks.iris.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.LocalActionHandler
import com.nervesparks.iris.ui.components.*
import com.nervesparks.iris.ui.navigation.AppDestinations

/**
 * Test screen to showcase all modern components
 */
@Composable
fun ModernTestScreen(
    navController: NavController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var showModelDropdown by remember { mutableStateOf(false) }

    val availableModels = listOf(
        "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
        "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
        "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
        "NemoTron-1.5B-Q4_K_M.gguf",
        "Qwen_Qwen3-0.6B-Q4_K_M.gguf"
    )

    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)
    val actionHandler = remember { LocalActionHandler(context) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                messageText = spokenText
            }
        }
    }

    LaunchedEffect(viewModel.lastAttachmentAction) {
        viewModel.lastAttachmentAction?.let { actionHandler.handleAttachmentAction(it, viewModel) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern top app bar
        ModernTopAppBar(
            title = "IRIS Star",
            onMenuClick = { navController.navigate(AppDestinations.CHAT_LIST) },
            onModelClick = { showModelDropdown = !showModelDropdown },
            currentModel = "No Model Selected",
            availableModels = availableModels,
            showModelDropdown = showModelDropdown,
            onModelDropdownDismiss = { showModelDropdown = false },
            viewModel = viewModel,
            extFilesDir = extFilesDir
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
                    onProfileClick = {
                        navController.navigate(AppDestinations.SETTINGS)
                    },
                    onSettingsClick = {
                        navController.navigate(AppDestinations.SETTINGS)
                    }
                )
            }
        }

        // Modern bottom input
        ModernChatInput(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                viewModel.updateMessage(messageText)
                viewModel.send()
                messageText = ""
            },
            onAttachmentClick = { viewModel.onFilesAttachment() },
            onVoiceClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                }
                speechLauncher.launch(intent)
            },
            onCameraClick = { viewModel.onCameraAttachment() },
            onPhotosClick = { viewModel.onPhotosAttachment() },
            onFilesClick = { viewModel.onFilesAttachment() },
            onCodeClick = { viewModel.toggleCodeMode() },
            isCodeMode = viewModel.isCodeMode,
            onTranslateClick = { viewModel.translate(messageText, "English") }
        )
    }
}