package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText as CoreMarkdownText

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    CoreMarkdownText(
        markdown = markdown,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyMedium
    )
}
