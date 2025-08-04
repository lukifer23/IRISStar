package com.nervesparks.iris.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.nervesparks.iris.ui.components.MessageBubble
import org.junit.Rule
import org.junit.Test

class MessageBubbleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun messageBubble_showsMessage() {
        composeRule.setContent {
            MaterialTheme {
                MessageBubble(message = "Hello", isUser = true)
            }
        }
        composeRule.onNodeWithText("Hello").assertIsDisplayed()
    }
}
