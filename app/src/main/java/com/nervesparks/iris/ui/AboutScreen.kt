package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.ui.theme.ComponentStyles


@Composable
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(ComponentStyles.defaultPadding)
    ) {
        item {
            SectionHeader(text = "Welcome to Iris")
        }
        item {
            Text(
                text = "Iris is an offline Android chat application powered by the llama.cpp framework. Designed to operate entirely offline, it ensures privacy and independence from external servers. Whether you're a developer exploring AI applications or a privacy-conscious user, this app provides a seamless and secure way to experience conversational AI. Please note that the app may occasionally generate inaccurate results.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(ComponentStyles.largePadding))
        }

        item {
            SectionHeader(text = "Features")
        }

        items(features) { feature ->
            FeatureItem(feature = feature)
        }

        item {
            Spacer(modifier = Modifier.height(ComponentStyles.largePadding))
            SectionHeader(text = "FAQs")
        }

        items(faqs) { faq ->
            FaqItem(question = faq.first, answer = faq.second)
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = ComponentStyles.defaultSpacing)
    )
}

@Composable
private fun FeatureItem(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ComponentStyles.smallPadding)
    ) {
        Box(
            modifier = Modifier
                .size(ComponentStyles.defaultIconSize)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(ComponentStyles.smallIconSize)
            )
        }
        Spacer(modifier = Modifier.width(ComponentStyles.defaultSpacing))
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ComponentStyles.smallPadding)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = ComponentStyles.smallPadding)
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = ComponentStyles.extraLargePadding)
        )
    }
}

private val features = listOf(
    "Offline AI Chat",
    "Privacy-First Design",
    "Multiple Model Support",
    "Customizable Parameters",
    "Code Generation",
    "Markdown Support"
)

private val faqs = listOf(
    "How does Iris work?" to "Iris uses the llama.cpp framework to run large language models locally on your device. It downloads model files and processes them entirely offline, ensuring your conversations remain private.",
    "What models are supported?" to "Iris supports GGUF format models from Hugging Face. You can download and use various models like Llama, Mistral, and others that are compatible with llama.cpp.",
    "Is my data private?" to "Yes! All processing happens locally on your device. No data is sent to external servers, ensuring complete privacy and security.",
    "How much storage do I need?" to "Model sizes vary from 1GB to 8GB depending on the model you choose. Make sure you have sufficient storage space before downloading models.",
    "Can I use my own models?" to "Yes, you can download and use any GGUF format model from Hugging Face or other sources that are compatible with llama.cpp."
)
