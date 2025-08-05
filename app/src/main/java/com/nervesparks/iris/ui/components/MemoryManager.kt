package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

@Composable
fun MemoryManager(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var memoryUsage by remember { mutableStateOf(0L) }
    var isOptimizing by remember { mutableStateOf(false) }
    
    // Update memory usage periodically
    LaunchedEffect(Unit) {
        while (true) {
            try {
                memoryUsage = viewModel.getMemoryUsage()
            } catch (e: Exception) {
                // Handle error silently
            }
            kotlinx.coroutines.delay(5000) // Update every 5 seconds
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Memory Management",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Memory Usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${memoryUsage / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isOptimizing = true
                            viewModel.optimizeMemory()
                            isOptimizing = false
                        },
                        enabled = !isOptimizing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isOptimizing) "Optimizing..." else "Optimize",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (viewModel.isModelLoaded()) {
                        Button(
                            onClick = { viewModel.unloadCurrentModel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Unload Model",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
} 