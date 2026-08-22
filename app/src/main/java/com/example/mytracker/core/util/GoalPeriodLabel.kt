package com.example.mytracker.core.util

fun GoalPeriod.label(): String = when (this) {
    GoalPeriod.DAILY -> "Täglich"
    GoalPeriod.WEEKLY -> "Wöchentlich"
    GoalPeriod.MONTHLY -> "Monatlich"
}
