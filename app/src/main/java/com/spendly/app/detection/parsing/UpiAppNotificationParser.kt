package com.spendly.app.detection.parsing

import com.spendly.app.domain.model.PaymentMethod
import com.spendly.app.domain.model.TransactionCandidate
import com.spendly.app.domain.model.TransactionType

object UpiAppNotificationParser {

    private val UPI_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                       // Paytm
        "in.org.npci.upiapp",                    // BHIM
        "com.dreamplug.androidapp"               // CRED
    )

    private val REFERENCE_PATTERNS = listOf(
        Regex("""\bUPI(?:\s*Ref(?:\s*(?:ID|Id|No|Number))?|/)?[:\s]+([0-9]{12})\b""", RegexOption.IGNORE_CASE),
        Regex("""\bUTR[:\s]+([0-9]{12})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:Txn|Transaction)\s*(?:ID|Id|No|Number)?[:\s]+([A-Za-z0-9_-]{6,30})\b""", RegexOption.IGNORE_CASE),
        Regex("""\bRef(?:\s*(?:No|Number|ID|Id))?[:\s]+([A-Za-z0-9]{6,25})\b""", RegexOption.IGNORE_CASE)
    )

    private val PAYEE_PATTERNS = listOf(
        Regex("""(?:Paid|Payment)\s+(?:of\s+)?(?:₹|Rs\.?|INR)?\s*[0-9,.]+\s+to\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:₹|Rs\.?|INR)?\s*[0-9,.]+\s+(?:paid|transferred|sent)\s+to\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Paid|Payment\s+to)\s+([a-zA-Z0-9 &'-]+)\s+(?:of\s+)?(?:₹|Rs\.?|INR)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Transferred|Sent)\s+(?:₹|Rs\.?|INR)?\s*[0-9,.]+\s+to\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Paid|Spent)\s+(?:₹|Rs\.?|INR)?\s*[0-9,.]+\s+at\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Received|Money received)\s+(?:of\s+)?(?:₹|Rs\.?|INR)?\s*[0-9,.]+\s+from\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:Money sent:\s*(?:₹|Rs\.?|INR)?\s*[0-9,.]+\s+to)\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE)
    )

    private val FAILED_KEYWORDS = listOf("failed", "declined", "unsuccessful", "failure")
    private val REFUND_KEYWORDS = listOf("refund", "refunded", "reversal")
    private val CREDIT_KEYWORDS = listOf("received", "credited", "added to your")

    fun canHandle(packageName: String): Boolean = UPI_PACKAGES.contains(packageName)

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long
    ): TransactionCandidate? {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".trim()
        val amountMinorUnits = AmountParser.extractAmountMinorUnits(combined) ?: return null

        val isFailed = FAILED_KEYWORDS.any { combined.contains(it, ignoreCase = true) }
        val transactionType = when {
            REFUND_KEYWORDS.any { combined.contains(it, ignoreCase = true) } -> TransactionType.REFUND
            CREDIT_KEYWORDS.any { combined.contains(it, ignoreCase = true) } -> TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }

        val referenceId = extractReferenceId(combined)
        val merchantName = extractMerchantName(combined)

        // Calculate confidence
        var confidence = 0.6f
        if (merchantName != null) confidence += 0.2f
        if (referenceId != null) confidence += 0.15f
        if (canHandle(packageName)) confidence += 0.05f
        confidence = confidence.coerceIn(0.0f, 1.0f)

        return TransactionCandidate(
            amountMinorUnits = amountMinorUnits,
            currency = "INR",
            transactionType = transactionType,
            merchantName = merchantName,
            purpose = "UPI Payment",
            paymentMethod = PaymentMethod.UPI,
            sourcePackage = packageName,
            referenceId = referenceId,
            timestamp = postTime,
            confidence = confidence,
            isFailed = isFailed,
            rawTitle = title,
            rawText = text
        )
    }

    private fun extractReferenceId(text: String): String? {
        for (pattern in REFERENCE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    private fun extractMerchantName(text: String): String? {
        for (pattern in PAYEE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val candidate = cleanPayee(match.groupValues[1])
                if (candidate.isNotBlank() && candidate.length > 1) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun cleanPayee(raw: String): String {
        return raw.trim()
            .replace(Regex("""^(?:to|from|at)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:successful|failed|via|using|on|dated|Ref|INR|Rs\.?|₹|Transaction|Txn|ID|Id|UPI).*$""", RegexOption.IGNORE_CASE), "")
            .trim('.', ',', ' ', '-')
    }
}
