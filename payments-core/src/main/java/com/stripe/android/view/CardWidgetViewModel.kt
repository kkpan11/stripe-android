package com.stripe.android.view

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.BuildConfig.DEBUG
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Provider

internal class CardWidgetViewModel(
    private val paymentConfigProvider: Provider<PaymentConfiguration>,
    private val stripeRepository: StripeRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _isCbcEligible = MutableStateFlow(false)
    val isCbcEligible: StateFlow<Boolean> = _isCbcEligible

    private var _onBehalfOf: String? = null
    val onBehalfOf: String?
        get() = _onBehalfOf

    init {
        getEligibility()
    }

    fun setOnBehalfOf(onBehalfOf: String? = null) {
        _onBehalfOf = onBehalfOf
        getEligibility()
    }

    private fun getEligibility() {
        viewModelScope.launch(dispatcher) {
            _isCbcEligible.value = determineCbcEligibility()
        }
    }

    private suspend fun determineCbcEligibility(): Boolean {
        val paymentConfig = paymentConfigProvider.get()

        val response = stripeRepository.retrieveCardElementConfig(
            requestOptions = ApiRequest.Options(
                apiKey = paymentConfig.publishableKey,
                stripeAccount = paymentConfig.stripeAccountId,
            ),
            params = onBehalfOf?.let {
                mapOf("on_behalf_of" to it)
            }
        )

        val config = response.getOrNull()
        return config?.cardBrandChoice?.eligible ?: false
    }

    class Factory(val context: Context) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val stripeRepository = StripeApiRepository(
                context = context,
                publishableKeyProvider = { PaymentConfiguration.getInstance(context).publishableKey },
            )

            @Suppress("UNCHECKED_CAST")
            return CardWidgetViewModel(
                paymentConfigProvider = { PaymentConfiguration.getInstance(context) },
                stripeRepository = stripeRepository,
            ) as T
        }
    }
}

internal fun View.doWithCardWidgetViewModel(
    viewModelStoreOwner: ViewModelStoreOwner? = null,
    action: LifecycleOwner.(CardWidgetViewModel) -> Unit,
) {
    val lifecycleOwner = findViewTreeLifecycleOwner()
    val storeOwner = viewModelStoreOwner ?: findViewTreeViewModelStoreOwner()

    if (lifecycleOwner == null || storeOwner == null) {
        if (DEBUG) {
            if (lifecycleOwner == null) {
                error("Couldn't find a LifecycleOwner for view")
            } else {
                error("Couldn't find a ViewModelStoreOwner for view")
            }
        }
        return
    }

    val factory = CardWidgetViewModel.Factory(context.applicationContext)
    val viewModel = ViewModelProvider(
        owner = storeOwner,
        factory = factory,
    )[CardWidgetViewModel::class.java]

    lifecycleOwner.action(viewModel)
}

context(LifecycleOwner)
internal inline fun <T> Flow<T>.launchAndCollect(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: (T) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(minActiveState) {
            collect {
                action(it)
            }
        }
    }
}
