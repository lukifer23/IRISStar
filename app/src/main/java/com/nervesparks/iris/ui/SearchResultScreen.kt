package com.nervesparks.iris.ui

import android.app.DownloadManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.ui.components.InfoModal
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.ModelCard
import com.nervesparks.iris.ui.components.IrisTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

@Composable
fun SearchResultScreen(
    viewModel: MainViewModel,
    dm: DownloadManager,
    extFilesDir: File,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var modelData by rememberSaveable { mutableStateOf<List<Map<String, String>>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val kc = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    var UserGivenModel by remember {
        mutableStateOf(
            TextFieldValue(
                text = viewModel.userGivenModel,
                selection = TextRange(viewModel.userGivenModel.length)
            )
        )
    }
    if (viewModel.showAlert) {
        // Modal dialog to show download options
        LoadingModal(viewModel)

    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IrisTopAppBar(
            title = "Search",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBackClick,
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            InfoModal(
                showDialog = viewModel.showDownloadInfoModal,
                onDismiss = { viewModel.showDownloadInfoModal = false }
            )
            // Search Input and Button Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Example: bartowski/Llama-3.2-1B-Instruct-GGUF",
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 10.sp
                    )

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("bartowski/Llama-3.2-1B-Instruct-GGUF"))
                            Toast.makeText(context, "Text copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.copy1),
                            contentDescription = "Copy text",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            OutlinedTextField(
            value = UserGivenModel,
            onValueChange = { newValue ->
                UserGivenModel = newValue
                viewModel.userGivenModel = newValue.text
            },
            label = { Text("Search Models Online") },
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Transparent),
            singleLine = true,
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
            )
        )

        Spacer(Modifier.height(16.dp))


        Button(
            onClick = {
                kc?.hide()
                
                // TODO: Add proper credentials check when API is implemented
                // if (!preferencesRepository.hasHuggingFaceCredentials()) {
                //     errorMessage = "Please set your HuggingFace credentials in Settings first"
                //     return@Button
                // }
                
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        // Use searchModelsAsync for proper async search
                        val response = viewModel.searchModelsAsync(UserGivenModel.text)
                        
                        if (response.success && response.data != null) {
                            // Convert search results to the expected format
                            // Note: Search results don't include siblings, so we'll show model info directly
                            modelData = response.data.map { model ->
                                mapOf(
                                    "modelId" to model.id,
                                    "modelName" to model.name,
                                    "description" to (model.description ?: ""),
                                    "downloads" to model.downloads.toString(),
                                    "likes" to model.likes.toString(),
                                    "tags" to model.tags.joinToString(", ")
                                )
                            }
                        } else {
                            errorMessage = response.error ?: "Failed to search models"
                            modelData = null
                        }
                    } catch (e: Exception) {
                        Log.e("SearchResultScreen", "Error searching models", e)
                        errorMessage = "Error: ${e.localizedMessage}"
                        modelData = null
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Search Models")
        }

        // Error Message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        
        // Model Results
        modelData?.let { models ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(models) { model ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Model header
                            Text(
                                text = model["modelName"] ?: "Unknown Model",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (model["description"]?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = model["description"] ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Downloads: ${model["downloads"] ?: "0"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Likes: ${model["likes"] ?: "0"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (model["tags"]?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tags: ${model["tags"]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Note about getting files
                            Text(
                                text = "Note: To download files, search for the specific model ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 10.sp
                            )
                            
                            // Copy model ID button
                            Button(
                                onClick = {
                                    val modelId = model["modelId"] ?: ""
                                    clipboardManager.setText(AnnotatedString(modelId))
                                    Toast.makeText(context, "Model ID copied: $modelId", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Copy Model ID")
                            }
                        }
                    }
                }
            }
        }

        // Loading Indicator (Optional)
        if (isLoading) {
            Text(
                text = "Searching for models...",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

