package com.nervesparks.iris.ui.components

import android.graphics.BitmapFactory
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.nervesparks.iris.MainViewModel

@Composable
fun DocumentManager(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val documentText = remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val text = when (context.contentResolver.getType(it)) {
                "application/pdf" -> {
                    val pdfReader = PdfReader(context.contentResolver.openInputStream(it))
                    val pdfDocument = PdfDocument(pdfReader)
                    val numPages = pdfDocument.numberOfPages
                    val stringBuilder = StringBuilder()
                    for (i in 1..numPages) {
                        stringBuilder.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
                    }
                    pdfDocument.close()
                    stringBuilder.toString()
                }
                "image/jpeg", "image/png" -> {
                    viewModel.sendImage(it)
                    null
                }
                else -> {
                    context.contentResolver.openInputStream(it)?.bufferedReader().use { reader -> reader?.readText() }
                }
            }
            if (text != null) {
                documentText.value = text
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
        documentText.value?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.indexDocument(text) }) {
                Text("Embed Document")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.summarizeDocument(text) }) {
                Text("Summarize Document")
            }
        }
    }
}
