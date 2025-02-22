package com.stripe.android.paymentelement.confirmation.gpay

import android.os.Parcelable
import com.stripe.android.CardBrandFilter
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class GooglePayConfirmationOption(
    val config: Config,
) : ConfirmationHandler.Option {
    @Parcelize
    data class Config(
        val environment: PaymentSheet.GooglePayConfiguration.Environment?,
        val merchantName: String,
        val merchantCountryCode: String,
        val merchantCurrencyCode: String?,
        val customAmount: Long?,
        val customLabel: String?,
        val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        val cardBrandFilter: CardBrandFilter
    ) : Parcelable
}
