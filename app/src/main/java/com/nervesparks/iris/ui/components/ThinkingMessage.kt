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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

@Composable
fun ThinkingMessage(
    message: String,
    viewModel: MainViewModel,
    onLongClick: () -> Unit
) {
    var isThinkingExpanded by remember { mutableStateOf(false) }
    var showThinkingTokens by remember { mutableStateOf(viewModel.showThinkingTokens) }
    
    // Parse via centralised parser
    val (thinkingContent, outputContent) = com.nervesparks.iris.llm.ReasoningParser.parse(message)
    
    // Debug logging
    LaunchedEffect(message) {
        android.util.Log.d("ThinkingMessage", "Message: $message")
        android.util.Log.d("ThinkingMessage", "Thinking content: $thinkingContent")
        android.util.Log.d("ThinkingMessage", "Output content: $outputContent")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a2e)),
        shape = RoundedCornerShape(12.dp),
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
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Reasoning Process",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF00BCD4),
                    fontWeight = FontWeight.Bold
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
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isThinkingExpanded) "Hide thinking" else "Show thinking",
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    )
                }
            }
            
            // Thinking content (collapsible)
            AnimatedVisibility(
                visible = isThinkingExpanded && showThinkingTokens && thinkingContent.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300))
            ) {
                if (thinkingContent.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Thinking Process:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00BCD4),
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = thinkingContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
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
                        checkedThumbColor = Color(0xFF00BCD4),
                        checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

private fun parseThinkingTokens(message: String): Pair<String, String> {
    val tagMatch = Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE).find(message)
    if (tagMatch != null) {
        val reasoning = tagMatch.groupValues[1].trim()
        val answer = message.substring(tagMatch.range.last + 1).trim()
        return Pair(reasoning, answer)
    }
    // Fallback: detect phrase pattern
    val splitter = Regex("(?:The answer is:|Therefore,|Answer:|Result:)", RegexOption.IGNORE_CASE)
    val idx = splitter.find(message)?.range?.first ?: -1
    return if (idx >= 0) {
        val reasoning = message.substring(0, idx).trim()
        val answer = message.substring(idx).replace(splitter, "").trim()
        Pair(reasoning, answer)
    } else if (message.contains("Let me think", ignoreCase = true)) {
        // Treat full message as reasoning when 'Let me think' present
        Pair(message.trim(), "")
    } else {
        Pair("", message.trim())
    }
}

    
/* legacy removed
    var inThinkingMode = false
    var thinkingStarted = false
    
    for (line in lines) {
        val trimmedLine = line.trim()
        
        // Enhanced thinking token detection
        when {
            // Start thinking mode
            trimmedLine.contains("<|im_start|>user") || 
            trimmedLine.contains("<|im_start|>assistant") ||
            trimmedLine.contains("<|user|>") ||
            trimmedLine.contains("<|assistant|>") ||
            trimmedLine.contains("<|im_start|>") ||
            trimmedLine.contains("<think>") -> {
                inThinkingMode = true
                thinkingStarted = true
                thinkingTokens.add(line)
            }
            // End thinking mode
            trimmedLine.contains("<|im_end|>") ||
            trimmedLine.contains("</think>") -> {
                inThinkingMode = false
                thinkingTokens.add(line)
            }
            // Continue thinking mode
            inThinkingMode -> {
                thinkingTokens.add(line)
            }
            // Check for thinking indicators in content
            trimmedLine.startsWith("Let me think") ||
            trimmedLine.contains("Let me think through") ||
            trimmedLine.startsWith("Let me analyze") ||
            trimmedLine.startsWith("I need to") ||
            trimmedLine.startsWith("First,") ||
            trimmedLine.startsWith("Step") ||
            trimmedLine.contains("thinking") ||
            trimmedLine.contains("reasoning") -> {
                if (!thinkingStarted) {
                    inThinkingMode = true
                    thinkingStarted = true
                }
                thinkingTokens.add(line)
            }
            // Final output (after thinking)
            else -> {
                if (line.isNotBlank() && !inThinkingMode) {
                    outputTokens.add(line)
                } else if (inThinkingMode) {
                    thinkingTokens.add(line)
                }
            }
        }
    }
    
    // If we have thinking tokens but no clear output, try to separate
    if (thinkingTokens.isNotEmpty() && outputTokens.isEmpty()) {
        val lastThinkingIndex = thinkingTokens.lastIndex
        val lastThinkingLine = thinkingTokens.getOrNull(lastThinkingIndex) ?: ""
        
        // Look for transition from thinking to output
        if (lastThinkingLine.contains("Here's") || 
            lastThinkingLine.contains("Therefore") ||
            lastThinkingLine.contains("So,") ||
            lastThinkingLine.contains("Answer:") ||
            lastThinkingLine.contains("Result:")) {
            // Move this line and subsequent lines to output
            val transitionIndex = thinkingTokens.indexOfLast { line ->
                line.contains("Here's") || 
                line.contains("Therefore") ||
                line.contains("So,") ||
                line.contains("Answer:") ||
                line.contains("Result:")
            }
            
            if (transitionIndex >= 0) {
                val outputLines = thinkingTokens.subList(transitionIndex, thinkingTokens.size)
                outputTokens.addAll(outputLines)
                thinkingTokens.removeAll(outputLines.toSet())
            }
        }
    }
    
    return Pair(
        thinkingTokens.joinToString("\n"),
        outputTokens.joinToString("\n")
    )
*/ 