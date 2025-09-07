package com.nervesparks.iris.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.*
import com.nervesparks.iris.ui.theme.IrisStarTheme
import com.nervesparks.iris.ui.ChatListScreen
import java.io.File
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues

// Navigation functions removed - using main implementations from separate files
// ModernChatListScreen, ModernChatScreen, ModernSettingsScreen moved to their own files

/**
 * Settings section with title and items
 */
@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
    onToggle: (SettingsItem, Boolean) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = ComponentStyles.defaultPadding)
        )
        
        items.forEach { item ->
            SettingsItemRow(
                item = item,
                onToggle = { isEnabled -> onToggle(item, isEnabled) }
            )
        }
    }
}

/**
 * Individual settings item row
 */
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ComponentStyles.smallPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(ComponentStyles.defaultIconSize)
        )
        
        Spacer(modifier = Modifier.width(ComponentStyles.largeSpacing))
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (item.isToggle) {
            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        } else if (item.isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ComponentStyles.smallIconSize + ComponentStyles.smallSpacing)
            )
        }
    }
}

/**
 * Data class for settings items
 */
data class SettingsItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val isSelected: Boolean = false,
    val isToggle: Boolean = false,
    val isEnabled: Boolean = false
) 