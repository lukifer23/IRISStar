package com.nervesparks.iris.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.nervesparks.iris.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageRestartNoticeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.S_V2)
    fun showsRestartPromptOnLegacyDevices() {
        composeRule.setContent {
            MaterialTheme {
                LanguageRestartNotice()
            }
        }

        val expectedText = composeRule.activity.getString(R.string.language_restart_required)
        composeRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun showsImmediateApplyMessageOnModernDevices() {
        composeRule.setContent {
            MaterialTheme {
                LanguageRestartNotice()
            }
        }

        val expectedText = composeRule.activity.getString(R.string.language_restart_not_required)
        composeRule.onNodeWithText(expectedText).assertIsDisplayed()
    }
}
