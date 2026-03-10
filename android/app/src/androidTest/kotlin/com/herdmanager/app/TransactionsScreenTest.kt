package com.herdmanager.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Focused UI tests around the Transactions screen.
 *
 * These tests rely on the same fake auth module as [MainActivityTest] so that
 * the main app (with bottom nav) is shown without Firebase.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TransactionsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Navigate to the Transactions screen via bottom nav and assert it renders.
     */
    @Test
    fun openTransactions_screenDisplaysWithoutCrash() {
        composeTestRule.waitForIdle()

        // Bottom nav item for transactions has test tag "nav_transactions"
        composeTestRule.onNodeWithTag("nav_transactions").performClick()
        composeTestRule.waitForIdle()

        // Root transactions screen should be visible
        composeTestRule.onNodeWithTag("transactions_screen").assertIsDisplayed()
        // FAB to add a transaction should be present
        composeTestRule.onNodeWithContentDescription("Add transaction").assertIsDisplayed()
    }

    /**
     * When there are no transactions, an empty-state message should be shown.
     */
    @Test
    fun emptyState_showsNoTransactionsMessage() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_transactions").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("No transactions yet").assertIsDisplayed()
    }

    /**
     * On the Expenses tab, category filter chips (including "All") should be visible.
     */
    @Test
    fun expensesTab_showsCategoryFilterChips() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_transactions").performClick()
        composeTestRule.waitForIdle()

        // Switch to Expenses tab
        composeTestRule.onNodeWithText("Expenses").performClick()
        composeTestRule.waitForIdle()

        // At minimum, the "All" chip should be visible
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
    }
}

