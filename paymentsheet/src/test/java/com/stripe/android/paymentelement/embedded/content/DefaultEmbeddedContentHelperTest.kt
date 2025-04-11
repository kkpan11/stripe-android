package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper.Companion.STATE_KEY_EMBEDDED_CONTENT
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class DefaultEmbeddedContentHelperTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `dataLoaded updates savedStateHandle with paymentMethodMetadata`() = testScenario {
        assertThat(savedStateHandle.get<PaymentMethodMetadata?>(STATE_KEY_EMBEDDED_CONTENT))
            .isNull()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val rowStyle = Embedded.RowStyle.FlatWithRadio.default
        embeddedContentHelper.dataLoaded(paymentMethodMetadata, rowStyle, embeddedViewDisplaysMandateText = true)
        val state = savedStateHandle.get<DefaultEmbeddedContentHelper.State?>(STATE_KEY_EMBEDDED_CONTENT)
        assertThat(state?.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(state?.rowStyle).isEqualTo(rowStyle)
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `dataLoaded emits embeddedContent event`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded.RowStyle.FlatWithRadio.default,
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `embeddedContent emits null when clearEmbeddedContent is called`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(
                PaymentMethodMetadataFactory.create(),
                Embedded.RowStyle.FlatWithRadio.default,
                embeddedViewDisplaysMandateText = true,
            )
            assertThat(awaitItem()).isNotNull()
            embeddedContentHelper.clearEmbeddedContent()
            assertThat(awaitItem()).isNull()
        }
        assertThat(eventReporter.showNewPaymentOptionsCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `initializing embeddedContentHelper with paymentMethodMetadata emits correct initial event`() = testScenario(
        setup = {
            set(
                STATE_KEY_EMBEDDED_CONTENT,
                DefaultEmbeddedContentHelper.State(
                    PaymentMethodMetadataFactory.create(),
                    Embedded.RowStyle.FloatingButton.default,
                    embeddedViewDisplaysMandateText = true,
                )
            )
        }
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val savedStateHandle: SavedStateHandle,
        val eventReporter: FakeEventReporter,
    )

    private fun testScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle.setup()
        val selectionHolder = EmbeddedSelectionHolder(MainThreadSavedStateHandle(savedStateHandle))
        val embeddedFormHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            embeddedSelectionHolder = selectionHolder,
            savedStateHandle = savedStateHandle,
        )
        val confirmationHandler = FakeConfirmationHandler()
        val eventReporter = FakeEventReporter()
        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            savedStateHandle = MainThreadSavedStateHandle(savedStateHandle),
            eventReporter = eventReporter,
            workContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
            customerRepository = FakeCustomerRepository(),
            selectionHolder = selectionHolder,
            embeddedWalletsHelper = { stateFlowOf(null) },
            customerStateHolder = CustomerStateHolder(savedStateHandle, selectionHolder.selection),
            embeddedFormHelperFactory = embeddedFormHelperFactory,
            confirmationHandler = confirmationHandler,
            confirmationStateHolder = EmbeddedConfirmationStateHolder(
                savedStateHandle = MainThreadSavedStateHandle(savedStateHandle),
                selectionHolder = selectionHolder,
                coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            ),
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            savedStateHandle = savedStateHandle,
            eventReporter = eventReporter,
        ).block()
        confirmationHandler.validate()
        eventReporter.validate()
    }
}
