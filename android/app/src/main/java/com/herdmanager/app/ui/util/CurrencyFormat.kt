package com.herdmanager.app.ui.util

import com.herdmanager.app.domain.model.FarmSettings

/**
 * Formats an amount in cents to a localized currency string using the farm's currency setting.
 * Default currency is South African Rand (R).
 */
object CurrencyFormat {

    /** ISO 4217 code -> symbol. Default ZAR = R. */
    private val SYMBOLS = mapOf(
        "ZAR" to "R",
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "BWP" to "P",
        "NAD" to "N$",
        "SZL" to "E",
        "LSL" to "L",
        "AUD" to "A$",
        "CAD" to "C$",
        "CHF" to "CHF",
        "JPY" to "¥",
        "INR" to "₹",
        "CNY" to "¥",
        "KES" to "KSh",
        "NGN" to "₦"
    )

    /**
     * Format amount in cents to currency string (e.g. "R 19.99" or "-R 10.00").
     * Uses [currencyCode] (e.g. ZAR) for symbol; falls back to R if unknown.
     */
    fun formatCents(cents: Long, currencyCode: String): String {
        val code = currencyCode.ifBlank { FarmSettings.DEFAULT_CURRENCY_CODE }.uppercase()
        val symbol = SYMBOLS[code] ?: "R"
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        val major = abs / 100
        val minor = abs % 100
        val minorStr = minor.toString().padStart(2, '0')
        return "$sign$symbol $major.$minorStr"
    }

    /** Supported currency codes for the settings picker (code to display label). */
    fun supportedCurrencies(): List<Pair<String, String>> = listOf(
        "ZAR" to "South African Rand (R)",
        "USD" to "US Dollar ($)",
        "EUR" to "Euro (€)",
        "GBP" to "British Pound (£)",
        "BWP" to "Botswana Pula (P)",
        "NAD" to "Namibian Dollar (N$)",
        "AUD" to "Australian Dollar (A$)",
        "CAD" to "Canadian Dollar (C$)",
        "CHF" to "Swiss Franc (CHF)",
        "KES" to "Kenyan Shilling (KSh)",
        "NGN" to "Nigerian Naira (₦)"
    )
}
