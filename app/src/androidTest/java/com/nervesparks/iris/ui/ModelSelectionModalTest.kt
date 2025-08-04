package com.nervesparks.iris.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.ModelSelectionModal
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import java.io.File

class ModelSelectionModalTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun modal_showsHeader() {
        val vm = mockk<MainViewModel>(relaxed = true)
        every { vm.getAvailableModels(any()) } returns listOf(mapOf("name" to "ModelA"))
        every { vm.loadedModelName } returns mutableStateOf("")
        composeRule.setContent {
            MaterialTheme {
                ModelSelectionModal(viewModel = vm, onDismiss = {})
            }
        }
        composeRule.onNodeWithText("Model Selection").assertIsDisplayed()
    }
}
