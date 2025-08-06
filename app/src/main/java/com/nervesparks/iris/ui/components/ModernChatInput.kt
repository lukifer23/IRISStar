package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ThemedChatInputField
import com.nervesparks.iris.ui.theme.ModernIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onCameraClick: () -> Unit = {},
    onPhotosClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    onCodeClick: () -> Unit = {},
    isCodeMode: Boolean = false,
    onTranslateClick: () -> Unit = {},
    onWebSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "Ask anything",
    enabled: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showTranslationDialog by remember { mutableStateOf(false) }

    if (showTranslationDialog) {
        TranslationDialog(
            onDismiss = { showTranslationDialog = false },
            onTranslate = { onTranslateClick() }
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = ComponentStyles.modalElevation,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = ComponentStyles.defaultPadding, vertical = ComponentStyles.smallPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernIconButton(
                    onClick = {
                        showAttachmentDialog = true
                        onAttachmentClick()
                    },
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ComponentStyles.defaultIconSize)
                    )
                }

                ModernIconButton(
                    onClick = onCodeClick,
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Code",
                        tint = if (isCodeMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ComponentStyles.defaultIconSize)
                    )
                }

                ModernIconButton(
                    onClick = { showTranslationDialog = true },
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ComponentStyles.defaultIconSize)
                    )
                }

                ModernIconButton(
                    onClick = onWebSearchClick,
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Web Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ComponentStyles.defaultIconSize)
                    )
                }

                ThemedChatInputField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text(placeholder) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSend() }
                    ),
                    enabled = enabled,
                )

                Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))

                if (value.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(ComponentStyles.defaultIconSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    ModernIconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier.size(ComponentStyles.defaultIconSize)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.mic_svgrepo_com),
                            contentDescription = "Voice Input",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(ComponentStyles.defaultIconSize)
                        )
                    }
                }
            }

            if (showAttachmentDialog) {
                AttachmentBottomSheet(
                    onDismiss = { showAttachmentDialog = false },
                    onCameraClick = onCameraClick,
                    onPhotosClick = onPhotosClick,
                    onFilesClick = onFilesClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onPhotosClick: () -> Unit,
    onFilesClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(ComponentStyles.defaultPadding)
        ) {
            Text(
                text = "Choose attachment",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = ComponentStyles.defaultPadding)
            )

            AttachmentOption(
                icon = Icons.Default.CameraAlt,
                text = "Camera",
                onClick = {
                    onCameraClick()
                    onDismiss()
                }
            )

            AttachmentOption(
                icon = Icons.Default.PhotoLibrary,
                text = "Photos",
                onClick = {
                    onPhotosClick()
                    onDismiss()
                }
            )

            AttachmentOption(
                icon = Icons.Default.AttachFile,
                text = "Files",
                onClick = {
                    onFilesClick()
                    onDismiss()
                }
            )
        }
    }
}

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
            .padding(vertical = ComponentStyles.defaultSpacing, horizontal = ComponentStyles.defaultPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(ComponentStyles.defaultIconSize)
        )

        Spacer(modifier = Modifier.width(ComponentStyles.defaultPadding))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
