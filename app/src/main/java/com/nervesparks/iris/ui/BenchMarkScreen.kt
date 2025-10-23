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
import androidx.compose.foundation.layout.Arrangement
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.ModernCard
import com.nervesparks.iris.ui.components.ModelSelectionModal
import kotlinx.coroutines.launch
import com.nervesparks.iris.viewmodel.ModelViewModel

data class BenchmarkState(
    val isRunning: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val showComparativeDialog: Boolean = false,
    val results: List<String> = emptyList(),
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchMarkScreen(viewModel: MainViewModel, modelViewModel: ModelViewModel, benchmarkViewModel: com.nervesparks.iris.viewmodel.BenchmarkViewModel) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var state by remember { mutableStateOf(BenchmarkState()) }
    var tokensPerSecond by remember { mutableStateOf(0.0) }

    val deviceInfo = buildDeviceInfo(viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Benchmark", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { innerPadding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
        // Benchmark Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PrimaryButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = ComponentStyles.smallPadding),
                onClick = {
                    if(viewModel.loadedModelName.value == ""){
                        Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        state = state.copy(showConfirmDialog = true)
                    }
                },
                enabled = !state.isRunning && !benchmarkViewModel.isBenchmarkRunning,
            ) {
                Text(if (state.isRunning) "Benchmarking..." else "Standard Benchmark", color = MaterialTheme.colorScheme.onPrimary)
            }
            
            PrimaryButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = ComponentStyles.smallPadding),
                onClick = {
                    benchmarkViewModel.showBenchmarkModelSelection()
                },
                enabled = !state.isRunning && !benchmarkViewModel.isBenchmarkRunning,
            ) {
                Text(if (benchmarkViewModel.isBenchmarkRunning) "Testing..." else "CPU vs GPU Test", color = MaterialTheme.colorScheme.onPrimary)
            }
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
            text = if (benchmarkViewModel.tokensPerSecond > 0) {
                "Tokens per second: %.2f".format(benchmarkViewModel.tokensPerSecond)
            } else {
                "Calculating tokens per second..."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(ComponentStyles.defaultPadding)
        )
        
        // Comparative Benchmark Results
        benchmarkViewModel.benchmarkResults.takeIf { it.isNotEmpty() }?.let { results ->
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ComponentStyles.defaultPadding)
            ) {
                Column(modifier = Modifier.padding(ComponentStyles.defaultPadding)) {
                    Text(
                        "CPU vs GPU Performance Test",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = ComponentStyles.smallPadding)
                    )
                    
                    // CPU Results (safe extraction)
                    Text(
                        "CPU Performance:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = ComponentStyles.smallPadding)
                    )
                    val cpuTps = (results["cpu_tokens_per_sec"] as? Number)?.toDouble() ?: 0.0
                    val cpuDurMs = (results["cpu_duration_ms"] as? Number)?.toLong() ?: 0L
                    val cpuTokens = (results["cpu_tokens_generated"] as? Number)?.toLong() ?: 0L
                    Text("Tokens/sec: %.2f".format(cpuTps))
                    Text("Duration: ${cpuDurMs}ms")
                    Text("Tokens generated: $cpuTokens")

                    // GPU Results (safe extraction)
                    val gpuAvailable = (results["gpu_available"] as? Boolean) == true
                    if (gpuAvailable) {
                        Text(
                            "GPU Performance:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = ComponentStyles.defaultPadding)
                        )
                        val gpuTps = (results["gpu_tokens_per_sec"] as? Number)?.toDouble() ?: 0.0
                        val gpuDurMs = (results["gpu_duration_ms"] as? Number)?.toLong() ?: 0L
                        val gpuTokens = (results["gpu_tokens_generated"] as? Number)?.toLong() ?: 0L
                        Text("Tokens/sec: %.2f".format(gpuTps))
                        Text("Duration: ${gpuDurMs}ms")
                        Text("Tokens generated: $gpuTokens")

                        // Speedup (safe extraction)
                        val speedup = (results["speedup"] as? Number)?.toDouble() ?: 0.0
                        val speedupPercentage = (results["speedup_percentage"] as? Number)?.toDouble() ?: 0.0
                        Text(
                            "Performance Improvement:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = ComponentStyles.defaultPadding)
                        )
                        Text(
                            "%.1fx faster (%.1f%% improvement)".format(speedup, speedupPercentage),
                            color = if (speedup > 1.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    } else {
                        val gpuError = results["gpu_error"]?.toString() ?: "Unknown"
                        Text(
                            "GPU not available: $gpuError",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = ComponentStyles.defaultPadding)
                        )
                    }
                }
            }
        }

        // Error Display
        state.error?.let { error ->
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(ComponentStyles.defaultPadding)
            )
        }
            }
        }
    }

    // Confirmation Dialog
    if (state.showConfirmDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { state = state.copy(showConfirmDialog = false) }) {
            com.nervesparks.iris.ui.theme.ThemedModalCard {
                Column(Modifier.padding(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding)) {
                    Text("Benchmarking Notice", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.smallPadding))
                    Text("This process will 30 seconds to 1 minute. Do you want to continue?")
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding))
                    Row(horizontalArrangement = Arrangement.spacedBy(com.nervesparks.iris.ui.theme.ComponentStyles.defaultSpacing)) {
                        TextButton(onClick = { state = state.copy(showConfirmDialog = false) }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        TextButton(onClick = {
                            state = state.copy(showConfirmDialog = false, isRunning = true, results = emptyList(), error = null)
                            scope.launch {
                                try {
                                    viewModel.myCustomBenchmark()
                                    state = state.copy(results = viewModel.tokensList.toList())
                                } catch (e: Exception) {
                                    state = state.copy(error = "Error: ${e.message}")
                                } finally {
                                    state = state.copy(isRunning = false)
                                }
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Start") }
                    }
                }
            }
        }
    }
    
    // Comparative Benchmark Dialog
    if (state.showComparativeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { state = state.copy(showComparativeDialog = false) }) {
            com.nervesparks.iris.ui.theme.ThemedModalCard {
                Column(Modifier.padding(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding)) {
                    Text("CPU vs GPU Performance Test", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.smallPadding))
                    Text("This will test the same model on both CPU and GPU backends to compare performance. Takes about 10-15 seconds. Continue?")
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding))
                    Row(horizontalArrangement = Arrangement.spacedBy(com.nervesparks.iris.ui.theme.ComponentStyles.defaultSpacing)) {
                        TextButton(onClick = { state = state.copy(showComparativeDialog = false) }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        TextButton(onClick = {
                            state = state.copy(showComparativeDialog = false)
                            scope.launch {
                                try { viewModel.runComparativeBenchmark() } catch (e: Exception) { state = state.copy(error = "Error: ${e.message}") }
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Start Test") }
                    }
                }
            }
        }
    }
    
    // Model Selection Modal for Benchmark
    BenchmarkModelSelectionModal(viewModel, modelViewModel, benchmarkViewModel)
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

// Model Selection Modal for Benchmark
@Composable
fun BenchmarkModelSelectionModal(viewModel: MainViewModel, modelViewModel: ModelViewModel, benchmarkViewModel: com.nervesparks.iris.viewmodel.BenchmarkViewModel) {
    if (benchmarkViewModel.showModelSelection) {
        ModelSelectionModal(
            viewModel = viewModel,
            modelViewModel = modelViewModel,
            benchmarkViewModel = benchmarkViewModel,
            onDismiss = { benchmarkViewModel.hideBenchmarkModelSelection() },
            onNavigateToModels = { /* Navigate to models screen */ },
            isForBenchmark = true
        )
    }
}
