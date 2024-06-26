package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestKlarna : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "klarna",
        executeInNightlyRun = true,
    ) { settings ->
        settings[CountrySettingsDefinition] = Country.US
        settings[CurrencySettingsDefinition] = Currency.USD
        settings[AutomaticPaymentMethodsSettingsDefinition] = true
    }

    @Test
    fun testKlarnaInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
            verifyCustomLpmFields = {
                verifyMandateFieldDoesNotExists()
            },
        )
    }

    @Test
    fun testKlarnaSetupFutureUsage() {
        testDriver.confirmCustom(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.PAYMENT_WITH_SETUP
            },
            verifyCustomLpmFields = {
                verifyMandateFieldExists()
            },
        )
    }

    @Test
    fun testKlarnaSetupIntentInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters.copyPlaygroundSettings { settings ->
                settings[CheckoutModeSettingsDefinition] = CheckoutMode.SETUP
            },
            verifyCustomLpmFields = {
                verifyMandateFieldExists()
            },
        )
    }
}
