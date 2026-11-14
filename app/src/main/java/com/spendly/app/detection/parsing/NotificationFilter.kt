package com.spendly.app.detection.parsing

object NotificationFilter {

    private val OTP_PATTERNS = listOf(
        Regex("""\b(?:otp|one[- ]time[- ]password|verification[- ]code|secret[- ]code)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:do not share|valid for \d+ min|is your (?:login|verification|security) code)\b""", RegexOption.IGNORE_CASE)
    )

    private val PROMO_PATTERNS = listOf(
        Regex("""\b(?:cashback(?: of)?(?: up to)?|win scratch card|scratch card|spin the wheel|use coupon|use code)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:flat \d+% off|flat rs|flat ₹|exclusive offer|claim reward|refer and earn)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:pre-approved loan|apply for credit card|credit score updated|check your cibil)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:discount on your next|hurry, offer ends|congratulations! you won)\b""", RegexOption.IGNORE_CASE)
    )

    private val SECURITY_PATTERNS = listOf(
        Regex("""\b(?:login detected|logged in from|password (?:has been )?changed|pin (?:has been )?reset|device registered)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:biometric login enabled|suspicious login|card blocked|sim card changed)\b""", RegexOption.IGNORE_CASE)
    )

    private val RATING_PATTERNS = listOf(
        Regex("""\b(?:rate your experience|how was your delivery|give us \d+ stars|leave a review|share feedback)\b""", RegexOption.IGNORE_CASE)
    )

    private val BALANCE_INQUIRY_ONLY_PATTERNS = listOf(
        Regex("""^(?:your )?(?:available|account|clear) balance (?:is|in)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:balance enquiry|mini statement|view statement)\b""", RegexOption.IGNORE_CASE)
    )

    private val FINANCIAL_CONFIRMATION_PATTERN = Regex(
        """\b(?:debited|credited|paid|sent|received|spent|transferred|purchase of|refund|refunded)\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns true if the notification is promotional, an OTP, a rating request,
     * a security alert, or a plain balance inquiry with no actual transaction.
     */
    fun isNonFinancial(title: String?, text: String?): Boolean {
        val combined = "${title.orEmpty()} ${text.orEmpty()}".trim()
        if (combined.isBlank()) return true

        // Strict OTP filter
        if (OTP_PATTERNS.any { it.containsMatchIn(combined) }) return true

        // Security alerts filter
        if (SECURITY_PATTERNS.any { it.containsMatchIn(combined) }) return true

        // Rating requests filter
        if (RATING_PATTERNS.any { it.containsMatchIn(combined) }) return true

        val hasTransactionVerb = FINANCIAL_CONFIRMATION_PATTERN.containsMatchIn(combined)

        // Balance inquiry only
        if (!hasTransactionVerb && BALANCE_INQUIRY_ONLY_PATTERNS.any { it.containsMatchIn(combined) }) {
            return true
        }

        // Promotional messages without an actual confirmed financial movement
        if (!hasTransactionVerb && PROMO_PATTERNS.any { it.containsMatchIn(combined) }) {
            return true
        }

        return false
    }
}
