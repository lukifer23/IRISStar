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
import com.nervesparks.iris.ui.theme.ThemedLoadingSurface
import com.nervesparks.iris.ui.theme.SemanticColors

@Composable
fun LoadingModal(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ThemedLoadingSurface(
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
                CircularProgressIndicator(
                    modifier = Modifier.size(ComponentStyles.largeIconSize),
                    color = SemanticColors.LoadingAccent(),
                    strokeWidth = ComponentStyles.thickBorderWidth
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SemanticColors.TextInverse(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                
                Text(
                    text = "Please wait...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticColors.LoadingAccent(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}