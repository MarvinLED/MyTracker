package com.example.prokject2_tracker.nutrition.diary

enum class DiaryPickerMode {
    ALL, FOOD, RECIPE, QUICK,
}

fun DiaryPickerMode.label(): String = when (this) {
    DiaryPickerMode.ALL -> "Alle"
    DiaryPickerMode.FOOD -> "Lebensmittel"
    DiaryPickerMode.RECIPE -> "Rezept"
    DiaryPickerMode.QUICK -> "Schnell hinzufügen"
}

enum class DiaryPickerSort {
    LAST_EATEN, NAME,
}

fun DiaryPickerSort.label(): String = when (this) {
    DiaryPickerSort.LAST_EATEN -> "Zuletzt gegessen"
    DiaryPickerSort.NAME -> "Name"
}
