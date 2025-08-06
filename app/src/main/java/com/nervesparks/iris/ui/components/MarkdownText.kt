package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.ui.theme.ComponentStyles

@Composable
fun MarkdownTextComponent(markdown: String) {
    val lines = markdown.split("\n")
    
    Column {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                    )
                }
                line.startsWith("```") -> {
                    // Code block
                    val codeContent = lines.dropWhile { !it.startsWith("```") }
                        .drop(1)
                        .takeWhile { !it.startsWith("```") }
                        .joinToString("\n")
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ComponentStyles.smallPadding),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = ComponentStyles.smallCardShape
                    ) {
                        Text(
                            text = codeContent,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(ComponentStyles.defaultPadding)
                        )
                    }
                }
                line.startsWith("- ") -> {
                    Row(
                        modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = line.removePrefix("- "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.startsWith("* ") -> {
                    Row(
                        modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = line.removePrefix("* "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.trim().isEmpty() -> {
                    Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                }
                else -> {
                    // Handle plain text with better formatting
                    val formattedText = formatPlainText(line)
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                    )
                }
            }
        }
    }
}

/**
 * Formats plain text to make it more readable
 */
private fun formatPlainText(text: String): String {
    return text
        .replace(Regex("([.!?])([A-Z])"), "$1\n$2") // Add line breaks after sentences
        .replace(Regex("([.!?])([a-z])"), "$1 $2") // Add spaces after sentences
        .replace(Regex("([a-z])([A-Z])"), "$1 $2") // Add spaces between camelCase
        .replace(Regex("([a-z])([0-9])"), "$1 $2") // Add spaces between letters and numbers
        .replace(Regex("([0-9])([a-zA-Z])"), "$1 $2") // Add spaces between numbers and letters
        .replace(Regex("\\s+"), " ") // Normalize multiple spaces
        .trim()
} 