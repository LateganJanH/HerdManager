package com.herdmanager.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [MainActivity].
 * Uses [TestAuthModule] (fake signed-in user) so the main app is shown without Firebase.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private fun waitForNodeWithTag(rule: ComposeContentTestRule, tag: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                rule.onNodeWithTag(tag).assertIsDisplayed()
                return
            } catch (_: Throwable) {
                Thread.sleep(300)
            }
        }
        rule.onNodeWithTag(tag).assertIsDisplayed() // fail with clear message
    }

    /** Type into a field after focusing it; helps ensure clean input. */
    private fun ComposeContentTestRule.typeIntoTag(tag: String, text: String) {
        onNodeWithTag(tag).performClick()
        waitForIdle()
        onNodeWithTag(tag).performTextInput(text)
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun withFakeAuth_showsMainApp() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun addAnimalFlow_navigateFillSave_returnsToHerdList() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_herd_list").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Register animal").performClick()
        composeTestRule.waitForIdle()
        // Focus then type so field has exact content
        composeTestRule.typeIntoTag("addAnimal_earTag", "IT-T001")
        composeTestRule.typeIntoTag("addAnimal_breed", "Angus")
        composeTestRule.onNodeWithTag("addAnimal_dobButton").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("date_picker_ok").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(400) // let date state apply before Save
        composeTestRule.onNodeWithTag("addAnimal_save").performClick()
        composeTestRule.waitForIdle()
        // App shows snackbar then auto-navigates back after 1.2s
        waitForNodeWithTag(composeTestRule, "herd_list_screen", timeoutMs = 5_000)
    }

    @Test
    fun deleteAnimalFlow_swipeShowsConfirmDialog_cancelKeepsAnimal() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_herd_list").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Register animal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.typeIntoTag("addAnimal_earTag", "IT-T002")
        composeTestRule.typeIntoTag("addAnimal_breed", "Angus")
        composeTestRule.onNodeWithTag("addAnimal_dobButton").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("date_picker_ok").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(400)
        composeTestRule.onNodeWithTag("addAnimal_save").performClick()
        composeTestRule.waitForIdle()
        waitForNodeWithTag(composeTestRule, "herd_list_screen", timeoutMs = 5_000)
        waitForNodeWithTag(composeTestRule, "herd_item_IT-T002", timeoutMs = 15_000)
        composeTestRule.onNodeWithTag("herd_item_IT-T002").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Remove animal?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("IT-T002").assertIsDisplayed()
    }
}
