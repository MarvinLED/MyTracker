package com.example.mytracker.core.util

import java.util.Locale

/**
 * The app is German throughout — every label and date format is hardcoded German — so numbers are
 * too. Pinning it rather than using [Locale.getDefault] means the same value never renders as "52.3"
 * on one device and "52,3" on another, and it matches what [String.toLocaleDoubleOrNull] accepts
 * back: a decimal comma.
 */
val AppLocale: Locale = Locale.GERMAN

/** "52" for whole numbers, "52,3" for fractional ones — used across all value displays. */
fun Double.formatCompact(): String =
    if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format(AppLocale, "%.1f", this)
    }

/**
 * Parses a decimal number typed with either ',' or '.' as the decimal separator. **Every** numeric
 * text field goes through this: on a German keyboard the decimal key is a comma, and plain
 * [String.toDoubleOrNull] rejects it, which silently disables the save button.
 */
fun String.toLocaleDoubleOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Up to [maxDecimals] decimal places, trimming trailing zeros — e.g. 1.5 -> "1,5", 0.03 -> "0,03". */
fun Double.formatDecimal(maxDecimals: Int): String {
    val text = String.format(AppLocale, "%.${maxDecimals}f", this)
    return if (text.contains(',')) text.trimEnd('0').trimEnd(',') else text
}
