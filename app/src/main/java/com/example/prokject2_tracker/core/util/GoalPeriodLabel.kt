package com.example.prokject2_tracker.core.util

fun GoalPeriod.label(): String = when (this) {
    GoalPeriod.DAILY -> "Täglich"
    GoalPeriod.WEEKLY -> "Wöchentlich"
    GoalPeriod.MONTHLY -> "Monatlich"
}
