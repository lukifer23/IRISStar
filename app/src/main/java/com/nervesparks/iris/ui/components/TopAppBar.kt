package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Settings
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
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernIconButton
import com.nervesparks.iris.ui.theme.ThemedModalCard
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
    extFilesDir: File?,
    onSettingsClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
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
            ModernIconButton(
                onClick = onMenuClick
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                )
            }
        },
        actions = {
            // Model selection button
            if (availableModels.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .padding(end = ComponentStyles.smallPadding)
                        .widthIn(max = 150.dp) // Constrain the width
                        .clickable { onModelClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = ComponentStyles.smallCardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.defaultElevation)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = ComponentStyles.smallPadding,
                            vertical = ComponentStyles.smallPadding
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Current model",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(ComponentStyles.smallIconSize)
                        )
                        Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
                        Text(
                            text = if (currentModel.isNotEmpty()) currentModel else "No Model",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1, // Ensure single line
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis // Add ellipsis
                        )
                        Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Model",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(ComponentStyles.smallIconSize)
                        )
                    }
                }
            }

            // Settings button
            ModernIconButton(
                onClick = onSettingsClick
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Chat Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                )
            }

            actions()
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
                extFilesDir?.let { viewModel.switchModel(modelName, it) }
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
        ThemedModalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultPadding),
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding)
            ) {
                Text(
                    text = "Select Model",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = ComponentStyles.defaultPadding)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
                ) {
                    items(availableModels, key = { it }) { model ->
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
                            shape = ComponentStyles.smallCardShape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(ComponentStyles.defaultSpacing),
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
                                        modifier = Modifier.size(ComponentStyles.defaultIconSize)
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