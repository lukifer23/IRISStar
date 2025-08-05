package com.nervesparks.iris.ui.components
import com.nervesparks.iris.ui.theme.Spacing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nervesparks.iris.ui.theme.*

/**
 * Modern bottom input area with attachment options, voice input, and quick actions
 * Matches the design patterns from reference images
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onLatestNews: () -> Unit = {},
    onCreateImages: () -> Unit = {},
    onCartoonStyle: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onPhotosClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "Ask anything",
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
    ) {
        // Quick action buttons (horizontal row)
        QuickActionsRow(
            onLatestNews = onLatestNews,
            onCreateImages = onCreateImages,
            onCartoonStyle = onCartoonStyle
        )
        
        Spacer(modifier = Modifier.height(Spacing.m))
        
        // Main input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = Spacing.m, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment button
            IconButton(
                onClick = {
                    showAttachmentDialog = true
                    onAttachmentClick()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Text input field
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(horizontal = Spacing.s),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSend() }
                ),
                enabled = enabled,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
            
            // Voice input button
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Voice Input",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Attachment dialog
        if (showAttachmentDialog) {
            AttachmentDialog(
                onDismiss = { showAttachmentDialog = false },
                onCameraClick = onCameraClick,
                onPhotosClick = onPhotosClick,
                onFilesClick = onFilesClick
            )
        }
    }
}

/**
 * Quick action buttons row
 */
@Composable
private fun QuickActionsRow(
    onLatestNews: () -> Unit,
    onCreateImages: () -> Unit,
    onCartoonStyle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Info,
            text = "Latest news",
            onClick = onLatestNews
        )
        
        QuickActionButton(
            icon = Icons.Default.Add,
            text = "Create images",
            onClick = onCreateImages
        )
        
        QuickActionButton(
            icon = Icons.Default.Edit,
            text = "Cartoon style",
            onClick = onCartoonStyle
        )
    }
}

/**
 * Individual quick action button
 */
@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = Spacing.s)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(Spacing.m)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(Spacing.m)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.s))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}

/**
 * Attachment dialog with camera, photos, and files options
 */
@Composable
private fun AttachmentDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotosClick: () -> Unit,
    onFilesClick: () -> Unit
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
                .padding(Spacing.m),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(Spacing.m)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.m)
            ) {
                Text(
                    text = "Choose attachment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.m)
                )
                
                // Camera option
                AttachmentOption(
                    icon = Icons.Default.Add,
                    text = "Camera",
                    onClick = {
                        onCameraClick()
                        onDismiss()
                    }
                )
                
                // Photos option
                AttachmentOption(
                    icon = Icons.Default.Add,
                    text = "Photos",
                    onClick = {
                        onPhotosClick()
                        onDismiss()
                    }
                )
                
                // Files option
                AttachmentOption(
                    icon = Icons.Default.Add,
                    text = "Files",
                    onClick = {
                        onFilesClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * Individual attachment option
 */
@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(Spacing.l)
        )
        
        Spacer(modifier = Modifier.width(Spacing.m))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
} 