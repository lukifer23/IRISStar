package com.nervesparks.iris.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.IrisStarTheme

data class PerformanceMonitorState(
    val tps: Double,
    val ttft: Long,
    val latency: Long,
    val memoryUsage: Long,
    val contextLimit: Int,
    val maxContextLimit: Int,
    val tokensGenerated: Int,
    val isGenerating: Boolean
)

@Composable
fun PerformanceMonitor(viewModel: MainViewModel) {
    PerformanceMonitor(
        state = PerformanceMonitorState(
            tps = viewModel.tps,
            ttft = viewModel.ttft,
            latency = viewModel.latency,
            memoryUsage = viewModel.memoryUsage,
            contextLimit = viewModel.contextLimit,
            maxContextLimit = viewModel.maxContextLimit,
            tokensGenerated = viewModel.tokensGenerated,
            isGenerating = viewModel.isGenerating
        )
    )
}

@Composable
fun PerformanceMonitor(state: PerformanceMonitorState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "TPS",
                    value = String.format("%.1f", state.tps),
                    unit = "tokens/s",
                    color = if (state.isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                MetricItem(
                    label = "TTFT",
                    value = if (state.ttft > 0) state.ttft.toString() else "N/A",
                    unit = "ms",
                    color = if (state.ttft > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                MetricItem(
                    label = "Latency",
                    value = if (state.latency > 0) state.latency.toString() else "N/A",
                    unit = "ms",
                    color = if (state.latency > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Memory and context metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Memory",
                    value = state.memoryUsage.toString(),
                    unit = "MB",
                    color = MaterialTheme.colorScheme.secondary
                )
                MetricItem(
                    label = "Context",
                    value = "${state.contextLimit}/${state.maxContextLimit}",
                    unit = "tokens",
                    color = MaterialTheme.colorScheme.tertiary
                )
                MetricItem(
                    label = "Generated",
                    value = state.tokensGenerated.toString(),
                    unit = "tokens",
                    color = if (state.isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Generation status
            if (state.isGenerating) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
        )
    }
}

private fun sampleState() = PerformanceMonitorState(
    tps = 10.5,
    ttft = 200,
    latency = 150,
    memoryUsage = 512,
    contextLimit = 256,
    maxContextLimit = 512,
    tokensGenerated = 300,
    isGenerating = true
)

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun PerformanceMonitorLightPreview() {
    IrisStarTheme(darkTheme = false) {
        PerformanceMonitor(state = sampleState())
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PerformanceMonitorDarkPreview() {
    IrisStarTheme(darkTheme = true) {
        PerformanceMonitor(state = sampleState())
    }
}