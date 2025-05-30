package com.stripe.android.paymentelement.embedded.form

import app.cash.turbine.Turbine
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class FakeFormActivityStateHelper : FormActivityStateHelper {
    val updateTurbine = Turbine<ConfirmationHandler.State>()

    override val state: StateFlow<FormActivityStateHelper.State>
        get() = stateFlowOf(
            FormActivityStateHelper.State(
                primaryButtonLabel = "".resolvableString,
                isEnabled = false,
                processingState = PrimaryButtonProcessingState.Idle(null),
                isProcessing = false,
                shouldDisplayLockIcon = true,
            )
        )

    override fun updateConfirmationState(confirmationState: ConfirmationHandler.State) {
        updateTurbine.add(confirmationState)
    }

    override fun updateMandate(mandateText: ResolvableString?) {
        error("This should never be called!")
    }

    override fun updatePrimaryButton(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        error("This should never be called!")
    }

    override fun updateError(error: ResolvableString?) {
        error("This should never be called!")
    }

    fun validate() {
        updateTurbine.ensureAllEventsConsumed()
    }
}
