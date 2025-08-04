package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel

@Composable
fun PerformanceMonitor(viewModel: MainViewModel) {
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
                color = Color.White,
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
                    value = String.format("%.1f", viewModel.tps),
                    unit = "tokens/s",
                    color = if (viewModel.isGenerating) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
                MetricItem(
                    label = "TTFT",
                    value = if (viewModel.ttft > 0) viewModel.ttft.toString() else "N/A",
                    unit = "ms",
                    color = if (viewModel.ttft > 0) Color(0xFF2196F3) else Color(0xFF9E9E9E)
                )
                MetricItem(
                    label = "Latency",
                    value = if (viewModel.latency > 0) viewModel.latency.toString() else "N/A",
                    unit = "ms",
                    color = if (viewModel.latency > 0) Color(0xFFFF9800) else Color(0xFF9E9E9E)
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
                    value = viewModel.memoryUsage.toString(),
                    unit = "MB",
                    color = Color(0xFFE91E63)
                )
                MetricItem(
                    label = "Context",
                    value = "${viewModel.contextLimit}/${viewModel.maxContextLimit}",
                    unit = "tokens",
                    color = Color(0xFF9C27B0)
                )
                MetricItem(
                    label = "Generated",
                    value = viewModel.tokensGenerated.toString(),
                    unit = "tokens",
                    color = if (viewModel.isGenerating) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
            }
            
            // Generation status
            if (viewModel.isGenerating) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF2E7D32)
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
            color = Color.White.copy(alpha = 0.7f)
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
            color = Color.White.copy(alpha = 0.5f)
        )
    }
} 