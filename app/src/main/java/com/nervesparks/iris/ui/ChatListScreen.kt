package com.nervesparks.iris.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Context
import timber.log.Timber
import android.content.Intent
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.viewmodel.ModelViewModel
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.repository.ChatStats
import com.nervesparks.iris.ui.components.ModernTopAppBar
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernIconButton
import com.nervesparks.iris.ui.theme.ModernTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class FilterType {
    ALL, TODAY, WEEK, MONTH
}

enum class SortBy {
    LAST_MODIFIED, TITLE, MESSAGE_COUNT
}

private fun getStartOfDay(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun getStartOfWeek(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun getStartOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun exportChatAsMarkdown(chat: Chat, stats: ChatStats?): String {
    val sb = StringBuilder()
    sb.append("# ${chat.title}\n\n")
    sb.append("Created: ${formatDate(chat.created)}\n")
    sb.append("Last Modified: ${formatDate(chat.updated)}\n\n")

    val totalMessages = stats?.totalMessages ?: 0
    val userMessages = stats?.userMessages ?: 0
    val assistantMessages = stats?.assistantMessages ?: 0
    val averageResponse = stats?.averageResponseTime ?: 0L
    val totalTokens = stats?.totalTokens ?: 0

    sb.append("## Statistics\n")
    sb.append("- Total messages: $totalMessages\n")
    sb.append("- User messages: $userMessages\n")
    sb.append("- Assistant messages: $assistantMessages\n")
    sb.append("- Estimated tokens: $totalTokens\n")
    if (averageResponse > 0) {
        sb.append("- Average response time: ${formatDuration(averageResponse)}\n")
    } else {
        sb.append("- Average response time: N/A\n")
    }
    sb.append("\n")

    return sb.toString()
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    return if (durationMs < 1000) {
        "$durationMs ms"
    } else {
        String.format(Locale.getDefault(), "%.1f s", durationMs / 1000.0)
    }
}

private suspend fun exportAllChatsAsMarkdown(
    context: Context,
    extFilesDir: File?,
    chats: List<Chat>,
    stats: Map<Long, ChatStats>
) {
    try {
        val exportDir = File(extFilesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        chats.forEach { chat ->
            val fileName = "${chat.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.md"
            val file = File(exportDir, fileName)

            file.writeText(exportChatAsMarkdown(chat, stats[chat.id]))

            // Share the file
            shareFile(context, file, "text/markdown")
        }

        // Show success message (you could add a toast here)
    } catch (e: Exception) {
        // Show error message (you could add a toast here)
        Timber.e(e, "Failed to export chats")
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Chat Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Export Chat"))
    } catch (e: Exception) {
        Timber.e(e, "Failed to share file")
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    modelViewModel: ModelViewModel,
    onChatSelected: (Long) -> Unit,
    onNewChat: () -> Unit,
    onMenuClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)
    val chats by viewModel.chats.collectAsState(initial = emptyList())
    val chatStatsMap by viewModel.chatStats.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    var sortBy by remember { mutableStateOf(SortBy.LAST_MODIFIED) }

    // Enhanced filtering logic
    val filteredChats = chats.filter { chat ->
        val matchesSearch = searchQuery.isEmpty() ||
            chat.title.contains(searchQuery, ignoreCase = true)
            // TODO: Re-enable message content search when message loading is implemented

        val matchesFilter = when (filterType) {
            FilterType.ALL -> true
            FilterType.TODAY -> chat.updated >= getStartOfDay()
            FilterType.WEEK -> chat.updated >= getStartOfWeek()
            FilterType.MONTH -> chat.updated >= getStartOfMonth()
        }

        matchesSearch && matchesFilter
    }.let { filtered ->
        when (sortBy) {
            SortBy.LAST_MODIFIED -> filtered.sortedByDescending { it.updated }
            SortBy.TITLE -> filtered.sortedBy { it.title }
            SortBy.MESSAGE_COUNT -> filtered.sortedByDescending { chatStatsMap[it.id]?.totalMessages ?: 0 }
        }
    }

    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Chats",
                onMenuClick = onMenuClick,
                onModelClick = { showModelDropdown = true },
                currentModel = viewModel.loadedModelName.value,
                availableModels = modelViewModel.availableModels.map { it["name"] ?: "" },
                showModelDropdown = showModelDropdown,
                onModelDropdownDismiss = { showModelDropdown = false },
                viewModel = viewModel,
                extFilesDir = extFilesDir,
                actions = {
                    if (chats.isNotEmpty()) {
                        // Filter button
                        ModernIconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Filter and Sort"
                            )
                        }

                        // Export all chats button
                        ModernIconButton(onClick = {
                            scope.launch {
                                exportAllChatsAsMarkdown(context, extFilesDir, chats, chatStatsMap)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export All Chats"
                            )
                        }

                        // Delete all chats button
                        ModernIconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete All Chats"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        // Filter Menu
        DropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false }
        ) {
            // Filter Section
            DropdownMenuItem(
                text = { Text("Filter by time") },
                onClick = { },
                enabled = false
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "All chats",
                        color = if (filterType == FilterType.ALL)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    filterType = FilterType.ALL
                    showFilterMenu = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Today",
                        color = if (filterType == FilterType.TODAY)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    filterType = FilterType.TODAY
                    showFilterMenu = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "This week",
                        color = if (filterType == FilterType.WEEK)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    filterType = FilterType.WEEK
                    showFilterMenu = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "This month",
                        color = if (filterType == FilterType.MONTH)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    filterType = FilterType.MONTH
                    showFilterMenu = false
                }
            )

            // Sort Section
            DropdownMenuItem(
                text = { Text("Sort by") },
                onClick = { },
                enabled = false
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Last modified",
                        color = if (sortBy == SortBy.LAST_MODIFIED)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    sortBy = SortBy.LAST_MODIFIED
                    showFilterMenu = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Title",
                        color = if (sortBy == SortBy.TITLE)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    sortBy = SortBy.TITLE
                    showFilterMenu = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        "Message count",
                        color = if (sortBy == SortBy.MESSAGE_COUNT)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    sortBy = SortBy.MESSAGE_COUNT
                    showFilterMenu = false
                }
            )
        }
        
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
            ModernTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.defaultPadding),
                singleLine = true
            )

            if (filteredChats.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (chats.isEmpty()) "No chats yet." else "No chats found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(filteredChats, key = { it.id }) { chat ->
                        val stats = chatStatsMap[chat.id]
                        ChatRow(
                            chat = chat,
                            stats = stats,
                            onClick = { onChatSelected(chat.id) },
                            onRename = { title -> scope.launch { viewModel.renameChat(chat, title) } },
                            onDelete = { scope.launch { viewModel.deleteChat(chat) } },
                            onExport = {
                                scope.launch {
                                    val exportDir = File(extFilesDir, "exports")
                                    if (!exportDir.exists()) {
                                        exportDir.mkdirs()
                                    }
                                    val fileName = "${chat.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.md"
                                    val file = File(exportDir, fileName)
                                    file.writeText(exportChatAsMarkdown(chat, stats))
                                    shareFile(context, file, "text/markdown")
                                }
                            },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
    
    // Delete All Chats Dialog
    if (showDeleteAllDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteAllDialog = false }) {
            com.nervesparks.iris.ui.theme.ThemedModalCard {
                Column(Modifier.padding(ComponentStyles.defaultPadding)) {
                    Text("Delete All Chats", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(ComponentStyles.smallPadding))
                    Text("Are you sure you want to delete all chats? This action cannot be undone.")
                    Spacer(Modifier.height(ComponentStyles.defaultPadding))
                    Row(horizontalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)) {
                        TextButton(onClick = { showDeleteAllDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        TextButton(onClick = {
                            scope.launch {
                                chats.forEach { chat -> viewModel.deleteChat(chat) }
                            }
                            showDeleteAllDialog = false
                        }, modifier = Modifier.weight(1f)) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(
    chat: Chat,
    stats: ChatStats?,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(chat.title) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ComponentStyles.defaultPadding, vertical = ComponentStyles.smallPadding)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(ComponentStyles.defaultPadding))
            Column(Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm").format(Date(chat.updated)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (stats != null) {
                    Text(
                        text = "${stats.totalMessages} messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Export") }, onClick = {
                        showMenu = false
                        onExport()
                    })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = {
                        showMenu = false
                        showRename = true
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        showMenu = false
                        onDelete()
                    })
                }
            }
        }
    }

    if (showRename) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showRename = false }) {
            com.nervesparks.iris.ui.theme.ThemedModalCard {
                Column(Modifier.padding(ComponentStyles.defaultPadding)) {
                    Text("Rename chat", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(ComponentStyles.smallPadding))
                    ModernTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true)
                    Spacer(Modifier.height(ComponentStyles.defaultPadding))
                    Row(horizontalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)) {
                        TextButton(onClick = { showRename = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        TextButton(onClick = {
                            if (newTitle.isNotBlank()) onRename(newTitle.trim())
                            showRename = false
                        }, modifier = Modifier.weight(1f)) { Text("Save") }
                    }
                }
            }
        }
    }
}