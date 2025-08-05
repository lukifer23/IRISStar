package com.nervesparks.iris.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.AboutScreen
import com.nervesparks.iris.ui.BenchMarkScreen
import com.nervesparks.iris.ui.ChatListScreen
import com.nervesparks.iris.ui.MainChatScreen
import com.nervesparks.iris.ui.ModelsScreen
import com.nervesparks.iris.ui.ParametersScreen
import com.nervesparks.iris.ui.SettingsScreen
import android.app.DownloadManager
import android.content.ClipboardManager
import com.nervesparks.iris.Downloadable
import java.io.File

object AppDestinations {
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val MODELS = "models"
    const val PARAMS = "params"
    const val ABOUT = "about"
    const val BENCHMARK = "benchmark"
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    clipboardManager: ClipboardManager,
    downloadManager: DownloadManager,
    models: List<Downloadable>,
    extFilesDir: File?
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppDestinations.CHAT_LIST) {
        composable(AppDestinations.CHAT_LIST) {
            ChatListScreen(
                viewModel = viewModel,
                onChatSelected = { chatId ->
                    navController.navigate("${AppDestinations.CHAT}?chatId=$chatId")
                },
                onNewChat = {
                    viewModel.clear()
                    navController.navigate(AppDestinations.CHAT)
                },
                onMenuClick = { navController.navigate(AppDestinations.SETTINGS) }
            )
        }
        composable("${AppDestinations.CHAT}?chatId={chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull()
            MainChatScreen(
                onNextButtonClicked = { /* Not used with NavHost */ },
                viewModel = viewModel,
                clipboard = clipboardManager,
                dm = downloadManager,
                models = models,
                extFileDir = extFilesDir,
                chatId = chatId,
                onBackPressed = { navController.popBackStack() },
                onNavigateToModels = { navController.navigate(AppDestinations.MODELS) }
            )
        }
        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onModelsScreenButtonClicked = { navController.navigate(AppDestinations.MODELS) },
                onParamsScreenButtonClicked = { navController.navigate(AppDestinations.PARAMS) },
                onAboutScreenButtonClicked = { navController.navigate(AppDestinations.ABOUT) },
                onBenchMarkScreenButtonClicked = { navController.navigate(AppDestinations.BENCHMARK) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.MODELS) {
            ModelsScreen(
                extFileDir = extFilesDir,
                viewModel = viewModel,
                onSearchResultButtonClick = { navController.popBackStack() },
                dm = downloadManager,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.PARAMS) {
            ParametersScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
        composable(AppDestinations.ABOUT) {
            AboutScreen(onBackClick = { navController.popBackStack() })
        }
        composable(AppDestinations.BENCHMARK) {
            BenchMarkScreen(viewModel = viewModel, onBackClick = { navController.popBackStack() })
        }
    }
}
