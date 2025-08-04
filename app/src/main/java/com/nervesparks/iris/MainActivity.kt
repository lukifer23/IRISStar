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
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.ui.SettingsBottomSheet
import com.nervesparks.iris.ui.theme.IRISTheme
import com.nervesparks.iris.ui.ChatListScreen
import com.nervesparks.iris.ui.MainChatScreen
import com.nervesparks.iris.ui.SettingsScreen
import com.nervesparks.iris.ui.components.ModelSelectionModal
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity(
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
): ComponentActivity() {
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            viewModel.loadExistingModels(extFilesDir)
        }

        setContent {
            IRISTheme {
                var showSettingSheet by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf("chat_list") } // chat_list, chat, settings
                var selectedChatId by remember { mutableStateOf<String?>(null) }
                
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight(),
                            drawerContainerColor = Color(0xFF070915)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(5.dp)
                                    .fillMaxHeight()
                            ) {
                                // Top section with logo and name
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.logo),
                                            contentDescription = "Centered Background Logo",
                                            modifier = Modifier.size(35.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(Modifier.padding(5.dp))
                                        Text(
                                            text = "IRIS Star",
                                            fontWeight = FontWeight(500),
                                            color = Color.White,
                                            fontSize = 30.sp
                                        )
                                        Spacer(Modifier.weight(1f))
                                        if (showSettingSheet) {
                                            SettingsBottomSheet(
                                                viewModel = viewModel,
                                                onDismiss = { showSettingSheet = false }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.padding(start = 45.dp)
                                    ) {
                                        Text(
                                            text = "NerveSparks",
                                            color = Color(0xFF636466),
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Navigation menu
                                Column(modifier = Modifier.padding(6.dp)) {
                                    // Chat List
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .padding(horizontal = 8.dp)
                                            .background(
                                                color = if (currentScreen == "chat_list") Color(0xFF00BCD4) else Color(0xFF16213e),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                currentScreen = "chat_list"
                                                selectedChatId = null
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "Chat List",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // New Chat
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .padding(horizontal = 8.dp)
                                            .background(
                                                color = if (currentScreen == "chat") Color(0xFF00BCD4) else Color(0xFF16213e),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                currentScreen = "chat"
                                                selectedChatId = null
                                                viewModel.clear()
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "New Chat",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Settings
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .padding(horizontal = 8.dp)
                                            .background(
                                                color = if (currentScreen == "settings") Color(0xFF00BCD4) else Color(0xFF16213e),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                currentScreen = "settings"
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "Settings",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Active Model Section
                                    Text(
                                        text = "Active Model",
                                        fontSize = 16.sp,
                                        color = Color(0xFF636466),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                    Text(
                                        text = viewModel.loadedModelName.value,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                    
                                    // Model selection button
                                    val availableModels = extFilesDir?.let { viewModel.getAvailableModels(it) } ?: emptyList()
                                    
                                    if (availableModels.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                                .padding(horizontal = 8.dp)
                                                .background(
                                                    color = Color(0xFF00BCD4),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    viewModel.showModelSelectionDialog()
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = "Switch Model (${availableModels.size} available)",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // Powered by section
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "powered by",
                                            color = Color(0xFF636466),
                                            fontSize = 14.sp
                                        )
                                        val context = LocalContext.current
                                        Text(
                                            modifier = Modifier.clickable {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://github.com/ggerganov/llama.cpp")
                                                }
                                                context.startActivity(intent)
                                            },
                                            text = " llama.cpp",
                                            color = Color(0xFF78797a),
                                            fontSize = 16.sp
                                        )
                                    }
                                 }
                            }
                        }
                    }
                ) {
                    // Main content based on current screen
                    when (currentScreen) {
                        "chat_list" -> {
                            ChatListScreen(
                                viewModel = viewModel,
                                onChatSelected = { chatId ->
                                    selectedChatId = chatId.toString()
                                    currentScreen = "chat"
                                },
                                onNewChat = {
                                    selectedChatId = null
                                    currentScreen = "chat"
                                    viewModel.clear()
                                }
                            )
                        }
                        "chat" -> {
                            MainChatScreen(
                                onNextButtonClicked = { /* TODO */ },
                                viewModel = viewModel,
                                clipboard = clipboardManager,
                                dm = downloadManager,
                                models = models,
                                extFileDir = extFilesDir,
                                chatId = selectedChatId?.toLongOrNull()
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                onModelsScreenButtonClicked = { currentScreen = "models" },
                                onParamsScreenButtonClicked = { currentScreen = "params" },
                                onAboutScreenButtonClicked = { currentScreen = "about" },
                                onBenchMarkScreenButtonClicked = { currentScreen = "benchmark" }
                            )
                        }
                    }
                }
                
                // Model selection modal
                if (viewModel.showModelSelection) {
                    ModelSelectionModal(
                        viewModel = viewModel,
                        onDismiss = { viewModel.showModelSelection = false }
                    )
                }
            }
        }
    }
}

@Composable
fun LinearGradient() {
    val darkNavyBlue = Color(0xFF050a14)
    val lightNavyBlue = Color(0xFF051633)
    val gradient = Brush.linearGradient(
        colors = listOf(darkNavyBlue, lightNavyBlue),
        start = Offset(0f, 300f),
        end = Offset(0f, 1000f)

    )
    Box(modifier = Modifier.background(gradient).fillMaxSize())
}







// [END android_compose_layout_material_modal_drawer]









