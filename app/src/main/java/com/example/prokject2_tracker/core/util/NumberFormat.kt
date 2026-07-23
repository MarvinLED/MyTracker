package com.example.prokject2_tracker.core.util

import java.util.Locale

/** "52" for whole numbers, "52.3" for fractional ones — used across nutrition value displays. */
fun Double.formatCompact(): String =
    if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", this)
    }

/** Parses a decimal number typed with either ',' or '.' as the decimal separator. */
fun String.toLocaleDoubleOrNull(): Double? = replace(',', '.').toDoubleOrNull()

/** Up to [maxDecimals] decimal places, trimming trailing zeros — e.g. formatDecimal(3): 1.5 -> "1,5", 0.03 -> "0,03". */
fun Double.formatDecimal(maxDecimals: Int): String {
    val text = String.format(Locale.GERMAN, "%.${maxDecimals}f", this)
    return if (text.contains(',')) text.trimEnd('0').trimEnd(',') else text
}
