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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.ui.components.ModernTopAppBar
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ModernIconButton
import com.nervesparks.iris.ui.theme.ModernTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    onChatSelected: (Long) -> Unit,
    onNewChat: () -> Unit,
    onMenuClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)
    val chats by viewModel.chats.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    val filteredChats = chats.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Chats",
                onMenuClick = onMenuClick,
                onModelClick = { showModelDropdown = true },
                currentModel = viewModel.loadedModelName.value,
                availableModels = viewModel.allModels.map { it["name"] ?: "" },
                showModelDropdown = showModelDropdown,
                onModelDropdownDismiss = { showModelDropdown = false },
                viewModel = viewModel,
                extFilesDir = extFilesDir,
                actions = {
                    if (chats.isNotEmpty()) {
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
                        ChatRow(
                            chat = chat,
                            onClick = { onChatSelected(chat.id) },
                            onRename = { title -> scope.launch { viewModel.renameChat(chat, title) } },
                            onDelete = { scope.launch { viewModel.deleteChat(chat) } },
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
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
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
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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