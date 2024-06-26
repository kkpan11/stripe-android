package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.test.core.TestParameters
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGiropay : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "giropay",
    )

    @Test
    @Ignore("Giropay is deprecated as of 2024-06-30")
    fun testGiropay() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = testParameters,
        )
    }

    @Test
    @Ignore("Giropay is deprecated as of 2024-06-30")
    fun testGiropayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = testParameters,
        )
    }
}
