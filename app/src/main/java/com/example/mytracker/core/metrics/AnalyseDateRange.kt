package com.example.mytracker.core.metrics

import com.example.mytracker.core.util.DateUtils

enum class AnalyseDateRange(val days: Int) {
    LAST_7(7),
    LAST_30(30),
    LAST_90(90),
    LAST_365(365),
}

fun AnalyseDateRange.label(): String = when (this) {
    AnalyseDateRange.LAST_7 -> "7 Tage"
    AnalyseDateRange.LAST_30 -> "30 Tage"
    AnalyseDateRange.LAST_90 -> "90 Tage"
    AnalyseDateRange.LAST_365 -> "1 Jahr"
}

fun AnalyseDateRange.toEpochDayRange(today: Long = DateUtils.todayEpochDay()): EpochDayRange =
    EpochDayRange(startInclusive = today - (days - 1), endInclusive = today)
