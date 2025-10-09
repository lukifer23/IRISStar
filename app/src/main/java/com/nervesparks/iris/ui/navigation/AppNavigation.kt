package com.nervesparks.iris.ui.navigation

import android.app.DownloadManager
import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nervesparks.iris.ui.theme.IrisAnimations
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.AboutScreen
import com.nervesparks.iris.ui.BenchMarkScreen
import com.nervesparks.iris.ui.ChatListScreen
import com.nervesparks.iris.ui.ModelsScreen
import com.nervesparks.iris.ui.ParametersScreen
import com.nervesparks.iris.ui.SettingsScreen
import com.nervesparks.iris.ui.ThemeSettingsScreen
import com.nervesparks.iris.ui.screens.MainChatScreen2
import com.nervesparks.iris.ui.ModelPerformanceScreen
import com.nervesparks.iris.data.UserPreferencesRepository
import java.io.File
import com.nervesparks.iris.viewmodel.ModelViewModel
import com.nervesparks.iris.viewmodel.ChatViewModel

object AppDestinations {
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val THEME_SETTINGS = "theme_settings"
    const val MODELS = "models"
    const val PARAMS = "params"
    const val ABOUT = "about"
    const val BENCHMARK = "benchmark"
    const val QUANTIZE = "quantize"
    const val TEMPLATES = "templates"
    const val MODEL_PERFORMANCE = "model_performance"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    modelViewModel: ModelViewModel,
    clipboardManager: ClipboardManager,
    downloadManager: DownloadManager,
    models: List<Downloadable>,
    extFilesDir: File?,
    preferencesRepository: UserPreferencesRepository
) {
    // Extract specialized ViewModels from MainViewModel
    val chatViewModel = viewModel.chatViewModel
    val documentViewModel = viewModel.documentViewModel
    val generationViewModel = viewModel.generationViewModel
    val toolViewModel = viewModel.toolViewModel
    val downloadViewModel = viewModel.downloadViewModel
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppDestinations.CHAT_LIST,
        enterTransition = { IrisAnimations.SlideInFromRight },
        exitTransition = { IrisAnimations.SlideOutToLeft },
        popEnterTransition = { IrisAnimations.SlideInFromRight },
        popExitTransition = { IrisAnimations.SlideOutToLeft }
    ) {
        composable(AppDestinations.CHAT_LIST) {
            ChatListScreen(
                viewModel = viewModel,
                modelViewModel = modelViewModel,
                onChatSelected = { chatId ->
                    navController.navigate("${AppDestinations.CHAT}?chatId=$chatId")
                },
                onNewChat = {
                    viewModel.clear()
                    navController.navigate(AppDestinations.CHAT)
                },
                onMenuClick = {
                    navController.navigate(AppDestinations.SETTINGS)
                }
            )
        }
        composable("${AppDestinations.CHAT}?chatId={chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull()
            if (chatId != null) {
                viewModel.loadChat(chatId)
            }
            MainChatScreen2(
                navController = navController,
                viewModel = viewModel,
                modelViewModel = modelViewModel,
                generationViewModel = generationViewModel
            )
        }
        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                generationViewModel = generationViewModel,
                preferencesRepository = preferencesRepository,
                onModelsScreenButtonClicked = { navController.navigate(AppDestinations.MODELS) },
                onParamsScreenButtonClicked = { navController.navigate(AppDestinations.PARAMS) },
                onAboutScreenButtonClicked = { navController.navigate(AppDestinations.ABOUT) },
                onBenchMarkScreenButtonClicked = { navController.navigate(AppDestinations.BENCHMARK) },
                onTemplatesScreenButtonClicked = { navController.navigate(AppDestinations.TEMPLATES) },
                onModelPerformanceScreenButtonClicked = { navController.navigate(AppDestinations.MODEL_PERFORMANCE) },
                onThemeSettingsClicked = { navController.navigate(AppDestinations.THEME_SETTINGS) }
            )
        }
        composable(AppDestinations.THEME_SETTINGS) {
            ThemeSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.MODELS) {
            ModelsScreen(
                extFileDir = extFilesDir,
                viewModel = viewModel,
                modelViewModel = modelViewModel,
                onSearchResultButtonClick = { navController.popBackStack() },
                dm = downloadManager,
                onQuantizeScreenButtonClicked = { navController.navigate(AppDestinations.QUANTIZE) }
            )
        }
        composable(AppDestinations.PARAMS) {
            ParametersScreen(viewModel = viewModel)
        }
        composable(AppDestinations.ABOUT) {
            AboutScreen()
        }
        composable(AppDestinations.BENCHMARK) {
            BenchMarkScreen(viewModel = viewModel, modelViewModel = modelViewModel)
        }
        composable(AppDestinations.QUANTIZE) {
            com.nervesparks.iris.ui.screens.QuantizeScreen(
                viewModel = viewModel,
                modelViewModel = modelViewModel
            )
        }
        composable(AppDestinations.TEMPLATES) {
            com.nervesparks.iris.ui.screens.TemplatesScreen(viewModel = viewModel)
        }
        composable(AppDestinations.MODEL_PERFORMANCE) {
            ModelPerformanceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}