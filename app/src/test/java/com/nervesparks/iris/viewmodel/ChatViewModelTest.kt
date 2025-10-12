package com.nervesparks.iris.viewmodel

import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.llm.EmbeddingService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var chatRepository: ChatRepository
    private lateinit var documentRepository: DocumentRepository
    private lateinit var embeddingService: EmbeddingService

    @Before
    fun setUp() {
        chatRepository = mockk(relaxed = true) {
            every { observeChats() } returns flowOf(emptyList())
        }
        documentRepository = mockk(relaxed = true)
        embeddingService = mockk()
    }

    @Test
    fun indexDocument_successfulIndexingUpdatesState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel()
            val content = "Important facts"

            coEvery { embeddingService.embed(content) } returns listOf(0.1f, 0.2f)
            coEvery { documentRepository.addDocument(content, listOf(0.1f, 0.2f)) } returns Unit

            viewModel.indexDocument(content)
            advanceUntilIdle()

            assertFalse(viewModel.isDocumentIndexing)
            assertEquals("Document indexed successfully", viewModel.documentIndexingSuccess)
            assertNull(viewModel.documentIndexingError)
            coVerify { documentRepository.addDocument(content, listOf(0.1f, 0.2f)) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun indexDocument_blankDocumentShowsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel()

            viewModel.indexDocument("   \n   ")
            advanceUntilIdle()

            assertFalse(viewModel.isDocumentIndexing)
            assertEquals("Document is empty", viewModel.documentIndexingError)
            assertNull(viewModel.documentIndexingSuccess)
            coVerify(exactly = 0) { embeddingService.embed(any()) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun indexDocument_repositoryFailureSurfacesError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel()
            val content = "Research notes"

            coEvery { embeddingService.embed(content) } returns listOf(0.4f, 0.5f)
            coEvery { documentRepository.addDocument(content, listOf(0.4f, 0.5f)) } throws ValidationException("Document text too long")

            viewModel.indexDocument(content)
            advanceUntilIdle()

            assertFalse(viewModel.isDocumentIndexing)
            assertEquals("Document text too long", viewModel.documentIndexingError)
            assertNull(viewModel.documentIndexingSuccess)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cleanup_retainsMostRecentMessages() {
        val viewModel = createViewModel()

        repeat(120) { index ->
            viewModel.addMessage("user", "Message $index")
        }

        viewModel.cleanup()

        assertEquals(50, viewModel.messages.size)
        assertEquals("Message 70", viewModel.messages.first()["content"])
        assertEquals("Message 119", viewModel.messages.last()["content"])
    }

    private fun createViewModel(): ChatViewModel {
        return ChatViewModel(chatRepository, documentRepository, embeddingService)
    }
}
