package com.nervesparks.iris.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.nervesparks.iris.ui.components.ModernTopAppBar
import org.junit.Rule
import org.junit.Test
import io.mockk.mockk

class TopAppBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun topAppBar_displaysTitleAndModel() {
        val vm = mockk<com.nervesparks.iris.MainViewModel>(relaxed = true)
        composeRule.setContent {
            MaterialTheme {
                ModernTopAppBar(
                    title = "Chat",
                    onMenuClick = {},
                    onModelClick = {},
                    currentModel = "ModelA",
                    availableModels = listOf("ModelA"),
                    showModelDropdown = false,
                    onModelDropdownDismiss = {},
                    viewModel = vm,
                    extFilesDir = null
                )
            }
        }
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("ModelA").assertIsDisplayed()
    }
}
