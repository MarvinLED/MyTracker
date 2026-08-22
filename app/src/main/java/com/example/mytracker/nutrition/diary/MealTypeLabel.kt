package com.example.mytracker.nutrition.diary

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "Frühstück"
    MealType.LUNCH -> "Mittagessen"
    MealType.DINNER -> "Abendessen"
    MealType.SNACK -> "Snack"
}
