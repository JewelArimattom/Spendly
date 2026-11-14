package com.spendly.app.detection.parsing

import kotlin.math.roundToLong

object AmountParser {

    private val PREFIX_AMOUNT_REGEX = Regex(
        """(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val SUFFIX_AMOUNT_REGEX = Regex(
        """([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:₹|Rs\.?|INR)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extracts amount in minor units (paisa) from text.
     * Returns null if no valid monetary value is found.
     */
    fun extractAmountMinorUnits(text: String): Long? {
        val prefixMatch = PREFIX_AMOUNT_REGEX.find(text)
        val matchedString = prefixMatch?.groupValues?.get(1)
            ?: SUFFIX_AMOUNT_REGEX.find(text)?.groupValues?.get(1)
            ?: return null

        return parseToMinorUnits(matchedString)
    }

    private fun parseToMinorUnits(rawNumber: String): Long? {
        val cleaned = rawNumber.replace(",", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0.0) return null
        return (value * 100.0).roundToLong()
    }
}
