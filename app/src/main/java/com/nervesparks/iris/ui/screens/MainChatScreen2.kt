package com.nervesparks.iris.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import com.nervesparks.iris.ui.navigation.AppDestinations
import kotlinx.coroutines.launch

@Composable
fun NavDrawer(
    navController: NavController,
    onCloseDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "IRIS",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = {
            onNewChat()
            onCloseDrawer()
        }) {
            Text("New Chat")
        }
        Button(onClick = {
            onSettings()
            onCloseDrawer()
        }) {
            Text("Settings")
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
                }
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
                }
            },
            bottomBar = {
                Column {
                    PerformanceMonitor(viewModel = viewModel)
                    ModernChatInput(
                        value = viewModel.message,
                        onValueChange = { viewModel.updateMessage(it) },
                        onSend = { viewModel.send() },
                        onAttachmentClick = {},
                        onVoiceClick = {},
                        onCameraClick = { viewModel.onCameraAttachment() },
                        onPhotosClick = { viewModel.onPhotosAttachment() },
                        onFilesClick = { viewModel.onFilesAttachment() },
                        onCodeClick = { viewModel.toggleCodeMode() },
                        isCodeMode = viewModel.isCodeMode,
                        onTranslateClick = { viewModel.translate(viewModel.message, "English") }
                    )
                }
            }
        )
    }
}