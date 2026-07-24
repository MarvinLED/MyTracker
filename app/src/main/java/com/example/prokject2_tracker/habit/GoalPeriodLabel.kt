package com.example.prokject2_tracker.habit

fun GoalPeriod.label(): String = when (this) {
    GoalPeriod.DAILY -> "Täglich"
    GoalPeriod.WEEKLY -> "Wöchentlich"
    GoalPeriod.MONTHLY -> "Monatlich"
}
