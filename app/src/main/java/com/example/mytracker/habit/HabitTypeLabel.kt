package com.example.mytracker.habit

fun HabitType.label(): String = when (this) {
    HabitType.YES_NO -> "Ja/Nein"
    HabitType.COUNT -> "Anzahl"
    HabitType.DURATION -> "Dauer"
}
