package com.nervesparks.iris.data

import com.nervesparks.iris.data.db.Document
import com.nervesparks.iris.data.db.DocumentDao
import com.nervesparks.iris.data.exceptions.ValidationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DocumentRepositoryTest {

    private class FakeDocumentDao : DocumentDao {
        val docs = mutableListOf<Document>()
        override suspend fun insertDocument(document: Document): Long {
            val id = (docs.size + 1).toLong()
            docs.add(document.copy(id = id))
            return id
        }
        override suspend fun getAllDocuments(): List<Document> = docs.toList()
    }

    private val dao = FakeDocumentDao()
    private val repository = DocumentRepository(dao)

    @Test
    fun addDocumentRejectsBlankText() {
        assertThrows(ValidationException::class.java) {
            runBlocking { repository.addDocument("", listOf(0.1f)) }
        }
    }

    @Test
    fun addDocumentStoresValidDocument() = runBlocking {
        repository.addDocument("hello", listOf(0.1f, 0.2f))
        assertEquals(1, dao.docs.size)
    }

    @Test
    fun topKSimilarReturnsMostSimilar() = runBlocking {
        dao.docs.clear()
        repository.addDocument("A", listOf(1f, 0f))
        repository.addDocument("B", listOf(0f, 1f))
        repository.addDocument("C", listOf(1f, 1f))

        val result = repository.topKSimilar(listOf(1f, 0f), 2)
        assertEquals(listOf("A", "C"), result.map { it.text })
    }
}

