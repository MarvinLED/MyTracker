package com.example.mytracker.core.metrics

fun Granularity.label(): String = when (this) {
    Granularity.DAILY -> "Tag"
    Granularity.WEEKLY -> "Woche"
    Granularity.MONTHLY -> "Monat"
}
