package com.nervesparks.iris.llm

import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.exceptions.ValidationException

suspend fun performDocumentIndexing(
    text: String,
    embeddingService: EmbeddingService,
    documentRepository: DocumentRepository
) {
    val sanitizedText = text.trim()
    if (sanitizedText.isBlank()) {
        throw ValidationException("Document is empty")
    }

    val embedding = embeddingService.embed(sanitizedText)
    documentRepository.addDocument(sanitizedText, embedding)
}
