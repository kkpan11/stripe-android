package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CustomerStateTest {

    @Test
    fun `Should create 'CustomerState' for customer session properly with permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val customer = createElementsSessionCustomer(
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Disabled
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isFalse()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        // Always true for `customer_session`
        assertThat(customerState.permissions.canRemoveDuplicates).isTrue()
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions enabled`() {
        val paymentMethods = PaymentMethodFactory.cards(4)
        val customer = createElementsSessionCustomer(
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = createEnabledMobilePaymentElement(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = true,
                canRemoveLastPaymentMethod = true,
                allowRedisplayOverride = null,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isTrue()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
        // Always true for `customer_session`
        assertThat(customerState.permissions.canRemoveDuplicates).isTrue()
    }

    @Test
    fun `Should create 'CustomerState' for customer session properly with remove permissions disabled`() {
        val paymentMethods = PaymentMethodFactory.cards(3)
        val customer = createElementsSessionCustomer(
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = createEnabledMobilePaymentElement(
                isPaymentMethodSaveEnabled = false,
                isPaymentMethodRemoveEnabled = false,
                canRemoveLastPaymentMethod = false,
                allowRedisplayOverride = null,
            ),
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isFalse()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        // Always true for `customer_session`
        assertThat(customerState.permissions.canRemoveDuplicates).isTrue()
    }

    private fun createCustomerSessionForTestingDefaultPaymentMethod(
        defaultPaymentMethodId: String?,
        isSetAsDefaultEnabled: Boolean,
    ): CustomerState {
        val paymentMethods = PaymentMethodFactory.cards(3)

        val mobilePaymentElementComponent = createEnabledMobilePaymentElement(
            isPaymentMethodSetAsDefaultEnabled = isSetAsDefaultEnabled,
        )
        val customer = createElementsSessionCustomer(
            paymentMethods = paymentMethods,
            mobilePaymentElementComponent = mobilePaymentElementComponent,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val customerState = CustomerState.createForCustomerSession(
            customer = customer,
            configuration = createConfiguration(),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        return customerState
    }

    @Test
    fun `Create 'CustomerState' for customer session with nonnull default PM`() {
        val defaultPaymentMethodId = "pm_123"

        val customerState = createCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = defaultPaymentMethodId,
            isSetAsDefaultEnabled = true,
        )

        assertThat(customerState.defaultPaymentMethodId).isEqualTo(defaultPaymentMethodId)
    }

    @Test
    fun `Create 'CustomerState' for customer session with null default PM`() {
        val customerState = createCustomerSessionForTestingDefaultPaymentMethod(
            defaultPaymentMethodId = null,
            isSetAsDefaultEnabled = true,
        )

        assertThat(customerState.defaultPaymentMethodId).isNull()
    }

    @Test
    fun `Should create 'CustomerState' for legacy ephemeral keys properly`() {
        val paymentMethods = PaymentMethodFactory.cards(7)

        val customerState = createLegacyEphemeralKeyCustomerState(
            allowsRemovalOfLastSavedPaymentMethod = true,
            paymentMethods = paymentMethods,
        )

        assertThat(customerState.permissions.canRemovePaymentMethods).isTrue()
        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
        assertThat(customerState.permissions.canRemoveDuplicates).isFalse()

        assertThat(customerState.paymentMethods).isEqualTo(paymentMethods)
        assertThat(customerState.customerSessionClientSecret).isNull()
        assertThat(customerState.defaultPaymentMethodId).isNull()
        assertThat(customerState.id).isEqualTo("cus_1")
        assertThat(customerState.ephemeralKeySecret).isEqualTo("ek_1")
    }

    @Test
    fun `Should create 'CustomerState' with filtered payment methods`() =
        runTest {
            val cards = PaymentMethodFixtures.createCards(2)

            val customer = createElementsSessionCustomer(
                paymentMethods = cards + listOf(
                    PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                    PaymentMethodFixtures.LINK_PAYMENT_METHOD,
                    PaymentMethodFixtures.AU_BECS_DEBIT,
                ),
                mobilePaymentElementComponent = createEnabledMobilePaymentElement(
                    isPaymentMethodSaveEnabled = false,
                    isPaymentMethodRemoveEnabled = false,
                    canRemoveLastPaymentMethod = false,
                    allowRedisplayOverride = null,
                ),
            )

            val customerState = CustomerState.createForCustomerSession(
                customer = customer,
                configuration = createConfiguration(),
                supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
                customerSessionClientSecret = "cuss_123",
            )

            assertThat(customerState.paymentMethods).containsExactlyElementsIn(cards)
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value is true for legacy ephemeral keys`() {
        val customerState = createLegacyEphemeralKeyCustomerState(
            allowsRemovalOfLastSavedPaymentMethod = true,
            paymentMethods = listOf(),
        )

        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false for legacy ephemeral keys`() {
        val customerState = createLegacyEphemeralKeyCustomerState(
            allowsRemovalOfLastSavedPaymentMethod = false,
            paymentMethods = listOf(),
        )

        assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value & server value are false`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false & server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server value is false`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = false,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false but server value is true`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = true,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value & server value are true`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { customerState ->
            assertThat(customerState.permissions.canRemoveLastPaymentMethod).isTrue()
        }

    private fun customerSessionPermissionsTest(
        paymentElementDisabled: Boolean = false,
        canRemoveLastPaymentMethodConfigValue: Boolean = true,
        canRemoveLastPaymentMethod: Boolean = true,
        test: (customerState: CustomerState) -> Unit,
    ) {
        val customerState = CustomerState.createForCustomerSession(
            customer = createElementsSessionCustomer(
                mobilePaymentElementComponent = if (paymentElementDisabled) {
                    ElementsSession.Customer.Components.MobilePaymentElement.Disabled
                } else {
                    createEnabledMobilePaymentElement(
                        isPaymentMethodRemoveEnabled = true,
                        isPaymentMethodSaveEnabled = false,
                        canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                        allowRedisplayOverride = null,
                    )
                }
            ),
            configuration = createConfiguration(
                allowsRemovalOfLastSavedPaymentMethod = canRemoveLastPaymentMethodConfigValue
            ),
            supportedSavedPaymentMethodTypes = listOf(PaymentMethod.Type.Card),
            customerSessionClientSecret = "cuss_123",
        )

        test(customerState)
    }

    private fun createEnabledMobilePaymentElement(
        isPaymentMethodSaveEnabled: Boolean = true,
        isPaymentMethodRemoveEnabled: Boolean = false,
        canRemoveLastPaymentMethod: Boolean = false,
        allowRedisplayOverride: PaymentMethod.AllowRedisplay? = null,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
    ): ElementsSession.Customer.Components.MobilePaymentElement {
        return ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = isPaymentMethodSaveEnabled,
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            allowRedisplayOverride = allowRedisplayOverride,
            isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
        )
    }

    private fun createLegacyEphemeralKeyCustomerState(
        allowsRemovalOfLastSavedPaymentMethod: Boolean,
        paymentMethods: List<PaymentMethod> = emptyList()
    ): CustomerState {
        return CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_1",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey(
                ephemeralKeySecret = "ek_1",
            ),
            configuration = createConfiguration(allowsRemovalOfLastSavedPaymentMethod),
            paymentMethods = paymentMethods,
        )
    }

    private fun createElementsSessionCustomer(
        customerId: String = "cus_1",
        ephemeralKeySecret: String = "ek_1",
        paymentMethods: List<PaymentMethod> = listOf(),
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement,
        defaultPaymentMethodId: String? = null,
    ): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = paymentMethods,
            defaultPaymentMethod = defaultPaymentMethodId,
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = customerId,
                apiKey = ephemeralKeySecret,
                apiKeyExpiry = 999999999,
                liveMode = false,
                components = ElementsSession.Customer.Components(
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    mobilePaymentElement = mobilePaymentElementComponent
                )
            ),
        )
    }

    private fun createConfiguration(
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): CommonConfiguration {
        return PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration().copy(
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        )
    }
}
