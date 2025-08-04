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
import com.nervesparks.iris.MainViewModel

@Composable
fun ThinkingMessage(
    message: String,
    viewModel: MainViewModel,
    onLongClick: () -> Unit
) {
    var isThinkingExpanded by remember { mutableStateOf(true) } // Start expanded when thinking content is detected
    var showThinkingTokens by remember { mutableStateOf(true) } // Always show thinking tokens
    
    // Parse via centralised parser
    val (thinkingContent, outputContent) = com.nervesparks.iris.llm.ReasoningParser.parse(message)
    
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
        android.util.Log.d("ThinkingMessage", "=== END THINKING DEBUG ===")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with thinking indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Thinking",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Reasoning Process",
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Toggle button
                IconButton(
                    onClick = { isThinkingExpanded = !isThinkingExpanded }
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isThinkingExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 300)
                    )
                    Icon(
                        imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isThinkingExpanded) "Hide thinking" else "Show thinking",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    )
                }
            }
            
            // Thinking content (collapsible) - Always show if thinking content exists
            AnimatedVisibility(
                visible = isThinkingExpanded && thinkingContent.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                android.util.Log.d("ThinkingMessage", "Thinking visibility: expanded=$isThinkingExpanded, showTokens=$showThinkingTokens, hasContent=${thinkingContent.isNotEmpty()}")
                if (thinkingContent.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Thinking Process:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = thinkingContent,
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            
            // Output content
            if (outputContent.isNotEmpty() || thinkingContent.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLongClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0f3460)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Final Answer:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (outputContent.isNotEmpty()) outputContent else message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Thinking toggle control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show thinking tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = showThinkingTokens,
                    onCheckedChange = { 
                        showThinkingTokens = it
                        viewModel.updateShowThinkingTokens(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}