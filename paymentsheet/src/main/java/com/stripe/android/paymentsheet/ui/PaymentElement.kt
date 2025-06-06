package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Link
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.image.StripeImageLoader
import java.util.UUID

@Composable
internal fun PaymentElement(
    enabled: Boolean,
    supportedPaymentMethods: List<SupportedPaymentMethod>,
    selectedItemCode: PaymentMethodCode,
    incentive: PaymentMethodIncentive?,
    formElements: List<FormElement>,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    formArguments: FormArguments,
    usBankAccountFormArguments: USBankAccountFormArguments,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    modifier: Modifier = Modifier,
    onInteractionEvent: () -> Unit = {},
) {
    val context = LocalContext.current
    val imageLoader = remember {
        StripeImageLoader(context.applicationContext)
    }

    val horizontalPadding = StripeTheme.getOuterFormInsets()

    val selectedIndex = remember(selectedItemCode, supportedPaymentMethods) {
        supportedPaymentMethods.map { it.code }.indexOf(selectedItemCode)
    }
    val selectedItem = remember(selectedIndex, supportedPaymentMethods) {
        supportedPaymentMethods[selectedIndex]
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (supportedPaymentMethods.size > 1) {
            NewPaymentMethodTabLayoutUI(
                selectedIndex = selectedIndex,
                isEnabled = enabled,
                paymentMethods = supportedPaymentMethods,
                incentive = incentive,
                onItemSelectedListener = onItemSelectedListener,
                imageLoader = imageLoader,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        FormElement(
            enabled = enabled,
            selectedPaymentMethodCode = selectedItem.code,
            formElements = formElements,
            formArguments = formArguments,
            usBankAccountFormArguments = usBankAccountFormArguments,
            horizontalPaddingValues = horizontalPadding,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            onInteractionEvent = onInteractionEvent,
        )
    }
}

@Composable
internal fun FormElement(
    enabled: Boolean,
    selectedPaymentMethodCode: PaymentMethodCode,
    formElements: List<FormElement>,
    formArguments: FormArguments,
    usBankAccountFormArguments: USBankAccountFormArguments,
    horizontalPaddingValues: PaddingValues,
    onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
    onInteractionEvent: () -> Unit,
) {
    // The PaymentMethodForm has a reference to a FormViewModel, which is scoped to a key. This is to ensure that
    // the FormViewModel is recreated when the PaymentElement is recomposed.
    val uuid = rememberSaveable { UUID.randomUUID().toString() }
    // This flag is stored at the parent node level to ensure the USBankAccount form completion
    // event is triggered exactly once per session, persisting even if the user navigates
    // between different payment methods before returning to USBankAccount in horizontal mode.
    var previouslyCompletedUSBankAccountForm by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .testTag(FORM_ELEMENT_TEST_TAG)
            .pointerInput("AddPaymentMethod") {
                awaitEachGesture {
                    val gesture = awaitPointerEvent()

                    when (gesture.type) {
                        PointerEventType.Press -> onInteractionEvent()
                        else -> Unit
                    }
                }
            }
            .onFocusChanged { state ->
                if (state.hasFocus) {
                    onInteractionEvent()
                }
            }
    ) {
        if (selectedPaymentMethodCode == USBankAccount.code || selectedPaymentMethodCode == Link.code) {
            USBankAccountForm(
                formArgs = formArguments,
                usBankAccountFormArgs = usBankAccountFormArguments,
                onCompleted = {
                    if (!previouslyCompletedUSBankAccountForm) {
                        usBankAccountFormArguments.onFormCompleted()
                    }
                    previouslyCompletedUSBankAccountForm = true
                },
                modifier = Modifier.padding(horizontalPaddingValues),
                enabled = enabled
            )
        } else {
            PaymentMethodForm(
                uuid = uuid,
                args = formArguments,
                enabled = enabled,
                onFormFieldValuesChanged = onFormFieldValuesChanged,
                formElements = formElements,
                modifier = Modifier.padding(horizontalPaddingValues)
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val FORM_ELEMENT_TEST_TAG = "FORM_ELEMENT_UI"
