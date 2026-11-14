package com.spendly.app.detection.parsing

import android.util.Log
import com.spendly.app.domain.model.PaymentMethod
import com.spendly.app.domain.model.TransactionCandidate
import com.spendly.app.domain.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationParserStrategy {
    fun canHandle(packageName: String, title: String?, text: String?): Boolean
    fun parse(packageName: String, title: String?, text: String?, postTime: Long): TransactionCandidate?
}

@Singleton
class NotificationTransactionParser @Inject constructor() {

    companion object {
        private const val TAG = "NotificationTxParser"

        val ALL_MONITORED_PACKAGES = setOf(
            // UPI Apps
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.dreamplug.androidapp"
        ) + BankNotificationParser.BANK_PACKAGES
    }

    private val strategies = mutableListOf<NotificationParserStrategy>()

    init {
        // Register UPI App Parser
        strategies.add(object : NotificationParserStrategy {
            override fun canHandle(packageName: String, title: String?, text: String?): Boolean {
                return UpiAppNotificationParser.canHandle(packageName)
            }

            override fun parse(
                packageName: String,
                title: String?,
                text: String?,
                postTime: Long
            ): TransactionCandidate? {
                return UpiAppNotificationParser.parse(packageName, title, text, postTime)
            }
        })

        // Register Bank Parser
        strategies.add(object : NotificationParserStrategy {
            override fun canHandle(packageName: String, title: String?, text: String?): Boolean {
                return BankNotificationParser.canHandle(packageName, title, text)
            }

            override fun parse(
                packageName: String,
                title: String?,
                text: String?,
                postTime: Long
            ): TransactionCandidate? {
                return BankNotificationParser.parse(packageName, title, text, postTime)
            }
        })
    }

    fun registerStrategy(strategy: NotificationParserStrategy) {
        strategies.add(0, strategy)
    }

    /**
     * Parses an incoming notification from title and text.
     * Returns a valid TransactionCandidate or null if non-financial / unparseable.
     */
    fun parseNotification(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long = System.currentTimeMillis()
    ): TransactionCandidate? {
        // Step 1: Filter non-financial notifications (OTP, promo, security, rating, pure balance inquiry)
        if (NotificationFilter.isNonFinancial(title, text)) {
            Log.d(TAG, "Filtered non-financial notification from $packageName")
            return null
        }

        // Step 2: Try registered strategies
        for (strategy in strategies) {
            if (strategy.canHandle(packageName, title, text)) {
                val candidate = strategy.parse(packageName, title, text, postTime)
                if (candidate != null) {
                    Log.d(TAG, "Successfully parsed transaction from $packageName via ${strategy.javaClass.simpleName}: amount=${candidate.amountMinorUnits}")
                    return candidate
                }
            }
        }

        // Step 3: Generic fallback for any other financial notification with clear amount
        return parseGenericFinancialNotification(packageName, title, text, postTime)
    }

    private fun parseGenericFinancialNotification(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long
    ): TransactionCandidate? {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".trim()
        val amountMinorUnits = AmountParser.extractAmountMinorUnits(combined) ?: return null

        val isFailed = listOf("failed", "declined", "unsuccessful").any { combined.contains(it, ignoreCase = true) }
        val isRefund = listOf("refund", "refunded").any { combined.contains(it, ignoreCase = true) }
        val isCredit = listOf("credited", "received").any { combined.contains(it, ignoreCase = true) }

        val type = when {
            isRefund -> TransactionType.REFUND
            isCredit -> TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }

        return TransactionCandidate(
            amountMinorUnits = amountMinorUnits,
            currency = "INR",
            transactionType = type,
            merchantName = null,
            purpose = "Detected Transaction",
            paymentMethod = PaymentMethod.OTHER,
            sourcePackage = packageName,
            referenceId = null,
            timestamp = postTime,
            confidence = 0.5f,
            isFailed = isFailed,
            rawTitle = title,
            rawText = text
        )
    }
}
