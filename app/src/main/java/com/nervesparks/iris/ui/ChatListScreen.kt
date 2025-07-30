package com.nervesparks.iris.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.data.ChatRepository
import com.nervesparks.iris.data.db.Chat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatListScreen(
    onChatSelected: (Long) -> Unit,
    onNewChat: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { ChatRepository.get(context) }
    val chats by repo.observeChats().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        if (chats.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No chats yet. Tap 'New Chat' to start.", color = Color.White.copy(alpha = 0.7f))
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(chats) { chat ->
                    ChatRow(chat,
                        onClick = { onChatSelected(chat.id) },
                        onRename = { title -> scope.launch { repo.renameChat(chat, title) } },
                        onDelete = { scope.launch { repo.deleteChat(chat) } }
                    )
                }
            }
        }
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
        ) {
            Text("New Chat", color = Color.White)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(chat.title, color = Color.White, fontWeight = FontWeight.Medium)
                Text(SimpleDateFormat("MMM dd, HH:mm").format(Date(chat.updated)), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
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