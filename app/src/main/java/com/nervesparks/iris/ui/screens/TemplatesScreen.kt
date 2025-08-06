package com.nervesparks.iris.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.Template

@Composable
fun TemplatesScreen(
    viewModel: MainViewModel
) {
    val templates = viewModel.templates
    var showDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<Template?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Templates")
        Button(onClick = {
            editingTemplate = null
            showDialog = true
        }) {
            Text("Add Template")
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(templates) { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        template.name,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        editingTemplate = template
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { viewModel.deleteTemplate(template) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember(editingTemplate) { mutableStateOf(editingTemplate?.name ?: "") }
        var content by remember(editingTemplate) { mutableStateOf(editingTemplate?.content ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingTemplate == null) "New Template" else "Edit Template") },
            text = {
                Column {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingTemplate == null) {
                        viewModel.addTemplate(Template(name = name, content = content))
                    } else {
                        viewModel.editTemplate(editingTemplate!!.copy(name = name, content = content))
                    }
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
