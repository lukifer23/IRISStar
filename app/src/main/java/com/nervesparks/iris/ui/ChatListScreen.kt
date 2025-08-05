package com.nervesparks.iris.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.ChatRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.ui.theme.IrisStarTheme
import com.nervesparks.iris.ui.components.IrisTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatListScreen(
    viewModel: MainViewModel,
    onChatSelected: (Long) -> Unit,
    onNewChat: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val chats by viewModel.chats.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isEmpty()) {
            chats
        } else {
            chats.filter { chat ->
                chat.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        IrisTopAppBar(
            title = "Chats",
            navigationIcon = Icons.Default.Menu,
            onNavigationClick = onMenuClick,
            actions = {
                IconButton(onClick = onNewChat) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                }
            }
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search chats...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (filteredChats.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isEmpty()) "No chats yet. Tap 'New Chat' to start." else "No chats match your search.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(filteredChats) { chat ->
                    ChatRow(chat,
                        onClick = { onChatSelected(chat.id) },
                        onRename = { title -> scope.launch { viewModel.renameChat(chat, title) } },
                        onDelete = { scope.launch { viewModel.deleteChat(chat) } }
                    )
                }
            }
        }
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("New Chat", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun ChatRow(
    chat: Chat,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(chat.title) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Medium)
                Text(SimpleDateFormat("MMM dd, HH:mm").format(Date(chat.updated)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSecondary)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; showRename = true })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) onRename(newTitle.trim())
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
            title = { Text("Rename chat") },
            text = {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true)
            }
        )
    }
}
