package com.example.prokject2_tracker.habit

fun HabitType.label(): String = when (this) {
    HabitType.YES_NO -> "Ja/Nein"
    HabitType.COUNT -> "Anzahl"
    HabitType.DURATION -> "Dauer"
}
