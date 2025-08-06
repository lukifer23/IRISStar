package com.nervesparks.iris.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.ModernCard
import kotlinx.coroutines.launch

data class BenchmarkState(
    val isRunning: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val results: List<String> = emptyList(),
    val error: String? = null
)

@Composable
fun BenchMarkScreen(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var state by remember { mutableStateOf(BenchmarkState()) }
    var tokensPerSecond by remember { mutableStateOf(0.0) }

    val deviceInfo = buildDeviceInfo(viewModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ComponentStyles.defaultPadding)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "Benchmark Information",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = ComponentStyles.defaultPadding)
        )

        // Device Info Card
        ModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = ComponentStyles.defaultPadding)
        ) {
            Column(
                modifier = Modifier.padding(ComponentStyles.defaultPadding)
            ) {
                deviceInfo.lines().forEach { line ->
                    Text(line, modifier = Modifier.padding(vertical = ComponentStyles.smallPadding))
                }
            }
        }
        val context = LocalContext.current
        // Benchmark Button

        PrimaryButton(
            modifier = Modifier.padding(vertical = ComponentStyles.smallPadding),
            onClick = {
                if(viewModel.loadedModelName.value == ""){
                    Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                }
                else{
                    state = state.copy(showConfirmDialog = true)
                }
            },
            enabled = !state.isRunning,
        ) {
            Text(if (state.isRunning) "Benchmarking..." else "Start Benchmark", color = MaterialTheme.colorScheme.onPrimary)
        }

        // Progress Indicator
        if (state.isRunning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(ComponentStyles.defaultPadding)
            ) {
                CircularProgressIndicator()
                Text(
                    "Benchmarking in progress...",
                    modifier = Modifier.padding(top = ComponentStyles.smallPadding)
                )
            }
        }

        // Results Section
        if (state.results.isNotEmpty()) {
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ComponentStyles.defaultPadding)
            ) {
                Column(modifier = Modifier.padding(ComponentStyles.defaultPadding)) {
                    Text(
                        "Benchmark Results",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = ComponentStyles.smallPadding)
                    )
                    state.results.forEach { result ->
                        Text(
                            result,
                            modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                        )
                    }
                }
            }
        }

        // Token Per Second Speed Display
        Text(
            text = if (viewModel.tokensPerSecondsFinal > 0) {
                "Tokens per second: %.2f".format(viewModel.tokensPerSecondsFinal)
            } else {
                "Calculating tokens per second..."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(ComponentStyles.defaultPadding)
        )

        // Error Display
        state.error?.let { error ->
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(ComponentStyles.defaultPadding)
            )
        }
    }

    // Confirmation Dialog
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                state = state.copy(showConfirmDialog = false)
            },
            title = { Text("Benchmarking Notice") },
            text = { Text("This process will 30 seconds to 1 minute. Do you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        state = state.copy(
                            showConfirmDialog = false,
                            isRunning = true,
                            results = emptyList(),
                            error = null
                        )
                        scope.launch {
                            try {
                                viewModel.myCustomBenchmark()

                                // Update tokens per second after benchmarking
                                state = state.copy(
                                    results = viewModel.tokensList.toList() // Fetch tokens collected
                                )
                            } catch (e: Exception) {
                                state = state.copy(
                                    error = "Error: ${e.message}"
                                )
                            } finally {
                                state = state.copy(isRunning = false)
                            }
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state = state.copy(showConfirmDialog = false)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}



private fun buildDeviceInfo(viewModel: MainViewModel): String {
    return buildString {
        append("Device: ${Build.MODEL}\n")
        append("Android: ${Build.VERSION.RELEASE}\n")
        append("Processor: ${Build.HARDWARE}\n")
        append("Available Threads: ${Runtime.getRuntime().availableProcessors()}\n")
        append("Current Model: ${viewModel.loadedModelName.value ?: "N/A"}\n")
        append("User Threads: ${viewModel.user_thread}")
    }
}
