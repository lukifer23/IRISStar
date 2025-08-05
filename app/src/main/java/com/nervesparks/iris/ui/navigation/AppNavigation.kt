package com.nervesparks.iris.ui.navigation

import android.app.DownloadManager
import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.AboutScreen
import com.nervesparks.iris.ui.BenchMarkScreen
import com.nervesparks.iris.ui.ChatListScreen
import com.nervesparks.iris.ui.ModelsScreen
import com.nervesparks.iris.ui.ParametersScreen
import com.nervesparks.iris.ui.SettingsScreen
import com.nervesparks.iris.ui.screens.MainChatScreen2
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
    NavHost(navController = navController, startDestination = AppDestinations.CHAT) {
        composable(AppDestinations.CHAT_LIST) {
            ChatListScreen(
                viewModel = viewModel,
                onChatSelected = { chatId ->
                    navController.navigate("${AppDestinations.CHAT}?chatId=$chatId")
                },
                onNewChat = {
                    viewModel.clear()
                    navController.navigate(AppDestinations.CHAT)
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
                viewModel = viewModel
            )
        }
        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onModelsScreenButtonClicked = { navController.navigate(AppDestinations.MODELS) },
                onParamsScreenButtonClicked = { navController.navigate(AppDestinations.PARAMS) },
                onAboutScreenButtonClicked = { navController.navigate(AppDestinations.ABOUT) },
                onBenchMarkScreenButtonClicked = { navController.navigate(AppDestinations.BENCHMARK) }
            )
        }
        composable(AppDestinations.MODELS) {
            ModelsScreen(
                extFileDir = extFilesDir,
                viewModel = viewModel,
                onSearchResultButtonClick = { navController.popBackStack() },
                dm = downloadManager
            )
        }
        composable(AppDestinations.PARAMS) {
            ParametersScreen(viewModel = viewModel)
        }
        composable(AppDestinations.ABOUT) {
            AboutScreen()
        }
        composable(AppDestinations.BENCHMARK) {
            BenchMarkScreen(viewModel = viewModel)
        }
    }
}