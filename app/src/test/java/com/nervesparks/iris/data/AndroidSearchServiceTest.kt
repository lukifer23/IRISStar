package com.nervesparks.iris.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.db.Document
import com.nervesparks.iris.data.db.DocumentDao
import com.nervesparks.iris.llm.EmbeddingService
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSearchServiceTest {

    private class FakeDocumentDao : DocumentDao {
        val docs = mutableListOf<Document>()
        override suspend fun insertDocument(document: Document): Long {
            val id = (docs.size + 1).toLong()
            docs.add(document.copy(id = id))
            return id
        }

        override suspend fun getAllDocuments(): List<Document> = docs.toList()
    }

    private lateinit var context: Context
    private lateinit var documentDao: FakeDocumentDao
    private lateinit var documentRepository: DocumentRepository
    private lateinit var embeddingService: EmbeddingService
    private lateinit var androidSearchService: AndroidSearchService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        documentDao = FakeDocumentDao()
        documentRepository = DocumentRepository(documentDao)
        embeddingService = mockk()
        androidSearchService = AndroidSearchService(context, documentRepository, embeddingService)
    }

    @Test
    fun searchDocumentsReturnsScoredResults() = runTest {
        val embedding = listOf(0.5f, 0.25f, 0.75f)
        documentDao.docs.clear()
        documentDao.docs.add(
            Document(
                id = 1,
                text = "Kotlin coroutines guide\nLearn how to use coroutines effectively in your Android apps.",
                embedding = embedding
            )
        )
        coEvery { embeddingService.embed("kotlin coroutines") } returns embedding

        val results = androidSearchService.searchDocuments("kotlin coroutines")

        assertEquals(1, results.size)
        val result = results.first()
        assertEquals("document://1", result.url)
        assertEquals("Local Document", result.source)
        assertTrue(result.snippet.contains("Kotlin coroutines"))
        assertTrue(result.confidence != null && result.confidence!! > 0.99f)
    }

    @Test
    fun searchDocumentsReturnsEmptyWhenNoDocuments() = runTest {
        documentDao.docs.clear()
        coEvery { embeddingService.embed("missing content") } returns listOf(1f, 0f, 0f)

        val results = androidSearchService.searchDocuments("missing content")

        assertTrue(results.isEmpty())
    }

    @Test
    fun searchDocumentsHandlesValidationErrorsGracefully() = runTest {
        coEvery { embeddingService.embed("invalid") } returns emptyList()

        val results = androidSearchService.searchDocuments("invalid")

        assertTrue(results.isEmpty())
    }
}
