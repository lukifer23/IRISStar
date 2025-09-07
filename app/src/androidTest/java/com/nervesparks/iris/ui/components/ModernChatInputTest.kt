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
    fun cameraHandlerTriggered() {
        var called = false
        composeTestRule.setContent {
            ModernChatInput(
                value = "",
                onValueChange = {},
                onSend = {},
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
                onVoiceClick = {},
                onFilesClick = { called = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Attach").performClick()
        composeTestRule.onNodeWithText("Files").performClick()
        assertTrue(called)
    }
}
