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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nervesparks.iris.MainViewModel
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
import com.nervesparks.iris.ui.util.rememberWindowClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
    viewModel: MainViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)
    val actionHandler = remember { LocalActionHandler(context) }
    val windowClass = rememberWindowClass()

    var showModelDropdown by remember { mutableStateOf(false) }

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

    if (windowClass.width == WindowWidthSizeClass.Compact) {
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
            ChatScaffold(
                navController = navController,
                viewModel = viewModel,
                scrollState = scrollState,
                drawerState = drawerState,
                scope = scope,
                extFilesDir = extFilesDir,
                showModelDropdown = showModelDropdown,
                onShowModelDropdownChange = { showModelDropdown = it },
                context = context
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            NavDrawer(
                navController = navController,
                onCloseDrawer = {},
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
                viewModel = viewModel,
                modifier = Modifier.width(240.dp)
            )
            ChatScaffold(
                navController = navController,
                viewModel = viewModel,
                scrollState = scrollState,
                drawerState = drawerState,
                scope = scope,
                extFilesDir = extFilesDir,
                showModelDropdown = showModelDropdown,
                onShowModelDropdownChange = { showModelDropdown = it },
                context = context,
                modifier = Modifier.weight(1f)
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScaffold(
    navController: NavController,
    viewModel: MainViewModel,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    drawerState: androidx.compose.material3.DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    extFilesDir: java.io.File?,
    showModelDropdown: Boolean,
    onShowModelDropdownChange: (Boolean) -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            ModernTopAppBar(
                title = "Iris ✨",
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                onModelClick = { onShowModelDropdownChange(true) },
                currentModel = viewModel.loadedModelName.value,
                availableModels = viewModel.allModels.map { it["name"] ?: "" },
                showModelDropdown = showModelDropdown,
                onModelDropdownDismiss = { onShowModelDropdownChange(false) },
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

                if (viewModel.isSearching) {
                    SearchLoadingModal(
                        message = viewModel.searchProgress,
                        searchQuery = viewModel.currentSearchQuery,
                        onDismiss = { }
                    )
                }

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
                    enabled = !viewModel.isGenerating
                )
            }
        }
    )
}