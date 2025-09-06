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
    val defaultCardShape = LegacyShapes.radiusSm
    val smallCardShape = LegacyShapes.radiusXs
    val largeCardShape = LegacyShapes.radiusMd
    val extraLargeCardShape = LegacyShapes.radiusLg
    val pillShape = LegacyShapes.pill

    // Button styles
    val primaryButtonShape = LegacyShapes.radiusXs
    val secondaryButtonShape = LegacyShapes.radiusXs
    val iconButtonShape = LegacyShapes.radiusXs
    val pillButtonShape = LegacyShapes.pill

    // Text field styles
    val textFieldShape = LegacyShapes.radiusXs
    val chatInputShape = LegacyShapes.pill

    // Modal and dialog styles
    val modalShape = LegacyShapes.radiusMd
    val dialogShape = LegacyShapes.radiusSm
    
    // Spacing system
    val defaultPadding = Dimens.spaceLg
    val smallPadding = Dimens.spaceSm
    val largePadding = Dimens.spaceXl
    val extraLargePadding = Dimens.spaceXxl
    
    val defaultSpacing = Dimens.spaceMd
    val smallSpacing = Dimens.spaceXs
    val largeSpacing = Dimens.spaceLg
    val extraLargeSpacing = Dimens.spaceXxl
    
    // Elevation system
    val defaultElevation = Elevation.level2
    val smallElevation = Elevation.level1
    val largeElevation = Elevation.level3
    val modalElevation = Elevation.level4
    
    // Border system
    val defaultBorderWidth = Dimens.strokeThin
    val thickBorderWidth = Dimens.strokeThick
    
    // Animation durations
    val shortAnimationDuration = Motion.fast
    val defaultAnimationDuration = Motion.normal
    val longAnimationDuration = 500
    
    // Icon sizes
    val smallIconSize = Dimens.iconSm
    val defaultIconSize = Dimens.iconMd
    val largeIconSize = Dimens.iconLg
    val extraLargeIconSize = Dimens.iconXl
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
            containerColor = SemanticColors.ModalBackground
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
            containerColor = SemanticColors.ModalSurface
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
            containerColor = SemanticColors.LoadingBackground
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
            containerColor = SemanticColors.DownloadSurface
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
            containerColor = SemanticColors.ModalAccent,
            contentColor = SemanticColors.TextInverse,
            disabledContainerColor = SemanticColors.Disabled,
            disabledContentColor = SemanticColors.TextDisabled
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
            contentColor = SemanticColors.TextInverse,
            disabledContainerColor = SemanticColors.Disabled,
            disabledContentColor = SemanticColors.TextDisabled
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
            contentColor = SemanticColors.TextInverse,
            disabledContainerColor = SemanticColors.Disabled,
            disabledContentColor = SemanticColors.TextDisabled
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