package com.nervesparks.iris.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.db.Chat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import com.nervesparks.iris.ui.util.rememberWindowClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    onChatSelected: (Long) -> Unit,
    onNewChat: () -> Unit,
) {
    val chats by viewModel.chats.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val filteredChats = chats.filter { it.title.contains(searchQuery, ignoreCase = true) }
    val windowClass = rememberWindowClass()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = {
                    if (chats.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
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
        if (windowClass.width == WindowWidthSizeClass.Compact) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ChatListContent(
                    filteredChats = filteredChats,
                    chats = chats,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onChatSelected = onChatSelected,
                    onRename = { chat, title -> scope.launch { viewModel.renameChat(chat, title) } },
                    onDelete = { chat -> scope.launch { viewModel.deleteChat(chat) } }
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ChatListContent(
                        filteredChats = filteredChats,
                        chats = chats,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onChatSelected = onChatSelected,
                        onRename = { chat, title -> scope.launch { viewModel.renameChat(chat, title) } },
                        onDelete = { chat -> scope.launch { viewModel.deleteChat(chat) } }
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a chat", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    
    // Delete All Chats Dialog
    if (showDeleteAllDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteAllDialog = false }) {
            com.nervesparks.iris.ui.theme.ThemedModalCard {
                Column(Modifier.padding(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding)) {
                    Text("Delete All Chats", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.smallPadding))
                    Text("Are you sure you want to delete all chats? This action cannot be undone.")
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding))
                    Row(horizontalArrangement = Arrangement.spacedBy(com.nervesparks.iris.ui.theme.ComponentStyles.defaultSpacing)) {
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
private fun ColumnScope.ChatListContent(
    filteredChats: List<Chat>,
    chats: List<Chat>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onChatSelected: (Long) -> Unit,
    onRename: (Chat, String) -> Unit,
    onDelete: (Chat) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search chats") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                    onRename = { title -> onRename(chat, title) },
                    onDelete = { onDelete(chat) },
                    modifier = Modifier.animateItemPlacement()
                )
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
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
                Column(Modifier.padding(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding)) {
                    Text("Rename chat", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.smallPadding))
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true)
                    Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding))
                    Row(horizontalArrangement = Arrangement.spacedBy(com.nervesparks.iris.ui.theme.ComponentStyles.defaultSpacing)) {
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