package com.nervesparks.iris.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.performance.MemoryMonitor
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.Shapes
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * PHASE 3.4: Performance Monitor Screen
 * Real-time performance monitoring and optimization dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMonitorScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val memoryMonitor = remember { MemoryMonitor(context) }
    var memoryStats by remember { mutableStateOf(memoryMonitor.getMemoryStats()) }

    // Update memory stats periodically
    LaunchedEffect(Unit) {
        while (true) {
            memoryStats = memoryMonitor.getMemoryStats()
            kotlinx.coroutines.delay(2000) // Update every 2 seconds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Monitor") },
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
                    MemoryUsageCard(memoryStats)
                }

                item {
                    PerformanceMetricsCard(memoryStats, memoryMonitor)
                }

                item {
                    OptimizationRecommendationsCard(memoryStats, memoryMonitor)
                }

                item {
                    SystemInfoCard()
                }
            }
        }
    }
}

@Composable
private fun MemoryUsageCard(memoryStats: MemoryMonitor.MemoryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Memory Usage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Memory usage bar
            LinearProgressIndicator(
                progress = memoryStats.memoryUsagePercent / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    memoryStats.memoryUsagePercent > 90f -> Color.Red
                    memoryStats.memoryUsagePercent > 75f -> Color.Yellow
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            // Memory stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MemoryStat(
                    label = "Used",
                    value = formatBytes(memoryStats.usedMemory)
                )
                MemoryStat(
                    label = "Available",
                    value = formatBytes(memoryStats.availableMemory)
                )
                MemoryStat(
                    label = "Total",
                    value = formatBytes(memoryStats.totalMemory)
                )
            }

            Text(
                text = "${memoryStats.memoryUsagePercent.roundToInt()}% used",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (memoryStats.isLowMemory) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = ComponentStyles.dialogShape
                ) {
                    Text(
                        text = "⚠️ Low memory warning",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PerformanceMetricsCard(
    memoryStats: MemoryMonitor.MemoryStats,
    memoryMonitor: MemoryMonitor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            PerformanceMetric(
                label = "Memory Optimization",
                value = if (memoryMonitor.shouldOptimizeMemory()) "Needed" else "Good",
                color = if (memoryMonitor.shouldOptimizeMemory())
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            PerformanceMetric(
                label = "Memory Pressure",
                value = if (memoryStats.isLowMemory) "High" else "Normal",
                color = if (memoryStats.isLowMemory)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            PerformanceMetric(
                label = "GC Pressure",
                value = "${(memoryStats.usedMemory.toDouble() / memoryStats.totalMemory.toDouble() * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PerformanceMetric(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun OptimizationRecommendationsCard(
    memoryStats: MemoryMonitor.MemoryStats,
    memoryMonitor: MemoryMonitor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Optimization Recommendations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (memoryMonitor.shouldOptimizeMemory()) {
                OptimizationItem(
                    icon = "🧹",
                    title = "Memory Cleanup",
                    description = "Clear cache and temporary data"
                )
            }

            if (memoryStats.isLowMemory) {
                OptimizationItem(
                    icon = "⚡",
                    title = "Reduce Memory Usage",
                    description = "Close unused apps and restart device"
                )
            }

            OptimizationItem(
                icon = "📱",
                title = "App Performance",
                description = "Keep app updated for best performance"
            )
        }
    }
}

@Composable
private fun OptimizationItem(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineSmall
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SystemInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            SystemInfoItem("Android Version", android.os.Build.VERSION.RELEASE)
            SystemInfoItem("API Level", android.os.Build.VERSION.SDK_INT.toString())
            SystemInfoItem("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            SystemInfoItem("Architecture", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
        }
    }
}

@Composable
private fun SystemInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }

    return "%.1f %s".format(value, units[unitIndex])
}
