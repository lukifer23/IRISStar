package com.nervesparks.iris

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import java.io.File
import androidx.compose.foundation.BorderStroke
import android.app.Application
import androidx.compose.foundation.border
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.security.BiometricAuthenticator
import com.nervesparks.iris.ui.theme.ManagedIrisStarTheme
import com.nervesparks.iris.ui.theme.ThemeViewModel
import com.nervesparks.iris.ui.theme.ThemeViewModelFactory
import com.nervesparks.iris.ui.navigation.AppNavigation
import com.nervesparks.iris.viewmodel.ModelViewModel
import com.nervesparks.iris.viewmodel.ChatViewModel
import com.nervesparks.iris.viewmodel.DownloadViewModel
import com.nervesparks.iris.workers.ModelUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    private val downloadManager by lazy { getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val modelViewModel: ModelViewModel by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize native library early
        try {
            android.util.Log.d("MainActivity", "Initializing native library...")
            val success = LLamaAndroid.instance().ensureLibraryLoaded()
            android.util.Log.d("MainActivity", "Native library initialization: $success")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize native library", e)
        }

        // Allow network operations on main thread for development
        val policy = VmPolicy.Builder()
            .detectAll()
            .build()
        StrictMode.setVmPolicy(policy)

        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(
            Downloadable(
                "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-3B-Instruct-Q4_K_L.gguf")
            ),
            Downloadable(
                "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-1B-Instruct-Q6_K_L.gguf")
            ),
            Downloadable(
                "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
                Uri.parse("https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true"),
                File(extFilesDir, "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf")
            ),
            Downloadable(
                "NemoTron-1.5B-Q4_K_M.gguf",
                Uri.parse("https://huggingface.co/bartowski/nvidia_OpenReasoning-Nemotron-1.5B-GGUF/resolve/main/nvidia_OpenReasoning-Nemotron-1.5B-Q4_K_M.gguf?download=true"),
                File(extFilesDir, "NemoTron-1.5B-Q4_K_M.gguf")
            ),
            Downloadable(
                "Qwen_Qwen3-0.6B-Q4_K_M.gguf",
                Uri.parse("https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen_Qwen3-0.6B-Q4_K_M.gguf?download=true"),
                File(extFilesDir, "Qwen_Qwen3-0.6B-Q4_K_M.gguf")
            )
        )

        if (extFilesDir != null) {
            modelViewModel.loadExistingModels(extFilesDir)
            downloadViewModel.loadExistingModels(extFilesDir)
        }

        ModelUpdateWorker.schedule(this)

        val content = @Composable {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModelFactory(preferencesRepository, applicationContext)
            )

            ManagedIrisStarTheme(themeViewModel = themeViewModel) {
                AppNavigation(
                    viewModel = viewModel,
                    chatViewModel = chatViewModel,
                    modelViewModel = modelViewModel,
                    clipboardManager = clipboardManager,
                    downloadManager = downloadManager,
                    models = models,
                    extFilesDir = extFilesDir,
                    preferencesRepository = preferencesRepository
                )
            }
        }

        lifecycleScope.launch {
            val biometricEnabled = preferencesRepository.getSecurityBiometricEnabled()
            if (biometricEnabled) {
                val biometricAuthenticator = BiometricAuthenticator(this@MainActivity)
                biometricAuthenticator.authenticate(this@MainActivity) {
                    setContent { content() }
                }
            } else {
                setContent { content() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Cleanup resources when app is backgrounded to help with memory management
        viewModel.cleanupResources()
    }
}











