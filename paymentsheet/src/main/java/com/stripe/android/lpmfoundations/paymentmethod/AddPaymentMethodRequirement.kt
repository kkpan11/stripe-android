package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

internal enum class AddPaymentMethodRequirement {
    /** A special case that indicates the payment method is always unsupported by PaymentSheet. */
    Unsupported {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean = false
    },

    /** Indicates the payment method is unsupported by PaymentSheet when using SetupIntents or SFU. */
    UnsupportedForSetup {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return !metadata.hasIntentToSetup(code)
        }
    },

    /** Indicates that a payment method requires shipping information. */
    ShippingAddress {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            if (metadata.allowsPaymentMethodsRequiringShippingAddress) {
                return true
            }

            val shipping = (metadata.stripeIntent as? PaymentIntent)?.shipping
            return shipping?.name != null &&
                shipping.address.line1 != null &&
                shipping.address.country != null &&
                shipping.address.postalCode != null
        }
    },

    /** Requires that the developer declare support for asynchronous payment methods. */
    MerchantSupportsDelayedPaymentMethods {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.allowsDelayedPaymentMethods
        }
    },

    /** Requires that the FinancialConnections SDK has been linked. */
    FinancialConnectionsSdk {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.financialConnectionsAvailability != null
        }
    },

    /** Requires a valid us bank verification method. */
    ValidUsBankVerificationMethod {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            // Verification method is always 'automatic' for deferred intents
            val isDeferred = metadata.stripeIntent.clientSecret == null
            return isDeferred || supportedVerificationMethodForNonDeferredIntent(metadata)
        }

        private fun supportedVerificationMethodForNonDeferredIntent(
            metadata: PaymentMethodMetadata,
        ): Boolean {
            val pmo = metadata.stripeIntent.getPaymentMethodOptions()[USBankAccount.code]
            val verificationMethod = (pmo as? Map<*, *>)?.get("verification_method") as? String
            val supportsVerificationMethod = verificationMethod in setOf("automatic", "instant", "instant_or_skip")
            return supportsVerificationMethod
        }
    },

    /** Requires that Instant Debits are possible for this transaction. */
    InstantDebits {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.linkConfiguration.shouldDisplay &&
                metadata.linkMode != LinkMode.LinkCardBrand &&
                metadata.supportsMobileInstantDebitsFlow
        }
    },

    /** Requires that LinkCardBrand is possible for this transaction. */
    LinkCardBrand {
        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.linkConfiguration.shouldDisplay &&
                metadata.linkMode == LinkMode.LinkCardBrand &&
                metadata.supportsMobileInstantDebitsFlow
        }
    };

    abstract fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean
}

private val PaymentMethodMetadata.supportsMobileInstantDebitsFlow: Boolean
    get() {
        val paymentMethodTypes = stripeIntent.paymentMethodTypes
        val noUsBankAccount = USBankAccount.code !in paymentMethodTypes
        val supportsBankAccounts = "bank_account" in stripeIntent.linkFundingSources
        return noUsBankAccount && supportsBankAccounts && canShowBankForm
    }

private val PaymentMethodMetadata.canShowBankForm: Boolean
    get() {
        val collectsEmail = billingDetailsCollectionConfiguration.email != CollectionMode.Never
        val attachDefaults = billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod
        val hasDefaultValue = attachDefaults && !defaultBillingDetails?.email.isNullOrBlank()
        return collectsEmail || hasDefaultValue
    }
