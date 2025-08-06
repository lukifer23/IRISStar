package com.nervesparks.iris.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.MainViewModel
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
        composeRule.setContent {
            ChatListScreen(viewModel = viewModel, onChatSelected = {}, onNewChat = {})
        }
        composeRule.onNodeWithText("No chats yet.").assertIsDisplayed()
    }
}

