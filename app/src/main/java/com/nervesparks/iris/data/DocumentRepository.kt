package com.nervesparks.iris.data

import com.nervesparks.iris.data.db.Document
import com.nervesparks.iris.data.db.DocumentDao
import com.nervesparks.iris.data.exceptions.ValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.sqrt

class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {
    suspend fun addDocument(text: String, embedding: List<Float>) = withContext(Dispatchers.IO) {
        // Validate input parameters
        if (text.isBlank()) {
            throw ValidationException("Document text cannot be blank")
        }
        
        if (text.length > 10000) {
            throw ValidationException("Document text too long (max 10000 characters)")
        }
        
        if (embedding.isEmpty()) {
            throw ValidationException("Document embedding cannot be empty")
        }
        
        if (embedding.size > 4096) {
            throw ValidationException("Document embedding too large (max 4096 dimensions)")
        }
        
        documentDao.insertDocument(Document(text = text, embedding = embedding))
    }

    suspend fun topKSimilar(embedding: List<Float>, k: Int): List<Document> = withContext(Dispatchers.IO) {
        // Validate input parameters
        if (embedding.isEmpty()) {
            throw ValidationException("Query embedding cannot be empty")
        }
        
        if (embedding.size > 4096) {
            throw ValidationException("Query embedding too large (max 4096 dimensions)")
        }
        
        if (k <= 0) {
            throw ValidationException("Invalid k value: $k")
        }
        
        if (k > 100) {
            throw ValidationException("k value too large (max 100)")
        }
        
        val docs = documentDao.getAllDocuments()
        docs.sortedByDescending { cosineSimilarity(it.embedding, embedding) }.take(k)
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val size = minOf(a.size, b.size)
        var dot = 0.0
        var magA = 0.0
        var magB = 0.0
        for (i in 0 until size) {
            dot += (a[i] * b[i])
            magA += (a[i] * a[i])
            magB += (b[i] * b[i])
        }
        val denom = sqrt(magA) * sqrt(magB)
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }
}
