package com.nervesparks.iris.ui.components
import com.nervesparks.iris.ui.theme.Spacing

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
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(Spacing.s)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Spacing.s))

            // Compact metrics layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactMetricItem(
                    label = "TPS",
                    value = String.format("%.1f", state.tps),
                    color = if (state.isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                CompactMetricItem(
                    label = "TTFT",
                    value = if (state.ttft > 0) "${state.ttft}ms" else "N/A",
                    color = if (state.ttft > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                CompactMetricItem(
                    label = "Memory",
                    value = "${state.memoryUsage}MB",
                    color = MaterialTheme.colorScheme.secondary
                )
                CompactMetricItem(
                    label = "Context",
                    value = "${state.contextLimit}/${state.maxContextLimit}",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Generation progress
            if (state.isGenerating) {
                Spacer(modifier = Modifier.height(6.dp))
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
private fun CompactMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
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