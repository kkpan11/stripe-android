package com.stripe.android.payments.wechatpay

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.getRequestCode
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.wechatpay.reflection.DefaultWeChatPayReflectionHelper
import com.stripe.android.payments.wechatpay.reflection.WeChatPayReflectionHelper
import com.stripe.android.view.AuthActivityStarterHost

/**
 * [PaymentNextActionHandler] implementation to authenticate through WeChatPay SDK.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WeChatPayNextActionHandler : PaymentNextActionHandler<StripeIntent>() {
    /**
     * [weChatPayAuthLauncher] is mutable and might be updated during
     * through [onNewActivityResultCaller]
     */
    private var weChatPayAuthLauncher: ActivityResultLauncher<WeChatPayAuthContract.Args>? = null

    // TODO(ccen): inject reflectionHelper as a singleton
    @VisibleForTesting
    internal var reflectionHelper: WeChatPayReflectionHelper = DefaultWeChatPayReflectionHelper()

    @VisibleForTesting
    internal var weChatAuthLauncherFactory =
        { host: AuthActivityStarterHost, requestCode: Int ->
            weChatPayAuthLauncher?.let {
                WeChatPayAuthStarter.Modern(it)
            } ?: WeChatPayAuthStarter.Legacy(host, requestCode)
        }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        weChatPayAuthLauncher = activityResultCaller.registerForActivityResult(
            WeChatPayAuthContract(),
            activityResultCallback
        )
    }

    override fun onLauncherInvalidated() {
        weChatPayAuthLauncher?.unregister()
        weChatPayAuthLauncher = null
    }

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        require(reflectionHelper.isWeChatPayAvailable()) {
            "WeChatPay dependency is not found, add " +
                "${WeChatPayReflectionHelper.WECHAT_PAY_GRADLE_DEP} in app's build.gradle"
        }

        val weChatPayRedirect =
            requireNotNull(
                actionable.nextActionData as? StripeIntent.NextActionData.WeChatPayRedirect
            ) {
                val incorrectNextActionDataType = actionable.nextActionData?.let {
                    it::class.java.simpleName
                } ?: run {
                    "null"
                }
                "stripeIntent.nextActionData should be WeChatPayRedirect, " +
                    "instead it is $incorrectNextActionDataType"
            }

        weChatAuthLauncherFactory(
            host,
            actionable.getRequestCode()
        ).start(
            WeChatPayAuthContract.Args(
                weChatPayRedirect.weChat,
                actionable.clientSecret.orEmpty()
            )
        )
    }
}
