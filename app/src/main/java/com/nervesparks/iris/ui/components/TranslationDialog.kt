package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ThemedModalCard
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.SecondaryButton

@Composable
fun TranslationDialog(
    onDismiss: () -> Unit,
    onTranslate: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ThemedModalCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Translate Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                Text(
                    text = "Would you like to translate the current message to English?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = ComponentStyles.defaultSpacing)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SecondaryButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(ComponentStyles.defaultSpacing))
                    
                    PrimaryButton(
                        onClick = {
                            onTranslate()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Translate")
                    }
                }
            }
        }
    }
}
