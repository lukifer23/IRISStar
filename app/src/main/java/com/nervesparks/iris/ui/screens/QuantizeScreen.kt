package com.nervesparks.iris.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantizeScreen(
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val models = viewModel.allModels.map { it["name"] }
    var selectedModel by remember { mutableStateOf(models.firstOrNull() ?: "") }

    var expandedQuantization by remember { mutableStateOf(false) }
    val quantizationLevels = listOf("Q4_K_M", "Q5_K_M", "Q6_K", "Q8_0")
    var selectedQuantization by remember { mutableStateOf(quantizationLevels[0]) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Model:")
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                models.forEach { model ->
                    if (model != null) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Text("Select Quantization Level:")
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expandedQuantization,
            onExpandedChange = { expandedQuantization = !expandedQuantization }
        ) {
            TextField(
                value = selectedQuantization,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedQuantization)
                },
                modifier = Modifier.menuAnchor()
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.quantizeModel(selectedModel, selectedQuantization) }) {
            Text("Quantize")
        }
    }
}
