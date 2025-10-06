package com.nervesparks.iris.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.nervesparks.iris.MainViewModel

@Composable
fun DocumentManager(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        errorMessage.value = null
        uri?.let {
            try {
                when (context.contentResolver.getType(it) ?: "") {
                    "application/pdf" -> {
                        val pdfReader = PdfReader(context.contentResolver.openInputStream(it))
                        val pdfDocument = PdfDocument(pdfReader)
                        val numPages = pdfDocument.numberOfPages
                        val stringBuilder = StringBuilder()
                        for (i in 1..numPages) {
                            stringBuilder.append(
                                PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i))
                            )
                        }
                        pdfDocument.close()
                        val text = stringBuilder.toString()
                        viewModel.indexDocument(text)
                        viewModel.summarizeDocument(text)
                    }
                    "image/jpeg", "image/png" -> {
                        viewModel.sendImage(it)
                    }
                    "text/plain" -> {
                        val text = context.contentResolver.openInputStream(it)?.bufferedReader()
                            .use { reader -> reader?.readText() }
                        if (text != null) {
                            viewModel.indexDocument(text)
                            viewModel.summarizeDocument(text)
                        } else {
                            errorMessage.value = "Failed to read document"
                        }
                    }
                    else -> {
                        errorMessage.value = "Unsupported file format"
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to process document: ${e.message}"
            }
        }
    }

    Column(modifier = modifier) {
        Button(onClick = { launcher.launch("*/*") }) {
            Text("Import Document")
        }
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Import Image")
        }
        if (viewModel.isDocumentIndexing) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Indexing document…")
        }

        viewModel.documentIndexingSuccess?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.primary)
        }

        viewModel.documentIndexingError?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }

        errorMessage.value?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }
    }
}
