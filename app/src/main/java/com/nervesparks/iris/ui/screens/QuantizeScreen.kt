package com.nervesparks.iris.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.PrimaryButton
import java.io.File
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(ComponentStyles.defaultPadding),
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
                    Toast.makeText(context, "Model file not found", Toast.LENGTH_LONG).show()
                } else {
                    showProgress = true
                    coroutineScope.launch {
                        val result = viewModel.quantizeModel(selectedModel, selectedQuantization)
                        showProgress = false
                        if (result == 0) {
                            Toast.makeText(context, "Quantization successful", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Quantization failed", Toast.LENGTH_LONG).show()
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
        LoadingModal(message = "Quantizing model...", onDismiss = {})
    }
}
