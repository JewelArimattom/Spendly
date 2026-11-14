package com.spendly.app.detection.parsing

import com.spendly.app.domain.model.PaymentMethod
import com.spendly.app.domain.model.TransactionCandidate
import com.spendly.app.domain.model.TransactionType

object BankNotificationParser {

    val BANK_PACKAGES = setOf(
        "com.snapwork.hdfc",             // HDFC MobileBanking
        "com.hdfcbank.payzapp",          // HDFC PayZapp
        "net.one97.payzapp",
        "com.hdfc.quickbanking",
        "com.sbi.lotusintouch",          // YONO SBI
        "com.sbi.upi",                   // SBI BHIM Pay
        "com.snapwork.sbi",
        "com.csam.icici.bank.imobile",   // ICICI iMobile
        "com.icicibank.pockets",         // ICICI Pockets
        "com.axis.mobile",               // Axis Mobile
        "com.axis.open",                 // Axis Open
        "com.msf.kbank.mobile",          // Kotak 811
        "com.pnb.pnbone",                // PNB ONE
        "com.bankofbaroda.mconnect"      // BOB World
    )

    private val BANK_IDENTIFIERS = listOf(
        "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "BARODA", "BOB"
    )

    private val SMS_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging"
    )

    private val BANK_REFERENCE_PATTERNS = listOf(
        Regex("""\bUPI(?:/|\s*Ref(?:\s*(?:ID|Id|No|Number))?[:\s]+)([0-9]{12})\b""", RegexOption.IGNORE_CASE),
        Regex("""\bUTR[:\s]+([0-9]{12})\b""", RegexOption.IGNORE_CASE),
        Regex("""\bRef(?:\s*(?:No|Number|ID|Id))?[:\s]+([A-Za-z0-9]{6,25})\b""", RegexOption.IGNORE_CASE),
        Regex("""\bInfo:\s*UPI/([0-9]{12})/""", RegexOption.IGNORE_CASE),
        Regex("""UPI/([0-9]{12})/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    )

    private val BANK_PAYEE_PATTERNS = listOf(
        // "Info: UPI/123456789012/MerchantName"
        Regex("""UPI/[0-9]{12}/([^/\s,\.]+)""", RegexOption.IGNORE_CASE),
        // "debited ... to MerchantName"
        Regex("""(?:debited|transfer|transferred|sent|paid)\s+(?:.*?)\s+(?:to|towards)\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        // "spent on ... at MerchantName"
        Regex("""(?:spent|purchase)\s+(?:.*?)\s+at\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE),
        // "towards MerchantName"
        Regex("""towards\s+([a-zA-Z0-9 &'-]+)""", RegexOption.IGNORE_CASE)
    )

    private val FAILED_KEYWORDS = listOf("failed", "declined", "unsuccessful", "failure")
    private val REFUND_KEYWORDS = listOf("refund", "refunded", "reversal")
    private val CREDIT_KEYWORDS = listOf("credited with", "credited by", "account has been credited", "received")

    fun canHandle(packageName: String, title: String?, text: String?): Boolean {
        if (BANK_PACKAGES.contains(packageName)) return true
        val combined = "${title.orEmpty()} ${text.orEmpty()}".uppercase()
        val hasBankMention = BANK_IDENTIFIERS.any { combined.contains(it) }
        return (SMS_PACKAGES.contains(packageName) && hasBankMention) || hasBankMention
    }

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
        val paymentMethod = detectPaymentMethod(combined)

        var confidence = 0.65f
        if (merchantName != null) confidence += 0.15f
        if (referenceId != null) confidence += 0.15f
        if (BANK_PACKAGES.contains(packageName)) confidence += 0.05f
        confidence = confidence.coerceIn(0.0f, 1.0f)

        return TransactionCandidate(
            amountMinorUnits = amountMinorUnits,
            currency = "INR",
            transactionType = transactionType,
            merchantName = merchantName,
            purpose = "Bank Transaction",
            paymentMethod = paymentMethod,
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
        for (pattern in BANK_REFERENCE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    private fun extractMerchantName(text: String): String? {
        for (pattern in BANK_PAYEE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val groupIndex = if (match.groupValues.size > 2 && match.groupValues[2].isNotBlank()) 2 else 1
                val candidate = cleanPayee(match.groupValues[groupIndex])
                if (candidate.isNotBlank() && candidate.length > 1 && !candidate.startsWith("A/C", ignoreCase = true)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun cleanPayee(raw: String): String {
        return raw.trim()
            .replace(Regex("""^(?:to|at|towards)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:on|dated|Ref|UPI|UTR|Avl|Bal|using|INR|Rs\.?|₹).*$""", RegexOption.IGNORE_CASE), "")
            .trim('.', ',', ' ', '-')
    }

    private fun detectPaymentMethod(text: String): PaymentMethod {
        val lower = text.lowercase()
        return when {
            lower.contains("upi") -> PaymentMethod.UPI
            lower.contains("card") || lower.contains("credit card") || lower.contains("debit card") -> PaymentMethod.CARD
            lower.contains("net banking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps") -> PaymentMethod.NET_BANKING
            lower.contains("wallet") -> PaymentMethod.WALLET
            else -> PaymentMethod.UPI
        }
    }
}
