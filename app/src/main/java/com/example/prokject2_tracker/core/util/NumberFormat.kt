package com.example.prokject2_tracker.core.util

import java.util.Locale

/** "52" for whole numbers, "52.3" for fractional ones — used across nutrition value displays. */
fun Double.formatCompact(): String =
    if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", this)
    }
