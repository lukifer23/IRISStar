package com.nervesparks.iris.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.repository.ChatStats
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun newChatButtonInvokesCallback() {
        val viewModel = mockk<MainViewModel>(relaxed = true)
        every { viewModel.chats } returns MutableStateFlow(emptyList())
        every { viewModel.chatStats } returns MutableStateFlow<Map<Long, ChatStats>>(emptyMap())
        every { viewModel.loadedModelName } returns mutableStateOf("")
        every { viewModel.allModels } returns emptyList()
        var called = false
        composeRule.setContent {
            ChatListScreen(viewModel = viewModel, onChatSelected = {}, onNewChat = { called = true })
        }
        composeRule.onNodeWithContentDescription("New Chat").performClick()
        assertTrue(called)
    }

    @Test
    fun showsEmptyStateWhenNoChats() {
        val viewModel = mockk<MainViewModel>(relaxed = true)
        every { viewModel.chats } returns MutableStateFlow(emptyList())
        every { viewModel.chatStats } returns MutableStateFlow<Map<Long, ChatStats>>(emptyMap())
        every { viewModel.loadedModelName } returns mutableStateOf("")
        every { viewModel.allModels } returns emptyList()
        composeRule.setContent {
            ChatListScreen(viewModel = viewModel, onChatSelected = {}, onNewChat = {})
        }
        composeRule.onNodeWithText("No chats yet.").assertIsDisplayed()
    }

    @Test
    fun sortsByMessageCountWhenSelected() {
        val viewModel = mockk<MainViewModel>(relaxed = true)
        val now = System.currentTimeMillis()
        val chatA = Chat(id = 1, title = "Alpha", created = now, updated = now - 1_000)
        val chatB = Chat(id = 2, title = "Beta", created = now, updated = now - 2_000)
        every { viewModel.chats } returns MutableStateFlow(listOf(chatA, chatB))
        every { viewModel.chatStats } returns MutableStateFlow(
            mapOf(
                chatA.id to ChatStats(totalMessages = 1, userMessages = 1, assistantMessages = 0, totalTokens = 10, averageResponseTime = 0),
                chatB.id to ChatStats(totalMessages = 5, userMessages = 3, assistantMessages = 2, totalTokens = 42, averageResponseTime = 1_200)
            )
        )
        every { viewModel.loadedModelName } returns mutableStateOf("")
        every { viewModel.allModels } returns emptyList()

        composeRule.setContent {
            ChatListScreen(viewModel = viewModel, onChatSelected = {}, onNewChat = {})
        }

        composeRule.onNodeWithContentDescription("Filter and Sort").performClick()
        composeRule.onNodeWithText("Message count").performClick()
        composeRule.waitForIdle()

        val betaNode = composeRule.onNodeWithText("Beta").fetchSemanticsNode()
        val alphaNode = composeRule.onNodeWithText("Alpha").fetchSemanticsNode()

        assertTrue(betaNode.boundsInRoot.top < alphaNode.boundsInRoot.top)
    }
}

