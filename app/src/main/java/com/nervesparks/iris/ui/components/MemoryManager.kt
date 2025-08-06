package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernCard
import com.nervesparks.iris.ui.theme.PrimaryButton

@Composable
fun MemoryManager(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(ComponentStyles.defaultSpacing)
        ) {
            Text(
                text = "Memory Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
            
            Text(
                text = "Clear conversation history and free up memory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
            
            PrimaryButton(
                onClick = { viewModel.clear() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Chat History")
            }
        }
    }
} 