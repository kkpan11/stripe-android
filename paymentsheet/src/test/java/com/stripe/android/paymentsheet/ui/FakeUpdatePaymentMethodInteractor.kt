package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeUpdatePaymentMethodInteractor(
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val canRemove: Boolean,
    override val isExpiredCard: Boolean,
    override val isModifiablePaymentMethod: Boolean,
    override val hasValidBrandChoices: Boolean = true,
    override val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    override val shouldShowSetAsDefaultCheckbox: Boolean,
    val viewActionRecorder: ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>?,
    initialState: UpdatePaymentMethodInteractor.State,
) : UpdatePaymentMethodInteractor {
    override val state: StateFlow<UpdatePaymentMethodInteractor.State> = MutableStateFlow(initialState)
    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )
    override val topBarState: PaymentSheetTopBarState = PaymentSheetTopBarStateFactory.create(
        isLiveMode = false,
        editable = PaymentSheetTopBarState.Editable.Never,
    )

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        viewActionRecorder?.record(viewAction)
    }
}
