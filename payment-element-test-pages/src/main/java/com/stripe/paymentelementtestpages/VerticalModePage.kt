package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_EDIT_SAVED_CARD
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_TEXT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_VIEW_MORE

@SuppressWarnings("TooManyFunctions")
class VerticalModePage(
    private val composeTestRule: ComposeTestRule
) {
    fun assertIsNotVisible() {
        composeTestRule
            .onNodeWithTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT)
            .assertDoesNotExist()
    }

    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun clickOnNewLpm(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode"))
            .performScrollTo()
            .performClick()
    }

    fun assertLpmIsSelected(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode").and(isSelected())
        ).assertExists()
    }

    fun assertPrimaryButton(matcher: SemanticsMatcher) {
        composeTestRule
            .onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG).and(matcher))
            .assertExists()
    }

    fun assertMandateExists() {
        composeTestRule
            .onNode(hasTestTag(PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG))
            .assertExists()
    }

    fun assertMandateDoesNotExists() {
        composeTestRule
            .onNode(hasTestTag(PAYMENT_SHEET_MANDATE_TEXT_TEST_TAG))
            .assertDoesNotExist()
    }

    fun assertHasSavedPaymentMethods() {
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(TEST_TAG_SAVED_TEXT).isDisplayed()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SAVED_TEXT).assertExists()
    }

    fun assertDoesNotHaveSavedPaymentMethods() {
        val savedText = composeTestRule.onNodeWithTag(TEST_TAG_SAVED_TEXT)

        composeTestRule.waitUntil { savedText.isNotDisplayed() }
        savedText.assertDoesNotExist()
    }

    fun assertHasSelectedSavedPaymentMethod(paymentMethodId: String, cardBrand: String? = null) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId").and(isSelected())
        ).assertExists()

        if (cardBrand != null) {
            composeTestRule.onNode(
                hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
                    .and(
                        hasAnyDescendant(
                            hasTestTag(TEST_TAG_ICON_FROM_RES).and(
                                hasTestMetadata(
                                    cardBrand
                                )
                            )
                        )
                    ),
                useUnmergedTree = true,
            ).assertExists()
        }
    }

    fun assertHasDisplayedSavedPaymentMethod(paymentMethodId: String) {
        composeTestRule.onNode(
            hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
        ).assertExists()
    }

    fun clickSavedPaymentMethod(paymentMethodId: String) {
        val savedPaymentMethod = composeTestRule
            .onNodeWithTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")

        composeTestRule.waitUntil { savedPaymentMethod.isDisplayed() }
        savedPaymentMethod.performClick()
    }

    fun clickViewMore() {
        composeTestRule.onNodeWithTag(TEST_TAG_VIEW_MORE).performClick()
    }

    fun clickEdit() {
        composeTestRule.onNodeWithTag(TEST_TAG_EDIT_SAVED_CARD).performClick()
    }

    fun clickNewPaymentMethodButton(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val testTag = "${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode"

        composeTestRule.waitUntil(
            timeoutMillis = 5000L
        ) {
            composeTestRule
                .onAllNodes(hasTestTag(testTag))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag(testTag))
            .performClick()
    }
}
