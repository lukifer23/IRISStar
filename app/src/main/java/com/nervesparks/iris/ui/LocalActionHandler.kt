package com.nervesparks.iris.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.nervesparks.iris.MainViewModel

class LocalActionHandler(private val context: Context) {
    fun handleQuickAction(action: String?, viewModel: MainViewModel) {
        when (action) {
            "create_images" -> {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            "latest_news" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://news.google.com"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            "cartoon_style" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=cartoon+style"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
        viewModel.clearLastQuickAction()
    }

    fun handleAttachmentAction(action: String?, viewModel: MainViewModel) {
        when (action) {
            "camera" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            "photos" -> {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            "files" -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Select File"))
            }
        }
        viewModel.clearLastAttachmentAction()
    }
}

