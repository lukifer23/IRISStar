package com.nervesparks.iris.ui.screens

import android.annotation.SuppressLint
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nervesparks.iris.ui.components.PerChatSettingsBottomSheet
import com.nervesparks.iris.viewmodel.ChatViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.viewmodel.ModelViewModel
import com.nervesparks.iris.ui.LocalActionHandler
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
    viewModel: MainViewModel,
    modelViewModel: ModelViewModel,
    generationViewModel: com.nervesparks.iris.viewmodel.GenerationViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)
    val actionHandler = remember { LocalActionHandler(context) }
    val chatViewModel: ChatViewModel = viewModel()

    var showModelDropdown by remember { mutableStateOf(false) }
    var showPerChatSettings by remember { mutableStateOf(false) }
    var currentChatSettings by remember { mutableStateOf<com.nervesparks.iris.viewmodel.ChatSettings>(com.nervesparks.iris.viewmodel.ChatSettings()) }

    LaunchedEffect(viewModel.lastQuickAction) {
        viewModel.lastQuickAction?.let {
            actionHandler.handleQuickAction(it, viewModel)
        }
    }

    LaunchedEffect(viewModel.lastAttachmentAction) {
        viewModel.lastAttachmentAction?.let {
            actionHandler.handleAttachmentAction(it, viewModel)
        }
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
                    viewModel.persistChat() // Save current chat first
                    viewModel.clear()
                    navController.navigate(AppDestinations.CHAT) {
                        // Pop up to chat list to avoid stacking CHAT destinations
                        popUpTo(AppDestinations.CHAT_LIST) { inclusive = false }
                    }
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
                    title = "Iris",
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onModelClick = { showModelDropdown = true },
                    currentModel = viewModel.loadedModelName.value,
                    availableModels = modelViewModel.availableModels.map { it["name"] ?: "" },
                    showModelDropdown = showModelDropdown,
                    onModelDropdownDismiss = { showModelDropdown = false },
                    viewModel = viewModel,
                    extFilesDir = extFilesDir,
                    onSettingsClick = {
                        scope.launch {
                            // For now, just show the settings dialog
                            currentChatSettings = com.nervesparks.iris.viewmodel.ChatSettings() // Default for now
                            showPerChatSettings = true
                        }
                    }
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
                            modelViewModel = modelViewModel,
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
                    PerformanceMonitor(generationViewModel = generationViewModel, mainViewModel = viewModel)
                    ModernChatInput(
                        value = viewModel.message,
                        onValueChange = { viewModel.updateMessage(it) },
                        onSend = { viewModel.send() },
                        onAttachmentClick = {},
                        onVoiceClick = { viewModel.startVoiceRecognition(context) },
                        onCameraClick = { viewModel.onCameraAttachment() },
                        onPhotosClick = { viewModel.onPhotosAttachment() },
                        onFilesClick = { viewModel.onFilesAttachment() },
                        onCodeClick = { viewModel.toggleCodeMode() },
                        isCodeMode = viewModel.isCodeMode,
                        onTranslateClick = { viewModel.translate(viewModel.message, "English") },
                        onWebSearchClick = { 
                            if (viewModel.message.isNotBlank()) {
                                viewModel.performWebSearch(viewModel.message)
                            }
                        },
                        enabled = !generationViewModel.isGenerating
                    )
                }
            }
        )

        // Per-chat settings dialog
        if (showPerChatSettings) {
            PerChatSettingsBottomSheet(
                chatViewModel = chatViewModel,
                chatId = viewModel.currentChatPublic?.id ?: 1L,
                currentSettings = currentChatSettings,
                onDismiss = { showPerChatSettings = false },
                onSave = { settings ->
                    currentChatSettings = settings
                    showPerChatSettings = false
                }
            )
        }
    }
}