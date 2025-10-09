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
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.IrisStarTheme
import com.nervesparks.iris.viewmodel.GenerationViewModel

data class PerformanceMonitorState(
    val tps: Double,
    val ttft: Long,
    val latency: Long,
    val memoryUsage: Long,
    val contextLimit: Int,
    val maxContextLimit: Int,
    val tokensGenerated: Int,
    val isGenerating: Boolean,
    val backend: String? = null,
    val offload: Pair<Int,Int>? = null
)

@Composable
fun PerformanceMonitor(generationViewModel: GenerationViewModel, mainViewModel: MainViewModel) {
    PerformanceMonitor(
        state = PerformanceMonitorState(
            tps = generationViewModel.tps,
            ttft = generationViewModel.ttft,
            latency = generationViewModel.latency,
            memoryUsage = generationViewModel.memoryUsage,
            contextLimit = generationViewModel.contextLimit,
            maxContextLimit = generationViewModel.maxContextLimit,
            tokensGenerated = generationViewModel.tokensGenerated,
            isGenerating = generationViewModel.isGenerating,
            backend = mainViewModel.currentBackend,
            offload = (generationViewModel.offloadedLayers to generationViewModel.totalLayers)
        )
    )
}

@Composable
fun PerformanceMonitor(state: PerformanceMonitorState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentStyles.defaultPadding, vertical = ComponentStyles.smallPadding),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.defaultElevation),
        shape = ComponentStyles.smallCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultSpacing)
        ) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))

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
                state.backend?.let { be ->
                    val off = state.offload?.let { (a,b) -> if (a >= 0 && b > 0) " ($a/$b)" else "" } ?: ""
                    CompactMetricItem(
                        label = "Backend",
                        value = be + off,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Generation progress
            if (state.isGenerating) {
                Spacer(modifier = Modifier.height(ComponentStyles.smallSpacing))
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