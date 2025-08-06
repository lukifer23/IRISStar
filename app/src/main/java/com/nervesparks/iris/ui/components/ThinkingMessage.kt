package com.nervesparks.iris.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.MarkdownTextComponent
import com.nervesparks.iris.ui.theme.ComponentStyles

@Composable
fun ThinkingMessage(
    message: String,
    viewModel: MainViewModel,
    showThinkingTokens: Boolean,
    onLongClick: () -> Unit
) {
    var isThinkingExpanded by remember { mutableStateOf(showThinkingTokens) }
    
    // Parse via centralised parser
    val (thinkingContent, outputContent) = com.nervesparks.iris.llm.ReasoningParser.parse(message)
    
    // Update expanded state when showThinkingTokens changes
    LaunchedEffect(showThinkingTokens) {
        isThinkingExpanded = showThinkingTokens
    }
    
    // Debug logging
    LaunchedEffect(message) {
        android.util.Log.d("ThinkingMessage", "=== THINKING MESSAGE DEBUG ===")
        android.util.Log.d("ThinkingMessage", "Message length: ${message.length}")
        android.util.Log.d("ThinkingMessage", "Message preview: ${message.take(200)}...")
        android.util.Log.d("ThinkingMessage", "Thinking content: '$thinkingContent'")
        android.util.Log.d("ThinkingMessage", "Output content: '$outputContent'")
        android.util.Log.d("ThinkingMessage", "Thinking content length: ${thinkingContent.length}")
        android.util.Log.d("ThinkingMessage", "Contains <think>: ${message.contains("<think>")}")
        android.util.Log.d("ThinkingMessage", "Contains </think>: ${message.contains("</think>")}")
        android.util.Log.d("ThinkingMessage", "showThinkingTokens: $showThinkingTokens")
        android.util.Log.d("ThinkingMessage", "isThinkingExpanded: $isThinkingExpanded")
        android.util.Log.d("ThinkingMessage", "=== END THINKING DEBUG ===")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentStyles.defaultPadding, vertical = ComponentStyles.smallPadding),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = ComponentStyles.extraLargeCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.smallElevation)
    ) {
        Column(
            modifier = Modifier.padding(ComponentStyles.defaultPadding)
        ) {
            // Header with thinking indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Thinking",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ComponentStyles.defaultIconSize)
                )
                
                Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                
                Text(
                    text = "AI Reasoning",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Toggle button - only show if there's thinking content
                if (thinkingContent.isNotEmpty()) {
                    IconButton(
                        onClick = { 
                            isThinkingExpanded = !isThinkingExpanded
                            viewModel.updateShowThinkingTokens(isThinkingExpanded)
                        }
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isThinkingExpanded) 180f else 0f,
                            animationSpec = tween(durationMillis = ComponentStyles.defaultAnimationDuration)
                        )
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isThinkingExpanded) "Hide reasoning" else "Show reasoning",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            }
            
            // Thinking content (collapsible) - Only show if thinking content exists and is expanded
            AnimatedVisibility(
                visible = isThinkingExpanded && thinkingContent.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(ComponentStyles.defaultAnimationDuration)),
                exit = shrinkVertically(animationSpec = tween(ComponentStyles.defaultAnimationDuration))
            ) {
                android.util.Log.d("ThinkingMessage", "Rendering thinking content: expanded=$isThinkingExpanded, hasContent=${thinkingContent.isNotEmpty()}")
                if (thinkingContent.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ComponentStyles.smallPadding),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = ComponentStyles.defaultCardShape
                    ) {
                        Column(
                            modifier = Modifier.padding(ComponentStyles.defaultSpacing)
                        ) {
                            Text(
                                text = "Internal Reasoning:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                            Text(
                                text = formatThinkingContent(thinkingContent),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
            
            // Output content
            if (outputContent.isNotEmpty() || thinkingContent.isEmpty()) {
                Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLongClick() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = ComponentStyles.defaultCardShape
                ) {
                    Column(
                        modifier = Modifier.padding(ComponentStyles.defaultSpacing)
                    ) {
                        Text(
                            text = "Response:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                        MarkdownTextComponent(
                            markdown = if (outputContent.isNotEmpty()) outputContent else message
                        )
                    }
                }
            }
            
            // Thinking toggle control - only show if there's thinking content
            if (thinkingContent.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ComponentStyles.smallPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show thinking tokens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isThinkingExpanded,
                        onCheckedChange = { 
                            isThinkingExpanded = it
                            viewModel.updateShowThinkingTokens(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Formats thinking content to make it more readable
 */
private fun formatThinkingContent(content: String): String {
    return content
        .replace(Regex("([.!?])([A-Z])"), "$1\n$2") // Add line breaks after sentences
        .replace(Regex("([.!?])([a-z])"), "$1 $2") // Add spaces after sentences
        .replace(Regex("([a-z])([A-Z])"), "$1 $2") // Add spaces between camelCase
        .replace(Regex("([a-z])([0-9])"), "$1 $2") // Add spaces between letters and numbers
        .replace(Regex("([0-9])([a-zA-Z])"), "$1 $2") // Add spaces between numbers and letters
        .replace(Regex("\\s+"), " ") // Normalize multiple spaces
        .trim()
}