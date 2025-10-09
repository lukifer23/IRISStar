package com.nervesparks.iris.viewmodel

import com.nervesparks.iris.data.AndroidSearcher
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.WebSearcher
import com.nervesparks.iris.data.db.Document
import com.nervesparks.iris.data.db.DocumentDao
import com.nervesparks.iris.data.search.SearchResponse
import com.nervesparks.iris.data.search.SearchResult
import com.nervesparks.iris.llm.EmbeddingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var webSearcher: FakeWebSearcher
    private lateinit var androidSearcher: FakeAndroidSearcher
    private lateinit var documentRepository: DocumentRepository
    private lateinit var embeddingService: FakeEmbeddingService

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        embeddingService = FakeEmbeddingService()
        documentRepository = DocumentRepository(
            FakeDocumentDao(
                listOf(
                    Document(id = 1, text = "Kotlin Coroutines Guide", embedding = listOf(1f, 0f)),
                    Document(id = 2, text = "Android search integration", embedding = listOf(0f, 1f))
                ).toMutableList()
            )
        )
        webSearcher = FakeWebSearcher()
        androidSearcher = FakeAndroidSearcher()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun performWebSearch_successCombinesSourcesAndSummarizes() = runTest {
        val viewModel = createViewModel()
        webSearcher.response = SearchResponse(
            success = true,
            results = listOf(
                SearchResult("Result A", "Details about A", "https://a.example", "Web"),
                SearchResult("Result B", "Details about B", "https://b.example", "Web")
            ),
            query = "kotlin"
        )

        viewModel.performWebSearch("kotlin")
        advanceUntilIdle()

        assertFalse(viewModel.webResults.isEmpty())
        assertFalse(viewModel.documentResults.isEmpty())
        assertEquals(viewModel.searchResults.size, viewModel.webResults.size + viewModel.documentResults.size)
        assertNotNull(viewModel.searchSummary)
        assertTrue(viewModel.searchStatusMessage.contains("Found") || viewModel.searchStatusMessage.contains("Key takeaways"))
    }

    @Test
    fun performWebSearch_errorSurfacesMessage() = runTest {
        val viewModel = createViewModel()
        webSearcher.response = SearchResponse(
            success = false,
            error = "network unavailable"
        )

        viewModel.performWebSearch("offline")
        advanceUntilIdle()

        assertEquals("network unavailable", viewModel.searchError)
        assertTrue(viewModel.searchStatusMessage.contains("network unavailable"))
    }

    @Test
    fun searchDocuments_successUpdatesResults() = runTest {
        val viewModel = createViewModel()

        viewModel.searchDocuments("kotlin")
        advanceUntilIdle()

        assertEquals(1, viewModel.documentResults.size)
        assertTrue(viewModel.searchStatusMessage.contains("Found"))
        assertTrue(viewModel.searchResults.any { it.source == "Indexed Document" })
    }

    @Test
    fun searchDocuments_errorSetsSearchError() = runTest {
        embeddingService.shouldThrow = true
        val viewModel = createViewModel()

        viewModel.searchDocuments("kotlin")
        advanceUntilIdle()

        assertNotNull(viewModel.searchError)
        assertTrue(viewModel.searchStatusMessage.contains("Document search failed"))
    }

    @Test
    fun performAndroidSearch_successStoresResults() = runTest {
        val viewModel = createViewModel()
        androidSearcher.response = SearchResponse(
            success = true,
            results = listOf(
                SearchResult("Search Launched", "Opened search", "https://google.com", "Android Browser")
            ),
            query = "compose"
        )

        viewModel.performAndroidSearch("compose")
        advanceUntilIdle()

        assertFalse(viewModel.searchResults.isEmpty())
        assertTrue(viewModel.searchStatusMessage.contains("Opened") || viewModel.searchStatusMessage.contains("search"))
    }

    @Test
    fun performAndroidSearch_errorUpdatesErrorState() = runTest {
        val viewModel = createViewModel()
        androidSearcher.response = SearchResponse(success = false, error = "no browser")

        viewModel.performAndroidSearch("compose")
        advanceUntilIdle()

        assertEquals("no browser", viewModel.searchError)
        assertTrue(viewModel.searchStatusMessage.contains("no browser"))
    }

    private fun createViewModel(): SearchViewModel {
        return SearchViewModel(
            webSearcher,
            androidSearcher,
            documentRepository,
            embeddingService
        )
    }

    private class FakeWebSearcher : WebSearcher {
        var response: SearchResponse = SearchResponse(success = true, results = emptyList())
        override suspend fun searchWeb(query: String): SearchResponse = response
    }

    private class FakeAndroidSearcher : AndroidSearcher {
        var response: SearchResponse = SearchResponse(success = true, results = emptyList())

        override suspend fun launchBrowserSearch(query: String): SearchResponse = response

        override suspend fun launchMultiSearch(query: String): SearchResponse = response

        override fun formatSearchResults(results: List<SearchResult>, query: String): String = ""
    }

    private class FakeEmbeddingService : EmbeddingService {
        var shouldThrow: Boolean = false
        override suspend fun embed(text: String): List<Float> {
            if (shouldThrow) throw IllegalStateException("embedding failed")
            return if (text.contains("kotlin", ignoreCase = true)) {
                listOf(1f, 0f)
            } else {
                listOf(0f, 1f)
            }
        }
    }

    private class FakeDocumentDao(
        private val documents: MutableList<Document>
    ) : DocumentDao {
        override suspend fun insertDocument(document: Document): Long {
            documents.add(document)
            return document.id
        }

        override suspend fun getAllDocuments(): List<Document> = documents.toList()
    }
}
