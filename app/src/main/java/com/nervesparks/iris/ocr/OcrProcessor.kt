package com.nervesparks.iris.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Utility for performing OCR using ML Kit's on-device text recognition.
 */
object OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    suspend fun processImage(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return result.text
    }

    suspend fun process(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri)
        return if (type == "application/pdf") {
            processPdf(context, uri)
        } else {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            result.text
        }
    }

    private suspend fun processPdf(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val pageText = processImage(bitmap)
                    sb.append(pageText).append('\n')
                }
            }
        }
        return sb.toString()
    }
}

