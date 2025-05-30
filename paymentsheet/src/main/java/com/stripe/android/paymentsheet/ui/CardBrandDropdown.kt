package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.SingleChoiceDropdown
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors

@Composable
internal fun CardBrandDropdown(
    selectedBrand: CardBrandChoice,
    availableBrands: List<CardBrandChoice>,
    onBrandChoiceChanged: (CardBrandChoice) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .clickable {
                if (!expanded) {
                    expanded = true
                } else {
                    expanded = false
                }
            }
            .semantics {
                this.contentDescription = selectedBrand.brand.displayName
            }
            .testTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                modifier = Modifier
                    .testTag("${SELECTED_CARD_BRAND_DROPDOWN_TAG}_${selectedBrand.label.resolve()}"),
                painter = painterResource(id = selectedBrand.icon),
                contentDescription = null
            )

            Icon(
                painter = painterResource(
                    id = R.drawable.stripe_ic_chevron_down
                ),
                contentDescription = null
            )
        }

        SingleChoiceDropdown(
            expanded = expanded,
            title = com.stripe.android.R.string.stripe_card_brand_choice_selection_header.resolvableString,
            currentChoice = selectedBrand,
            choices = availableBrands,
            headerTextColor = MaterialTheme.stripeColors.subtitle,
            optionTextColor = MaterialTheme.stripeColors.onComponent,
            onChoiceSelected = { item ->
                expanded = false

                onBrandChoiceChanged(item)
            },
            onDismiss = {
                expanded = false
            }
        )
    }
}

internal const val SELECTED_CARD_BRAND_DROPDOWN_TAG = "selected_card_brand_dropdown_tag"
