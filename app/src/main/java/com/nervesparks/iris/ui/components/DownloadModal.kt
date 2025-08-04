package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.HuggingFaceApiService
import kotlinx.coroutines.launch

@Composable
fun DownloadModal(viewModel: MainViewModel, dm: DownloadManager, models: List<Downloadable>) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Map<String, String>>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Set test token for development
    LaunchedEffect(Unit) {
        viewModel.setTestHuggingFaceToken()
    }

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1a1a2e),
            modifier = Modifier
                .padding(16.dp)
                .height(if (showSearch) 650.dp else 400.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Download Required",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Don't close or minimize the app!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Download at least 1 model",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0f3460),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Mode selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showSearch = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!showSearch) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Text("Default Models", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = { showSearch = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showSearch) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Text("Search Models", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (!showSearch) {
                    // Default models list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filteredModels = models.filter { !it.destination.exists() }
                        Log.d("DownloadModal", "Total models: ${models.size}")
                        Log.d("DownloadModal", "Filtered models: ${filteredModels.size}")
                        models.forEach { model ->
                            Log.d("DownloadModal", "Model: ${model.name}, exists: ${model.destination.exists()}")
                        }
                        items(filteredModels) { model ->
                            DefaultModelCard(viewModel, dm, model)
                        }
                    }
                } else {
                    // Search interface
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search for models (e.g., 'qwen', 'llama')") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0f3460),
                                unfocusedBorderColor = Color(0xFF16213e),
                                focusedLabelColor = Color(0xFF0f3460),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSearching = true
                                    searchError = null
                                    try {
                                        val response = viewModel.searchModels(searchQuery)
                                        if (response.success && response.data != null) {
                                            val detailedModels = mutableListOf<Map<String, String>>()
                                            for (model in response.data.take(3)) {
                                                try {
                                                    val detailResponse = viewModel.getModelDetails(model.id)
                                                    if (detailResponse.success && detailResponse.data != null) {
                                                        val detailedModel = detailResponse.data.first()
                                                        detailedModels.add(mapOf(
                                                            "modelId" to detailedModel.id,
                                                            "modelName" to detailedModel.name,
                                                            "description" to (detailedModel.description ?: ""),
                                                            "downloads" to detailedModel.downloads.toString(),
                                                            "likes" to detailedModel.likes.toString(),
                                                            "tags" to detailedModel.tags.joinToString(", "),
                                                            "files" to detailedModel.siblings.joinToString("\n") { file ->
                                                                val sizeMB = file.size?.let { it / (1024 * 1024) } ?: 0
                                                                "${file.filename} (${sizeMB}MB${file.quantType?.let { " - $it" } ?: ""})"
                                                            }
                                                        ))
                                                    } else {
                                                        // Fallback to search result if detailed fetch fails
                                                        detailedModels.add(mapOf(
                                                            "modelId" to model.id,
                                                            "modelName" to model.name,
                                                            "description" to (model.description ?: ""),
                                                            "downloads" to model.downloads.toString(),
                                                            "likes" to model.likes.toString(),
                                                            "tags" to model.tags.joinToString(", "),
                                                            "files" to "File details not available"
                                                        ))
                                                    }
                                                } catch (e: Exception) {
                                                    // Fallback to search result if detailed fetch fails
                                                    detailedModels.add(mapOf(
                                                        "modelId" to model.id,
                                                        "modelName" to model.name,
                                                        "description" to (model.description ?: ""),
                                                        "downloads" to model.downloads.toString(),
                                                        "likes" to model.likes.toString(),
                                                        "tags" to model.tags.joinToString(", "),
                                                        "files" to "File details not available"
                                                    ))
                                                }
                                            }
                                            searchResults = detailedModels
                                        } else {
                                            searchError = response.error ?: "Unknown error occurred"
                                        }
                                    } catch (e: Exception) {
                                        searchError = "Network error: ${e.message}"
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0f3460),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSearching && searchQuery.isNotEmpty()
                        ) {
                            Text(if (isSearching) "Searching..." else "Search Models")
                        }

                        searchError?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        searchResults?.let { results ->
                            Text(
                                text = "Found ${results.size} models:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(results) { model ->
                                    SearchResultCard(model, dm, context)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultModelCard(viewModel: MainViewModel, dm: DownloadManager, model: Downloadable) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Downloadable.Button(viewModel, dm, model)
        }
    }
}

@Composable
private fun SearchResultCard(model: Map<String, String>, dm: DownloadManager, context: Context) {
    var showFileSelection by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xff0f172a)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model["modelName"] ?: "Unknown Model",
                color = Color(0xFFbbbdbf),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            model["description"]?.takeIf { it.isNotEmpty() }?.let { description ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Downloads: ${model["downloads"] ?: "0"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0f3460)
                )
                Text(
                    text = "Likes: ${model["likes"] ?: "0"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2196F3)
                )
            }
            
            model["tags"]?.takeIf { it.isNotEmpty() }?.let { tags ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tags: $tags",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            
            model["files"]?.takeIf { it != "File details not available" }?.let { files ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Available Files:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0f3460),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = files,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 3,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val modelId = model["modelId"] ?: ""
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Model ID", modelId)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Model ID copied! Search for '$modelId' in the Models screen to download specific files.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0f3460),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Copy Model ID")
                }
                
                Button(
                    onClick = { showFileSelection = !showFileSelection },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showFileSelection) Color(0xFF16213e) else Color(0xFF0f3460),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (showFileSelection) "Hide Files" else "Select File")
                }
            }
            
            // File selection dropdown
            if (showFileSelection) {
                Spacer(modifier = Modifier.height(8.dp))
                model["files"]?.takeIf { it != "File details not available" }?.let { files ->
                    val fileList = files.split("\n").filter { it.contains(".gguf") }
                    LazyColumn(
                        modifier = Modifier.height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(fileList) { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = file,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            try {
                                                val modelId = model["modelId"] ?: ""
                                                val downloadUrl = "https://huggingface.co/$modelId/resolve/main/$file"
                                                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                                    .setTitle("Downloading $file")
                                                    .setDescription("Downloading model file")
                                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "models/$file")
                                                
                                                val downloadId = dm.enqueue(request)
                                                Toast.makeText(context, "Download started for: $file", Toast.LENGTH_SHORT).show()
                                                
                                                // Log download info for debugging
                                                Log.d("DownloadModal", "Started download: $downloadUrl")
                                                Log.d("DownloadModal", "Download ID: $downloadId")
                                            } catch (e: Exception) {
                                                Log.e("DownloadModal", "Download failed for $file", e)
                                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0f3460),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Download")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
