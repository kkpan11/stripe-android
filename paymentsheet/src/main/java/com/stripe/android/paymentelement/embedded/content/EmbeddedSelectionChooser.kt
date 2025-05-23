@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.containsVolatileDifferences
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal fun interface EmbeddedSelectionChooser {
    fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        newConfiguration: CommonConfiguration,
    ): PaymentSelection?
}

internal class DefaultEmbeddedSelectionChooser @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val formHelperFactory: EmbeddedFormHelperFactory,
    private val eventReporter: EventReporter,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedSelectionChooser {
    private var previousConfiguration: CommonConfiguration?
        get() = savedStateHandle[PREVIOUS_CONFIGURATION_KEY]
        set(value) = savedStateHandle.set(PREVIOUS_CONFIGURATION_KEY, value)

    private var previousPaymentMethodMetadata: PaymentMethodMetadata?
        get() = savedStateHandle[PREVIOUS_PAYMENT_METHOD_METADATA_KEY]
        set(value) = savedStateHandle.set(PREVIOUS_PAYMENT_METHOD_METADATA_KEY, value)

    override fun choose(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection?,
        newSelection: PaymentSelection?,
        newConfiguration: CommonConfiguration,
    ): PaymentSelection? {
        val result = previousSelection?.takeIf { selection ->
            canUseSelection(
                paymentMethodMetadata = paymentMethodMetadata,
                paymentMethods = paymentMethods,
                previousSelection = selection,
            ) && previousConfiguration?.containsVolatileDifferences(newConfiguration) != true
        } ?: newSelection

        previousConfiguration = newConfiguration
        previousPaymentMethodMetadata = paymentMethodMetadata

        return result
    }

    private fun canUseSelection(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod>?,
        previousSelection: PaymentSelection,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = paymentMethodMetadata.supportedPaymentMethodTypes()

        return when (previousSelection) {
            is PaymentSelection.New -> {
                val code = previousSelection.paymentMethodCreateParams.typeCode
                code in allowedTypes && hasCompatibleForm(
                    previousSelection = previousSelection,
                    paymentMethodMetadata = paymentMethodMetadata
                )
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = previousSelection.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && paymentMethod in (paymentMethods ?: emptyList())
            }
            is PaymentSelection.GooglePay -> {
                paymentMethodMetadata.isGooglePayReady
            }
            is PaymentSelection.Link -> {
                paymentMethodMetadata.linkState != null
            }
            is PaymentSelection.ExternalPaymentMethod -> {
                paymentMethodMetadata.isExternalPaymentMethod(previousSelection.type)
            }
            is PaymentSelection.CustomPaymentMethod -> {
                paymentMethodMetadata.isCustomPaymentMethod(previousSelection.id)
            }
        }
    }

    private fun hasCompatibleForm(
        previousSelection: PaymentSelection.New,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Boolean {
        val newFormHelper = formHelperFactory.create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            // Not important for determining formType so use default value
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
        ) {}
        val newFormType = newFormHelper.formTypeForCode(previousSelection.paymentMethodType)
        if (newFormType != FormHelper.FormType.UserInteractionRequired) {
            return true
        }
        return previousPaymentMethodMetadata?.let { previousPaymentMethodMetadata ->
            val previousFormHelper = formHelperFactory.create(
                coroutineScope = coroutineScope,
                paymentMethodMetadata = previousPaymentMethodMetadata,
                eventReporter = eventReporter,
                // Not important for determining formType so use default value
                setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            ) {}
            val previousFormElements = previousFormHelper.formElementsForCode(previousSelection.paymentMethodType)
            val newFormElements = newFormHelper.formElementsForCode(previousSelection.paymentMethodType)
            previousFormElements.size >= newFormElements.size
        } == true
    }

    companion object {
        const val PREVIOUS_CONFIGURATION_KEY = "DefaultEmbeddedSelectionChooser_PREVIOUS_CONFIGURATION_KEY"
        const val PREVIOUS_PAYMENT_METHOD_METADATA_KEY =
            "DefaultEmbeddedSelectionChooser_PREVIOUS_PAYMENT_METHOD_METADATA_KEY"
    }
}
