package com.nervesparks.iris.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.PrimaryButton
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantizeScreen(
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val models = viewModel.allModels.mapNotNull { it["name"] }
    var selectedModel by remember { mutableStateOf(models.firstOrNull() ?: "") }

    var expandedQuantization by remember { mutableStateOf(false) }
    val quantizationLevels = listOf("Q4_K_M", "Q5_K_M", "Q6_K", "Q8_0")
    var selectedQuantization by remember { mutableStateOf(quantizationLevels[0]) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showProgress by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var quantizeJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier.padding(ComponentStyles.defaultPadding),
        verticalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)
    ) {
        Text("Select Model", style = MaterialTheme.typography.titleMedium)
        Card(
            shape = ComponentStyles.defaultCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(ComponentStyles.defaultElevation)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.smallPadding)
            ) {
                TextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                selectedModel = model
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Text("Select Quantization Level", style = MaterialTheme.typography.titleMedium)
        Card(
            shape = ComponentStyles.defaultCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(ComponentStyles.defaultElevation)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedQuantization,
                onExpandedChange = { expandedQuantization = !expandedQuantization },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.smallPadding)
            ) {
                TextField(
                    value = selectedQuantization,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuantization)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedQuantization,
                    onDismissRequest = { expandedQuantization = false }
                ) {
                    quantizationLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                selectedQuantization = level
                                expandedQuantization = false
                            }
                        )
                    }
                }
            }
        }

        PrimaryButton(
            onClick = {
                val file = File(context.getExternalFilesDir(null), selectedModel)
                if (!file.exists() || !selectedModel.endsWith(".gguf", ignoreCase = true)) {
                    errorMessage = "Model file not found: $selectedModel"
                } else {
                    showProgress = true
                    progress = 0f
                    progressText = "Downloading model..."
                    quantizeJob = coroutineScope.launch {
                        try {
                            delay(100)
                            progress = 0.33f
                            progressText = "Converting model..."
                            val result = viewModel.quantizeModel(selectedModel, selectedQuantization)
                            if (result == 0) {
                                progress = 0.66f
                                progressText = "Saving model..."
                                progress = 1f
                                showProgress = false
                                Toast.makeText(context, "Quantization successful", Toast.LENGTH_SHORT).show()
                            } else {
                                showProgress = false
                                errorMessage = "Quantization failed with code $result"
                            }
                        } catch (e: Exception) {
                            showProgress = false
                            errorMessage = "Quantization failed: ${e.message}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quantize", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showProgress) {
        QuantizeProgressDialog(
            stepText = progressText,
            progress = progress,
            onDismiss = {
                showProgress = false
                quantizeJob?.cancel()
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            },
            text = { Text(message) }
        )
    }
}

@Composable
private fun QuantizeProgressDialog(
    stepText: String,
    progress: Float,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = ComponentStyles.defaultCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(ComponentStyles.defaultElevation)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.defaultPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stepText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
