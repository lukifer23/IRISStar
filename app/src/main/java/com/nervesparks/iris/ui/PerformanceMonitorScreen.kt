package com.nervesparks.iris.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.performance.MemoryMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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

    // Update memory stats periodically with adaptive refresh rate
    LaunchedEffect(Unit) {
        var lastStats = memoryStats
        while (isActive) {
            val newStats = memoryMonitor.getMemoryStats()

            // Only update if stats have changed significantly to reduce unnecessary recompositions
            if (newStats.memoryUsagePercent != lastStats.memoryUsagePercent ||
                newStats.isLowMemory != lastStats.isLowMemory) {
                memoryStats = newStats
                lastStats = newStats
            }

            // Adaptive refresh rate: faster updates when memory usage is high
            val delayMillis = when {
                newStats.memoryUsagePercent > 80f -> 1000L  // 1 second for high usage
                newStats.memoryUsagePercent > 60f -> 1500L  // 1.5 seconds for medium usage
                else -> 3000L  // 3 seconds for normal usage
            }

            delay(delayMillis)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Monitor") }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Memory Usage",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Memory:")
                                Text("${memoryStats.totalMemory / (1024 * 1024)} MB")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Used Memory:")
                                Text("${memoryStats.usedMemory / (1024 * 1024)} MB")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Available Memory:")
                                Text("${memoryStats.availableMemory / (1024 * 1024)} MB")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Usage:")
                                Text("%.1f%%".format(memoryStats.memoryUsagePercent))
                            }
                        }

                        LinearProgressIndicator(
                            progress = (memoryStats.memoryUsagePercent / 100f).toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "System Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Low Memory Warning: ${if (memoryStats.isLowMemory) "Yes" else "No"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}