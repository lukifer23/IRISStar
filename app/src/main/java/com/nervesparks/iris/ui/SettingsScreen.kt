package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.data.UserPreferencesRepository
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.platform.getCurrentAppLanguage
import com.nervesparks.iris.platform.supportsPerAppLanguagePreferences
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.MemoryManager
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernCard
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.SecondaryButton
import com.nervesparks.iris.ui.theme.ModernTextField
import com.nervesparks.iris.ui.animations.BounceButton
import com.nervesparks.iris.ui.theme.IrisAnimations
import com.nervesparks.iris.security.BiometricAuthenticator
import kotlinx.coroutines.launch
import java.util.Locale
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    generationViewModel: com.nervesparks.iris.viewmodel.GenerationViewModel,
    preferencesRepository: UserPreferencesRepository,
    onModelsScreenButtonClicked: () -> Unit,
    onParamsScreenButtonClicked: () -> Unit,
    onAboutScreenButtonClicked: () -> Unit,
    onBenchMarkScreenButtonClicked: () -> Unit,
    onTemplatesScreenButtonClicked: () -> Unit,
    onModelPerformanceScreenButtonClicked: () -> Unit,
    onThemeSettingsClicked: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Simplified biometric handling for now
    var biometricEnabled by remember { mutableStateOf(false) }
    
    var huggingFaceToken by remember { mutableStateOf(preferencesRepository.huggingFaceToken) }
    var huggingFaceUsername by remember { mutableStateOf(preferencesRepository.huggingFaceUsername) }
    var showToken by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    LaunchedEffect(showSaveSuccess) {
        if (showSaveSuccess) {
            kotlinx.coroutines.delay(2000)
            showSaveSuccess = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(ComponentStyles.defaultPadding),
        verticalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)
    ) {
        // HuggingFace Settings Section
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding),
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)
            ) {
                Text(
                    text = "HuggingFace Integration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Add your HuggingFace credentials to search and download models. You can get your token from huggingface.co/settings/tokens",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Username Field
                ModernTextField(
                    value = huggingFaceUsername,
                    onValueChange = { huggingFaceUsername = it },
                    label = { Text("Username (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )

                // Token Field
                ModernTextField(
                    value = huggingFaceToken,
                    onValueChange = { huggingFaceToken = it },
                    label = { Text("Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) Icons.Default.Star else Icons.Default.Check,
                                contentDescription = if (showToken) "Hide token" else "Show token"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // Save credentials
                            preferencesRepository.huggingFaceToken = huggingFaceToken
                            preferencesRepository.huggingFaceUsername = huggingFaceUsername
                            showSaveSuccess = true
                        }
                    )
                )

                // Save Button
                PrimaryButton(
                    onClick = {
                        preferencesRepository.huggingFaceToken = huggingFaceToken
                        preferencesRepository.huggingFaceUsername = huggingFaceUsername
                        showSaveSuccess = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Credentials")
                }

                // Success Message
                if (showSaveSuccess) {
                    Text(
                        text = "Credentials saved successfully!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (preferencesRepository.hasHuggingFaceCredentials()) 
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = if (preferencesRepository.hasHuggingFaceCredentials()) 
                            "Credentials configured" else "No credentials set",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (preferencesRepository.hasHuggingFaceCredentials()) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        LanguageSettingsCard(
            modifier = Modifier.fillMaxWidth()
        )

        // App Information Section
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding),
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
            ) {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "IRIS - On-Device LLM App",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Version: 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Built with llama.cpp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Help Section
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding),
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
            ) {
                Text(
                    text = "Getting Started",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "1. Add your HuggingFace token above",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "2. Search for models in the search tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "3. Download and use models locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "4. Configure model parameters in the settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Existing Settings Buttons
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding),
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
            ) {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Theme Settings Button with bounce animation
                BounceButton(
                    onClick = onThemeSettingsClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text(
                        text = "Theme Settings",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                    )
                }


                // Models Button
                SecondaryButton(
                    onClick = onModelsScreenButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Models")
                }
                
                // Parameters Button
                SecondaryButton(
                    onClick = onParamsScreenButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Parameters")
                }
                
                // Benchmark Button
                SecondaryButton(
                    onClick = onBenchMarkScreenButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Benchmark")
                }
                
                // About Button
                SecondaryButton(
                    onClick = onAboutScreenButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("About")
                }

                // Templates Button
                SecondaryButton(
                    onClick = onTemplatesScreenButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Templates")
                }

                // Model Performance Button
                SecondaryButton(
                    onClick = onModelPerformanceScreenButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Model Performance")
                }

                // Biometric Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biometric Authentication")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { enabled ->
                            biometricEnabled = enabled
                            coroutineScope.launch {
                                preferencesRepository.setSecurityBiometricEnabled(enabled)
                            }
                        }
                    )
                }
            }
        }
        
        // Hardware Information Section
        ModernCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding),
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
            ) {
                Text(
                    text = "Hardware Acceleration",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Backend Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
                ) {
                    Text(
                        text = "Backend Selection:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Backend Buttons
                    val backends = viewModel.availableBackends.split(",").map { it.trim() }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
                    ) {
                        backends.forEach { backend ->
                            val isSelected = viewModel.currentBackend == backend
                            Button(
                                onClick = { viewModel.selectBackend(backend) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = backend,
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    // Backend Info
                    Text(
                        text = "Current: ${viewModel.currentBackend}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
                
                // Available Backends
                Text(
                    text = "Available Backends: ${viewModel.availableBackends}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // GPU Information
                Column(verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)) {
                    Text(
                        text = "GPU Info: ${viewModel.gpuInfo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val offText = if (generationViewModel.offloadedLayers >= 0 && generationViewModel.totalLayers > 0) {
                        "Offload: ${generationViewModel.offloadedLayers}/${generationViewModel.totalLayers}"
                    } else {
                        "Offload: n/a"
                    }
                    Text(
                        text = offText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Adreno GPU Status
                if (viewModel.isAdrenoGpu) {
                    Text(
                        text = "✓ Adreno GPU detected - OpenCL acceleration available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "ℹ CPU-only mode - No GPU acceleration detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Refresh Button
                SecondaryButton(
                    onClick = { viewModel.detectHardwareCapabilities() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh Hardware Detection")
                }
            }
        }
        
        // Memory Management Section
        MemoryManager(
            viewModel = viewModel,
            modifier = Modifier.padding(top = ComponentStyles.defaultPadding)
        )
    }
}

@Composable
private fun LanguageSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentLanguageTag = remember { getCurrentAppLanguage(context) }
    val currentLanguageLabel = remember(currentLanguageTag) {
        currentLanguageTag?.let { tag ->
            try {
                val locale = Locale.forLanguageTag(tag)
                locale.getDisplayName(locale)
            } catch (e: Exception) {
                Timber.w(e, "Unable to format language tag: $tag")
                tag
            }
        } ?: context.getString(R.string.language_settings_system_default)
    }

    ModernCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(ComponentStyles.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
        ) {
            Text(
                text = stringResource(R.string.language_settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.language_settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.language_settings_current, currentLanguageLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LanguageRestartNotice(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LanguageRestartNotice(modifier: Modifier = Modifier) {
    val restartRequired = !supportsPerAppLanguagePreferences()
    val messageRes = if (restartRequired) {
        R.string.language_restart_required
    } else {
        R.string.language_restart_not_required
    }
    val messageColor = if (restartRequired) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodySmall,
        color = messageColor,
        modifier = modifier.testTag("languageRestartNotice")
    )
}
