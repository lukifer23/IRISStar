package com.nervesparks.iris.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.nervesparks.iris.R
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.ui.theme.*

/**
 * PHASE 3.2: Theme Settings Screen - Complete dark theme system
 * Allows users to customize theme preferences and switch between modes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit = {},
    userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository.getInstance(LocalContext.current),
    themeViewModel: ThemeViewModel = viewModel(
        factory = ThemeViewModelFactory(userPreferencesRepository, LocalContext.current)
    )
) {
    val themeState = rememberThemeState(themeViewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Settings") },
                navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ThemeModeSection(themeViewModel, themeState)
            }

            item {
                ThemeFeaturesSection(themeViewModel, themeState)
            }

            item {
                ThemePreviewSection(themeState)
            }

            item {
                ThemeActionsSection(themeViewModel)
            }
        }
    }
}

@Composable
private fun ThemeModeSection(
    themeViewModel: ThemeViewModel,
    themeState: ThemeState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SurfaceShapes.card
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Choose how the app adapts to light and dark themes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    ThemeModeOption(
                        mode = mode,
                        isSelected = themeState.themeMode == mode,
                        onSelect = { themeViewModel.setThemeMode(mode) }
                    )
                }
            }

                androidx.compose.material3.Text(
                    text = "Current: ${themeViewModel.getCurrentThemeDisplayName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
        }
    }
}

@Composable
private fun ThemeModeOption(
    mode: ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (title, subtitle, icon) = when (mode) {
        ThemeMode.SYSTEM -> Triple(
            "System",
            "Follows your device's theme",
            Icons.Default.Settings
        )
        ThemeMode.LIGHT -> Triple(
            "Light",
            "Always use light theme",
            Icons.Default.LightMode
        )
        ThemeMode.DARK -> Triple(
            "Dark",
            "Always use dark theme",
            Icons.Default.DarkMode
        )
    }

    Surface(
        onClick = onSelect,
        shape = InteractiveShapes.button,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ThemeFeaturesSection(
    themeViewModel: ThemeViewModel,
    themeState: ThemeState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SurfaceShapes.card
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Theme Features",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            ThemeFeatureToggle(
                title = "Dynamic Colors",
                subtitle = "Use colors from your wallpaper (Material You)",
                checked = themeState.dynamicColors,
                onCheckedChange = { themeViewModel.setDynamicColors(it) }
            )

            ThemeFeatureToggle(
                title = "High Contrast",
                subtitle = "Increase contrast for better visibility",
                checked = themeState.highContrast,
                onCheckedChange = { themeViewModel.setHighContrast(it) }
            )

            ThemeFeatureToggle(
                title = "Material You",
                subtitle = "Use Android 12+ Material You features",
                checked = themeState.materialYou,
                onCheckedChange = { themeViewModel.setMaterialYou(it) }
            )
        }
    }
}

@Composable
private fun ThemeFeatureToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun ThemePreviewSection(themeState: ThemeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SurfaceShapes.card
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Theme Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Preview components
            Surface(
                shape = SurfaceShapes.card,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp, 4.dp)
                    ) {}

                    Text(
                        text = "Primary Color",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = InteractiveShapes.button,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(
                            text = "Button",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Surface(
                    shape = SurfaceShapes.card,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Surface",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeActionsSection(themeViewModel: ThemeViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { themeViewModel.toggleTheme() },
            modifier = Modifier.fillMaxWidth(),
            shape = InteractiveShapes.button
        ) {
            Text("Quick Theme Toggle")
        }

        OutlinedButton(
            onClick = { themeViewModel.resetToDefaults() },
            modifier = Modifier.fillMaxWidth(),
            shape = InteractiveShapes.button
        ) {
            Text("Reset to Defaults")
        }
    }
}
