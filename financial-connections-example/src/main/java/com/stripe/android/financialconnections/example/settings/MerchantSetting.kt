package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class MerchantSetting(
    val merchants: List<Merchant> = listOf(Merchant.default()),
    override val selectedOption: Merchant = merchants.first(),
    override val key: String = "merchant"
) : Saveable<Merchant>, SingleChoiceSetting<Merchant>(
    displayName = "Merchant",
    options = merchants.map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        flow = selectedOption.value
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        flow = selectedOption.value
    )

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Merchant): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToString(value: Merchant): String {
        return Json.encodeToString(value)
    }

    override fun convertToValue(value: String): Merchant {
        return runCatching {
            Json.decodeFromString<Merchant>(value)
        }.getOrElse {
            merchants.first()
        }
    }
}
