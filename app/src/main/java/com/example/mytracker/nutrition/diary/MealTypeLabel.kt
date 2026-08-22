package com.example.prokject2_tracker.nutrition.diary

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "Frühstück"
    MealType.LUNCH -> "Mittagessen"
    MealType.DINNER -> "Abendessen"
    MealType.SNACK -> "Snack"
}
