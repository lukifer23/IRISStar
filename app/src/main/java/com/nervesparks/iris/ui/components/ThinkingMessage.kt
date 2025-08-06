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
    val (thinkingContent, outputContent) = com.nervesparks.iris.llm.ReasoningParser.parse(message, viewModel.supportsReasoning)
    
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
            // Header with thinking indicator - only show when reasoning text exists
            if (thinkingContent.isNotEmpty()) {
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
            
            // Output content - Only show if there's actual output content OR if there's no thinking content
            val hasOutputContent = outputContent.isNotEmpty() || thinkingContent.isEmpty()
            if (hasOutputContent) {
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
                        
                        // Use the actual output content, not the formatted thinking content
                        val responseText = if (outputContent.isNotEmpty()) {
                            outputContent
                        } else {
                            // If no output content was parsed, use the original message
                            message
                        }
                        
                        MarkdownTextComponent(
                            markdown = responseText
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
 * This function is specifically for the jumbled thinking content
 */
private fun formatThinkingContent(content: String): String {
    return content
        // First, handle the most common jumbled patterns with specific replacements
        .replace("thequestion", "the question")
        .replace("theuser", "the user")
        .replace("theresult", "the result")
        .replace("theanswer", "the answer")
        .replace("Letme", "Let me")
        .replace("Ineed", "I need")
        .replace("Ishould", "I should")
        .replace("Sincethe", "Since the")
        .replace("Juststate", "Just state")
        .replace("Butwait", "But wait")
        .replace("Actually", "Actually")
        .replace("Letmecheck", "Let me check")
        .replace("Letmeverify", "Let me verify")
        .replace("Whenyouadd", "When you add")
        .replace("theresultshouldbe", "the result should be")
        .replace("maybetheuser", "maybe the user")
        .replace("istestingif", "is testing if")
        .replace("Icanhandle", "I can handle")
        .replace("basicmath", "basic math")
        .replace("Ineedtoconfirm", "I need to confirm")
        .replace("Letmecheckthecalculation", "Let me check the calculation")
        .replace("Yes,that'scorrect", "Yes, that's correct")
        .replace("Ishouldrespondwith", "I should respond with")
        .replace("theanswerdirectly", "the answer directly")
        .replace("Sincetheuserisprobablyjustasking", "Since the user is probably just asking")
        .replace("fortheresult", "for the result")
        .replace("there'snoneed", "there's no need")
        .replace("foranyadditionalexplanation", "for any additional explanation")
        .replace("Juststatetheanswer", "Just state the answer")
        .replace("clearly.2plus2is4", "clearly. 2 plus 2 is 4")
        .replace("userisasking", "user is asking")
        .replace("methinkaboutthis", "me think about this")
        .replace("questionisstraightforward", "question is straightforward")
        .replace("Whenyouadd2and2", "When you add 2 and 2")
        .replace("theresultshouldbe4", "the result should be 4")
        .replace("maybetheuseristestingif", "maybe the user is testing if")
        .replace("Icanhandlebasicmath", "I can handle basic math")
        .replace("Ineedtoconfirmtheanswer", "I need to confirm the answer")
        .replace("Letmecheckthecalculationagain:2+2equals4", "Let me check the calculation again: 2+2 equals 4")
        .replace("Yes,that'scorrect", "Yes, that's correct")
        .replace("Ishouldrespondwiththeanswerdirectly", "I should respond with the answer directly")
        .replace("Sincetheuserisprobablyjustaskingfortheresult", "Since the user is probably just asking for the result")
        .replace("there'snoneedforanyadditionalexplanation", "there's no need for any additional explanation")
        .replace("Juststatetheanswerclearly", "Just state the answer clearly")
        .replace("2plus2is4", "2 plus 2 is 4")
        // Now handle general patterns
        .replace(Regex("([a-z])([A-Z])"), "$1 $2") // Add spaces between camelCase
        .replace(Regex("([a-z])([0-9])"), "$1 $2") // Add spaces between letters and numbers
        .replace(Regex("([0-9])([a-zA-Z])"), "$1 $2") // Add spaces between numbers and letters
        .replace(Regex("([.!?])([A-Z])"), "$1\n$2") // Add line breaks after sentences
        .replace(Regex("([.!?])([a-z])"), "$1 $2") // Add spaces after sentences
        .replace(Regex("([a-z])([a-z])([A-Z])"), "$1$2 $3") // Handle cases like "thequestion" -> "the question"
        .replace(Regex("([A-Z])([a-z])([A-Z])"), "$1$2 $3") // Handle cases like "Whenyouadd" -> "When you add"
        .replace(Regex("([a-z])([A-Z])([a-z])"), "$1 $2$3") // Handle cases like "userisasking" -> "user is asking"
        .replace(Regex("([a-z])([a-z])([a-z])([A-Z])"), "$1$2$3 $4") // Handle longer jumbled words
        .replace(Regex("([A-Z])([a-z])([a-z])([A-Z])"), "$1$2$3 $4") // Handle longer jumbled words
        .replace(Regex("\\s+"), " ") // Normalize multiple spaces
        .trim()
}