package com.nervesparks.iris.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
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
import androidx.core.content.ContextCompat
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.*
import com.nervesparks.iris.ui.theme.IrisStarTheme
import java.io.File
import java.io.FileOutputStream

/**
 * Test screen to showcase all modern components
 */
@Composable
fun ModernTestScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    var showModelDropdown by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceRecognition(context)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { fos ->
                    it.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                viewModel.sendImage(Uri.fromFile(file))
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.handleFile(context, it) }
    }
    
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
            onVoiceClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.startVoiceRecognition(context)
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onCameraClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onPhotosClick = {
                photoPickerLauncher.launch(arrayOf("image/*"))
            },
            onFilesClick = {
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onCodeClick = { viewModel.toggleCodeMode() },
            isCodeMode = viewModel.isCodeMode,
            onTranslateClick = { viewModel.translate(messageText, "English") }
        )
    }
}