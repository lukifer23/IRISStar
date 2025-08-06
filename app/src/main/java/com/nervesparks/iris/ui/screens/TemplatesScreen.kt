package com.nervesparks.iris.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

@Composable
fun TemplatesScreen(
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Templates")
        Button(onClick = { /* TODO: Add new template */ }) {
            Text("Add Template")
        }
    }
}
