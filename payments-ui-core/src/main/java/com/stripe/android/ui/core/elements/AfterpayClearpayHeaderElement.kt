package com.stripe.android.ui.core.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.compose.ui.text.intl.Locale
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AfterpayClearpayHeaderElement(
    override val identifier: IdentifierSpec,
    private val amount: Amount,
    override val controller: Controller? = null
) : FormElement {
    override val allowsUserInteraction: Boolean = false
    override val mandateText: ResolvableString? = null

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        stateFlowOf(emptyList())

    val infoUrl: String
        get() = url.format(getLocaleString(Locale.current))

    private fun getLocaleString(locale: Locale) =
        locale.language.lowercase() + "_" + locale.region.uppercase()

    fun getLabel(resources: Resources): String {
        val numInstallments = when (amount.currencyCode.lowercase()) {
            "eur" -> 3
            else -> 4
        }
        return resources.getString(
            R.string.stripe_afterpay_clearpay_message
        ).replace("<num_installments/>", numInstallments.toString())
            .replace(
                "<installment_price/>",
                CurrencyFormatter.format(
                    amount.value / numInstallments,
                    amount.currencyCode
                )
            )
            // The no break space will keep the afterpay logo and (i) on the same line.
            .replace(
                "<img/>",
                "<img/>$NO_BREAK_SPACE<b>ⓘ</b>"
            )
    }

    companion object {
        const val url = "https://static.afterpay.com/modal/%s.html"
        const val NO_BREAK_SPACE = "\u00A0"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun isClearpay() = setOf("GB", "ES", "FR", "IT").contains(Locale.current.region)
    }
}
