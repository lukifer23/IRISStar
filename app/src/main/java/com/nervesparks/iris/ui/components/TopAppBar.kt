package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nervesparks.iris.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopAppBar(
    title: String,
    onMenuClick: () -> Unit,
    onModelClick: () -> Unit,
    currentModel: String,
    availableModels: List<String>,
    showModelDropdown: Boolean,
    onModelDropdownDismiss: () -> Unit,
    viewModel: MainViewModel,
    extFilesDir: File?
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(
                onClick = { viewModel.togglePerformanceMonitor() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Toggle Performance Monitor",
                    tint = if (viewModel.showPerformanceMonitor)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Model selection button
            if (availableModels.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { onModelClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentModel.isNotEmpty()) currentModel else "No Model",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Model",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
    
    // Model selection dropdown
    if (showModelDropdown && availableModels.isNotEmpty()) {
        ModelSelectionDropdown(
            availableModels = availableModels,
            currentModel = currentModel,
            onModelSelected = { modelName ->
                extFilesDir?.let { viewModel.loadModelByName(modelName, it) }
                onModelDropdownDismiss()
            },
            onDismiss = onModelDropdownDismiss
        )
    }
}

@Composable
private fun ModelSelectionDropdown(
    availableModels: List<String>,
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Model",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableModels) { model ->
                        val isCurrentModel = model == currentModel
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModelSelected(model) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentModel) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrentModel) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isCurrentModel) FontWeight.Bold else FontWeight.Normal
                                )
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                if (isCurrentModel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Current Model",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 