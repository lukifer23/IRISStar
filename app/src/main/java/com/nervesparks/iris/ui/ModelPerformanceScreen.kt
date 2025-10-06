package com.nervesparks.iris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nervesparks.iris.llm.ModelComparison
import com.nervesparks.iris.llm.ModelPerformanceTracker
import com.nervesparks.iris.viewmodel.ModelViewModel

/**
 * Screen for displaying model performance metrics and comparisons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPerformanceScreen(
    onNavigateBack: () -> Unit = {},
    modelViewModel: ModelViewModel = viewModel()
) {
    val performanceComparison by modelViewModel.getPerformanceComparison().collectAsState()
    val bestModel = remember(performanceComparison) { modelViewModel.getBestPerformingModel() }

    ModelPerformanceScreenContent(
        performanceComparison = performanceComparison,
        bestModel = bestModel,
        onClearData = { modelViewModel.clearPerformanceData() },
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun ModelPerformanceScreenContent(
    performanceComparison: List<ModelComparison>,
    bestModel: ModelPerformanceTracker.ModelMetrics?,
    onClearData: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Performance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onClearData) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Data")
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
            ModelPerformanceHeader(bestModel)
            ModelPerformanceList(performanceComparison)
        }
    }
}

private fun LazyListScope.ModelPerformanceHeader(bestModel: ModelPerformanceTracker.ModelMetrics?) {
    bestModel?.let { model ->
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Best Performing Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = model.modelName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Performance Score: ${model.calculatePerformanceScore().toInt()}/100",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = model.getRecommendation(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun LazyListScope.ModelPerformanceList(performanceComparison: List<ModelComparison>) {
    if (performanceComparison.isNotEmpty()) {
        item {
            Text(
                text = "Model Comparison",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(performanceComparison) { comparison ->
            PerformanceComparisonCard(comparison)
        }
    } else {
        item {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No performance data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Load models and use them to see performance metrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceComparisonCard(comparison: ModelComparison) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comparison.modelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${comparison.performanceScore.toInt()}/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = comparison.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Avg TPS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${comparison.averageTokensPerSecond.toInt()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Memory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${comparison.averageMemoryUsage / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${comparison.totalSessions}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelPerformanceScreenPreview() {
    val comparisonItems = listOf(
        ModelComparison(
            modelName = "Orion-7B",
            performanceScore = 82.5,
            recommendation = "Excellent performance",
            averageTokensPerSecond = 45.0,
            averageMemoryUsage = 512L * 1024 * 1024,
            totalSessions = 12,
            lastUsed = System.currentTimeMillis()
        ),
        ModelComparison(
            modelName = "Nebula-3B",
            performanceScore = 68.0,
            recommendation = "Good performance",
            averageTokensPerSecond = 28.0,
            averageMemoryUsage = 384L * 1024 * 1024,
            totalSessions = 7,
            lastUsed = System.currentTimeMillis()
        )
    )

    val bestModel = ModelPerformanceTracker.ModelMetrics(
        modelName = "Orion-7B",
        modelPath = "models/orion-7b.bin",
        totalSessions = 12,
        averageLoadTime = 700,
        averageInferenceTime = 250,
        averageTokensPerSecond = 45.0,
        averageMemoryUsage = 512L * 1024 * 1024,
        bestTokensPerSecond = 60.0,
        worstTokensPerSecond = 30.0,
        totalTokensGenerated = 25_000,
        lastUsed = System.currentTimeMillis(),
        backendUsed = "gpu",
        configuration = ModelPerformanceTracker.ModelConfiguration(
            temperature = 0.7f,
            topP = 0.9f,
            topK = 40,
            threadCount = 4,
            gpuLayers = 8,
            contextLength = 2048,
            chatFormat = "CHATML"
        ),
        deviceInfo = ModelPerformanceTracker.DeviceInfo(
            deviceModel = "Pixel 8",
            androidVersion = "14",
            availableMemory = 8_000L,
            cpuCores = 8,
            hasGpu = true
        )
    )

    ModelPerformanceScreenContent(
        performanceComparison = comparisonItems,
        bestModel = bestModel,
        onClearData = {},
        onNavigateBack = {}
    )
}
