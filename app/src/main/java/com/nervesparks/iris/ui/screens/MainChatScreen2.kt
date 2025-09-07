package com.nervesparks.iris.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.ChatMessageList
import com.nervesparks.iris.ui.components.ModernChatInput
import com.nervesparks.iris.ui.components.ModernTopAppBar
import com.nervesparks.iris.ui.components.PerformanceMonitor
import com.nervesparks.iris.ui.components.SearchLoadingModal
import com.nervesparks.iris.ui.components.ModelSelectionModal
import com.nervesparks.iris.ui.navigation.AppDestinations
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.PrimaryButton
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

@Composable
fun NavDrawer(
    navController: NavController,
    onCloseDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit,
    onChatList: () -> Unit,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.padding(ComponentStyles.defaultPadding)) {
        Text(
            text = "IRIS",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = ComponentStyles.defaultPadding)
        )
        
        // New Chat
        PrimaryButton(
            onClick = {
                onNewChat()
                onCloseDrawer()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(ComponentStyles.defaultIconSize)
            )
            Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
            Text("New Chat")
        }
        
        Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
        
        // Chat List
        PrimaryButton(
            onClick = {
                onChatList()
                onCloseDrawer()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(ComponentStyles.defaultIconSize)
            )
            Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
            Text("Chat History")
        }
        
        Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
        
        // Settings
        PrimaryButton(
            onClick = {
                onSettings()
                onCloseDrawer()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(ComponentStyles.defaultIconSize)
            )
            Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
            Text("Settings")
        }
        
        Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
        
        // Current Model Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = ComponentStyles.smallCardShape
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding)
            ) {
                Text(
                    text = "Current Model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = viewModel.loadedModelName.value.ifEmpty { "No Model" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen2(
    navController: NavController,
    viewModel: MainViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)

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

    DisposableEffect(Unit) {
        onDispose {
            viewModel.persistChat()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavDrawer(
                navController = navController,
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onNewChat = {
                    viewModel.clear()
                    navController.navigate(AppDestinations.CHAT)
                },
                onSettings = {
                    navController.navigate(AppDestinations.SETTINGS)
                },
                onChatList = {
                    viewModel.persistChat()
                    navController.navigate(AppDestinations.CHAT_LIST)
                },
                viewModel = viewModel
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                ModernTopAppBar(
                    title = "Iris ✨",
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onModelClick = { showModelDropdown = true },
                    currentModel = viewModel.loadedModelName.value,
                    availableModels = viewModel.allModels.map { it["name"] ?: "" },
                    showModelDropdown = showModelDropdown,
                    onModelDropdownDismiss = { showModelDropdown = false },
                    viewModel = viewModel,
                    extFilesDir = extFilesDir
                )
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    ChatMessageList(viewModel = viewModel, scrollState = scrollState)
                    
                    // Show search loading modal when searching
                    if (viewModel.isSearching) {
                        SearchLoadingModal(
                            message = viewModel.searchProgress,
                            searchQuery = viewModel.currentSearchQuery,
                            onDismiss = { }
                        )
                    }
                    
                    // Show model selection modal when models are available
                    if (viewModel.showModelSelection) {
                        ModelSelectionModal(
                            viewModel = viewModel,
                            onDismiss = { viewModel.hideModelSelectionDialog() },
                            onNavigateToModels = { navController.navigate(AppDestinations.MODELS) }
                        )
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    PerformanceMonitor(viewModel = viewModel)
                    ModernChatInput(
                        value = viewModel.message,
                        onValueChange = { viewModel.updateMessage(it) },
                        onSend = { viewModel.send() },
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
                        onTranslateClick = { viewModel.translate(viewModel.message, "English") },
                        onWebSearchClick = {
                            if (viewModel.message.isNotBlank()) {
                                viewModel.performWebSearch(viewModel.message)
                            }
                        },
                        enabled = !viewModel.isGenerating
                    )
                }
            }
        )
    }
}