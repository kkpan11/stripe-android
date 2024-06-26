package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry

/**
 * The identifier here comes from the form element (section, static text, etc)
 */
internal data class FormFieldValues(
    val fieldValuePairs: Map<IdentifierSpec, FormFieldEntry> = mapOf(),
    val userRequestedReuse: PaymentSelection.CustomerRequestedSave
)
