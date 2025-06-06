package com.stripe.android.paymentelement.embedded.form

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.TestModeBadge
import com.stripe.android.paymentsheet.utils.DismissKeyboardOnProcessing
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun FormActivityUI(
    interactor: DefaultVerticalModeFormInteractor,
    eventReporter: EventReporter,
    onClick: () -> Unit,
    onProcessingCompleted: () -> Unit,
    state: FormActivityStateHelper.State,
    onDismissed: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val interactorState by interactor.state.collectAsState()

    DismissKeyboardOnProcessing(interactorState.isProcessing)

    EventReporterProvider(eventReporter) {
        BottomSheetScaffold(
            topBar = {
                FormActivityTopBar(
                    isLiveMode = interactor.isLiveMode,
                    onDismissed = onDismissed
                )
            },
            content = {
                VerticalModeFormUI(
                    interactor = interactor,
                    showsWalletHeader = false
                )
                USBankAccountMandate(state)
                FormActivityError(state)
                Spacer(Modifier.height(40.dp))
                FormActivityPrimaryButton(
                    state = state,
                    onClick = onClick,
                    onProcessingCompleted = onProcessingCompleted,
                )
                PaymentSheetContentPadding()
            },
            scrollState = scrollState
        )
    }
}

@Composable
internal fun USBankAccountMandate(
    state: FormActivityStateHelper.State
) {
    state.mandateText?.let {
        Mandate(
            mandateText = it.resolve(),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(StripeTheme.getOuterFormInsets())
        )
    }
}

@Composable
internal fun FormActivityError(
    state: FormActivityStateHelper.State
) {
    state.error?.let {
        ErrorMessage(
            error = it.resolve(),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .padding(StripeTheme.getOuterFormInsets())
        )
    }
}

@Composable
internal fun FormActivityPrimaryButton(
    state: FormActivityStateHelper.State,
    onProcessingCompleted: () -> Unit = {},
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.padding(StripeTheme.getOuterFormInsets())
    ) {
        PrimaryButton(
            modifier = Modifier.testTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON),
            label = state.primaryButtonLabel.resolve(),
            locked = state.shouldDisplayLockIcon,
            enabled = state.isEnabled,
            onClick = onClick,
            onProcessingCompleted = onProcessingCompleted,
            processingState = state.processingState
        )
    }
}

@Composable
internal fun FormActivityTopBar(
    isLiveMode: Boolean,
    onDismissed: () -> Unit
) {
    val tintColor = MaterialTheme.stripeColors.appBarIcon
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = StripeTheme.formInsets.start.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (!isLiveMode) {
            TestModeBadge()
        }
        IconButton(
            enabled = true,
            onClick = onDismissed,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                contentDescription = stringResource(R.string.stripe_paymentsheet_close),
                tint = tintColor
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON = "EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON"
