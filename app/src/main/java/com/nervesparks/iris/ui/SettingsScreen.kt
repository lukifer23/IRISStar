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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.MemoryManager
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernCard
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.SecondaryButton
import com.nervesparks.iris.ui.theme.ModernTextField
import com.nervesparks.iris.security.BiometricAuthenticator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    preferencesRepository: UserPreferencesRepository,
    onModelsScreenButtonClicked: () -> Unit,
    onParamsScreenButtonClicked: () -> Unit,
    onAboutScreenButtonClicked: () -> Unit,
    onBenchMarkScreenButtonClicked: () -> Unit,
    onTemplatesScreenButtonClicked: () -> Unit,
) {
    val context = LocalContext.current
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }
    var biometricEnabled by remember { mutableStateOf(preferencesRepository.getSecurityBiometricEnabled()) }
    
    var huggingFaceToken by remember { mutableStateOf(preferencesRepository.getHuggingFaceToken()) }
    var huggingFaceUsername by remember { mutableStateOf(preferencesRepository.getHuggingFaceUsername()) }
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
                            preferencesRepository.setHuggingFaceToken(huggingFaceToken)
                            preferencesRepository.setHuggingFaceUsername(huggingFaceUsername)
                            showSaveSuccess = true
                        }
                    )
                )

                // Save Button
                PrimaryButton(
                    onClick = {
                        preferencesRepository.setHuggingFaceToken(huggingFaceToken)
                        preferencesRepository.setHuggingFaceUsername(huggingFaceUsername)
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

                // Biometric Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biometric Authentication")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = {
                            if (biometricAuthenticator.isBiometricAuthAvailable()) {
                                biometricEnabled = it
                                preferencesRepository.setSecurityBiometricEnabled(it)
                            }
                        }
                    )
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
