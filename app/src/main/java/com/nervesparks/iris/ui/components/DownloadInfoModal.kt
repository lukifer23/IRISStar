package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.SemanticColors
import com.nervesparks.iris.ui.theme.ThemedDownloadSurface
import com.nervesparks.iris.ui.theme.ThemedAccentButton

@Composable
fun DownloadInfoModal(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ThemedDownloadSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.defaultPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SemanticColors.TextInverse(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SemanticColors.TextInverse().copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                Text(
                    text = "Download progress will be shown in notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticColors.TextInverse().copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                ThemedAccentButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        }
    }
}