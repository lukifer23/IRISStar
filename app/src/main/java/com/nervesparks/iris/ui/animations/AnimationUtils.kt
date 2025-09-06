package com.nervesparks.iris.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.ui.theme.IrisAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PHASE 3.3: Enhanced Animation Utilities
 * Sophisticated animations and effects for modern UI interactions
 */

// Enhanced animated visibility with spring physics
@Composable
fun SpringAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = IrisAnimations.ScaleInTransition,
    exit: ExitTransition = IrisAnimations.ScaleOutTransition,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

// Pulsing animation for loading states
@Composable
fun PulsingBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = IrisAnimations.LoadingPulse
    )

    Box(
        modifier = modifier.scale(scale),
        content = { content() }
    )
}

// Shimmer loading effect
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = IrisAnimations.ShimmerAnimation
    )

    Box(
        modifier = modifier.graphicsLayer {
            val shimmerWidth = size.width
            val shimmerOffset = shimmerProgress * shimmerWidth * 2
            clip = true
            translationX = shimmerOffset
        },
        content = { content() }
    )
}

// Bounce animation for buttons and interactive elements
@Composable
fun BounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Reset pressed state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }

    androidx.compose.material3.Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.scale(scale),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

// Staggered animation for lists
@Composable
fun StaggeredColumn(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    staggerDelay: Long = 50L
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(index * staggerDelay)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 4 }
                ),
                exit = fadeOut()
            ) {
                item()
            }
        }
    }
}

// Morphing animation between shapes
@Composable
fun MorphingShape(
    isRounded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isRounded) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius)
    ) {
        content()
    }
}

// Floating action button animation
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = modifier.scale(scale)
    ) {
        content()
    }
}

// Typing indicator animation
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val dotCount = 3
    val animationDelay = 200L

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(dotCount) { index ->
            val infiniteTransition = rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * animationDelay.toInt())
                )
            )

            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                modifier = Modifier.size(6.dp)
            ) {}
        }
    }
}

// Card flip animation
@Composable
fun FlipCard(
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing)
    )

    Box(modifier = modifier) {
        // Front
        androidx.compose.material3.Card(
            modifier = Modifier.graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
        ) {
            if (rotation <= 90f) {
                frontContent()
            }
        }

        // Back
        androidx.compose.material3.Card(
            modifier = Modifier.graphicsLayer {
                rotationY = rotation + 180f
                cameraDistance = 12f * density
            }
        ) {
            if (rotation > 90f) {
                backContent()
            }
        }
    }
}

// Page transition animation
@Composable
fun PageTransition(
    currentPage: Int,
    targetPage: Int,
    modifier: Modifier = Modifier,
    content: @Composable (page: Int) -> Unit
) {
    val direction = if (targetPage > currentPage) 1 else -1

    AnimatedContent(
        targetState = targetPage,
        modifier = modifier,
        transitionSpec = {
            if (direction > 0) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        }
    ) { page ->
        content(page)
    }
}

// Enhanced loading spinner
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        strokeWidth = 3.dp
    )
}

// Success animation with checkmark
@Composable
fun SuccessAnimation(
    show: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = show,
        enter = IrisAnimations.ScaleInTransition,
        exit = IrisAnimations.ScaleOutTransition
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            modifier = modifier.scale(scale)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Success",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// Error shake animation
@Composable
fun ShakeAnimation(
    shake: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shake) {
        if (shake) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 600
                    -10f at 100
                    10f at 200
                    -10f at 300
                    10f at 400
                    0f at 500
                }
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            translationX = shakeOffset.value
        }
    ) {
        content()
    }
}
