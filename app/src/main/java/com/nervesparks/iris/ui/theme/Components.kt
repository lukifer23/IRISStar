package com.nervesparks.iris.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

// Common component styles for consistent UI across the app

object ComponentStyles {
    
    // Card styles
    val defaultCardShape = RoundedCornerShape(12.dp)
    val smallCardShape = RoundedCornerShape(8.dp)
    val largeCardShape = RoundedCornerShape(16.dp)
    val extraLargeCardShape = RoundedCornerShape(20.dp)
    val pillShape = RoundedCornerShape(20.dp)
    
    // Button styles
    val primaryButtonShape = RoundedCornerShape(8.dp)
    val secondaryButtonShape = RoundedCornerShape(8.dp)
    val iconButtonShape = RoundedCornerShape(8.dp)
    val pillButtonShape = RoundedCornerShape(20.dp)
    
    // Text field styles
    val textFieldShape = RoundedCornerShape(8.dp)
    val chatInputShape = RoundedCornerShape(20.dp)
    
    // Modal and dialog styles
    val modalShape = RoundedCornerShape(16.dp)
    val dialogShape = RoundedCornerShape(12.dp)
    
    // Spacing system
    val defaultPadding = 16.dp
    val smallPadding = 8.dp
    val largePadding = 24.dp
    val extraLargePadding = 32.dp
    
    val defaultSpacing = 12.dp
    val smallSpacing = 6.dp
    val largeSpacing = 20.dp
    val extraLargeSpacing = 32.dp
    
    // Elevation system
    val defaultElevation = 2.dp
    val smallElevation = 1.dp
    val largeElevation = 4.dp
    val modalElevation = 8.dp
    
    // Border system
    val defaultBorderWidth = 1.dp
    val thickBorderWidth = 2.dp
    
    // Animation durations
    val shortAnimationDuration = 150
    val defaultAnimationDuration = 300
    val longAnimationDuration = 500
    
    // Icon sizes
    val smallIconSize = 16.dp
    val defaultIconSize = 24.dp
    val largeIconSize = 32.dp
    val extraLargeIconSize = 48.dp
}

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ComponentStyles.primaryButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        content()
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ComponentStyles.secondaryButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        content()
    }
}

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = ComponentStyles.defaultCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = ComponentStyles.defaultCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = ComponentStyles.textFieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ModernIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        content()
    }
}

// New themed components using semantic colors
@Composable
fun ThemedModalSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = ComponentStyles.modalShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.semanticModalBackground()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.modalElevation)
    ) {
        content()
    }
}

@Composable
fun ThemedModalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = ComponentStyles.dialogShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.semanticModalSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.largeElevation)
    ) {
        content()
    }
}

@Composable
fun ThemedLoadingSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = ComponentStyles.defaultCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.semanticLoadingBackground()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.defaultElevation)
    ) {
        content()
    }
}

@Composable
fun ThemedDownloadSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = ComponentStyles.defaultCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.semanticDownloadSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.defaultElevation)
    ) {
        content()
    }
}

@Composable
fun ThemedAccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ComponentStyles.primaryButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.semanticModalAccent(),
            contentColor = MaterialTheme.colorScheme.semanticTextInverse(),
            disabledContainerColor = MaterialTheme.colorScheme.semanticDisabled(),
            disabledContentColor = MaterialTheme.colorScheme.semanticTextDisabled()
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = ComponentStyles.defaultElevation,
            pressedElevation = ComponentStyles.largeElevation
        )
    ) {
        content()
    }
}

@Composable
fun ThemedSuccessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ComponentStyles.primaryButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.semanticSuccess(),
            contentColor = MaterialTheme.colorScheme.semanticTextInverse(),
            disabledContainerColor = MaterialTheme.colorScheme.semanticDisabled(),
            disabledContentColor = MaterialTheme.colorScheme.semanticTextDisabled()
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = ComponentStyles.defaultElevation,
            pressedElevation = ComponentStyles.largeElevation
        )
    ) {
        content()
    }
}

@Composable
fun ThemedWarningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ComponentStyles.primaryButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.semanticWarning(),
            contentColor = MaterialTheme.colorScheme.semanticTextInverse(),
            disabledContainerColor = MaterialTheme.colorScheme.semanticDisabled(),
            disabledContentColor = MaterialTheme.colorScheme.semanticTextDisabled()
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = ComponentStyles.defaultElevation,
            pressedElevation = ComponentStyles.largeElevation
        )
    ) {
        content()
    }
}

@Composable
fun ThemedChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = ComponentStyles.chatInputShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
} 