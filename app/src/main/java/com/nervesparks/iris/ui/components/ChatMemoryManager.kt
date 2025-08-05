package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernCard
import com.nervesparks.iris.ui.theme.PrimaryButton

@Composable
fun ChatMemoryManager(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.loadMemories()
    }
    val memories = viewModel.memories
    ModernCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(ComponentStyles.defaultPadding),
            verticalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)
        ) {
            Text(
                text = "Stored Memories",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (memories.isEmpty()) {
                Text(
                    text = "No memories stored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                memories.forEach { memory ->
                    Text(
                        text = "Chat ${'$'}{memory.chatId}: ${'$'}{memory.summary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            PrimaryButton(
                onClick = { viewModel.clearMemories() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Memories")
            }
        }
    }
}
