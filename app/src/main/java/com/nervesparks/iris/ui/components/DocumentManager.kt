package com.nervesparks.iris.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

@Composable
fun DocumentManager(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val documentText = remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader().use { reader -> reader?.readText() }
            documentText.value = text
        }
    }

    Column(modifier = modifier) {
        Button(onClick = { launcher.launch("text/*") }) {
            Text("Import Document")
        }
        documentText.value?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.indexDocument(text) }) {
                Text("Embed Document")
            }
        }
    }
}
