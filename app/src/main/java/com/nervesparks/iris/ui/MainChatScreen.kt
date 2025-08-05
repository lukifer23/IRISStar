package com.nervesparks.iris.ui

import android.app.DownloadManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.ChatMessageList
import com.nervesparks.iris.ui.components.DownloadModal
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.ModelSelectionModal
import com.nervesparks.iris.ui.components.ModelSettingsScreen
import com.nervesparks.iris.ui.components.ModelStatusCard
import com.nervesparks.iris.ui.components.ModernChatInput
import com.nervesparks.iris.ui.components.PromptList
import com.nervesparks.iris.ui.components.rememberSpeechRecognizer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    viewModel: MainViewModel,
    dm: DownloadManager,
    models: List<Downloadable>,
    extFileDir: File?,
    chatId: Long? = null,
    onBackPressed: () -> Unit = {},
    onNavigateToModels: () -> Unit = {}
) {
    val kc = LocalSoftwareKeyboardController.current
    val scrollState = rememberLazyListState()
    val startSpeech = rememberSpeechRecognizer(viewModel)

    LaunchedEffect(chatId) {
        if (chatId != null) {
            viewModel.loadChat(chatId)
        } else {
            viewModel.clear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
    ) {
        ModelStatusCard(viewModel = viewModel, onBackPressed = onBackPressed)

        if (viewModel.showModal) {
            DownloadModal(viewModel = viewModel, dm = dm, models = models)
        }
        if (viewModel.showModelSelection) {
            ModelSelectionModal(
                viewModel = viewModel,
                onDismiss = { viewModel.hideModelSelectionDialog() },
                onNavigateToModels = onNavigateToModels
            )
        }
        if (viewModel.showAlert) {
            LoadingModal(viewModel)
        }
        if (viewModel.showModelSettings) {
            ModelSettingsScreen(
                viewModel = viewModel,
                onBackPressed = { viewModel.hideModelSettings() }
            )
        }

        ChatMessageList(viewModel = viewModel, scrollState = scrollState)

        PromptList(viewModel = viewModel) { prompt ->
            viewModel.updateMessage(prompt)
        }

        ModernChatInput(
            value = viewModel.message,
            onValueChange = viewModel::updateMessage,
            onSend = {
                kc?.hide()
                viewModel.send()
            },
            onAttachmentClick = {},
            onVoiceClick = {
                kc?.hide()
                startSpeech()
            },
            onLatestNews = viewModel::onLatestNews,
            onCreateImages = viewModel::onCreateImages,
            onCartoonStyle = viewModel::onCartoonStyle,
            onCameraClick = viewModel::onCameraAttachment,
            onPhotosClick = viewModel::onPhotosAttachment,
            onFilesClick = viewModel::onFilesAttachment,
            enabled = !viewModel.getIsSending()
        )
    }
}

