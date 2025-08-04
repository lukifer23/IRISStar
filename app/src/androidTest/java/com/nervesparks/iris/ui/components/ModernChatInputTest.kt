package com.nervesparks.iris.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernChatInputTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun latestNewsActionTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onAttachmentClick = {},
                onVoiceClick = {},
                onLatestNews = { called = true }
            )
        }

        composeTestRule.onNodeWithText("Latest news").performClick()
        assertTrue(called)
    }

    @Test
    fun createImagesActionTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onAttachmentClick = {},
                onVoiceClick = {},
                onCreateImages = { called = true }
            )
        }

        composeTestRule.onNodeWithText("Create images").performClick()
        assertTrue(called)
    }

    @Test
    fun cartoonStyleActionTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onAttachmentClick = {},
                onVoiceClick = {},
                onCartoonStyle = { called = true }
            )
        }

        composeTestRule.onNodeWithText("Cartoon style").performClick()
        assertTrue(called)
    }

    @Test
    fun cameraHandlerTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onAttachmentClick = {},
                onVoiceClick = {},
                onCameraClick = { called = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Attach").performClick()
        composeTestRule.onNodeWithText("Camera").performClick()
        assertTrue(called)
    }

    @Test
    fun photosHandlerTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onAttachmentClick = {},
                onVoiceClick = {},
                onPhotosClick = { called = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Attach").performClick()
        composeTestRule.onNodeWithText("Photos").performClick()
        assertTrue(called)
    }

    @Test
    fun filesHandlerTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
                onAttachmentClick = {},
                onVoiceClick = {},
                onFilesClick = { called = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Attach").performClick()
        composeTestRule.onNodeWithText("Files").performClick()
        assertTrue(called)
    }
}

