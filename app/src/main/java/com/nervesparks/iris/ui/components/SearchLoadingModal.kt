package com.nervesparks.iris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.SemanticColors
import com.nervesparks.iris.ui.theme.ThemedModalSurface
import kotlinx.coroutines.delay

@Composable
fun SearchLoadingModal(
    message: String,
    searchQuery: String = "",
    onDismiss: () -> Unit
) {
    // Add a minimum display time to ensure users can read the feedback
    var showModal by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        // Show modal for at least 2 seconds to ensure users can read the feedback
        delay(2000)
        showModal = false
        onDismiss()
    }
    
    if (!showModal) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "search_loading")
    
    // Pulsing animation for the search icon
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search_scale"
    )
    
    // Rotating animation for the loading dots
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "search_rotation"
    )
    
    // Bouncing animation for the progress dots
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search_bounce"
    )

    Dialog(onDismissRequest = { 
        showModal = false
        onDismiss()
    }) {
        ThemedModalSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.largePadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search icon with pulsing animation
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(SemanticColors.LoadingAccent()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                // Search query display
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "Searching for:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticColors.TextInverse().copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                    
                    Text(
                        text = "\"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SemanticColors.TextInverse(),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                }
                
                // Main message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SemanticColors.TextInverse(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                // Animated progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val delay = index * 200
                        val dotBounce by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = delay, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_bounce_$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(0.5f + dotBounce * 0.5f)
                                .clip(CircleShape)
                                .background(SemanticColors.LoadingAccent())
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                
                // Progress steps
                SearchProgressSteps()
                
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

@Composable
private fun SearchProgressSteps() {
    val steps = listOf("Querying", "Processing", "Formatting")
    var currentStep by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            currentStep = (currentStep + 1) % steps.size
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        steps.forEachIndexed { index, step ->
            val isActive = index == currentStep
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) SemanticColors.LoadingAccent() 
                            else SemanticColors.TextInverse().copy(alpha = 0.3f)
                        )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) SemanticColors.LoadingAccent() else SemanticColors.TextInverse().copy(alpha = 0.7f),
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
} 