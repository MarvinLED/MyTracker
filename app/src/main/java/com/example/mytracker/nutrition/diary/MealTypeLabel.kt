package com.example.mytracker.nutrition.diary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Cake
import androidx.compose.ui.graphics.vector.ImageVector

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "Frühstück"
    MealType.LUNCH -> "Mittagessen"
    MealType.DINNER -> "Abendessen"
    MealType.SNACK -> "Snack"
}

/**
 * The name shortened to fit four chips on one line of a dialog. Only for places that show all four
 * side by side — on its own, a meal is still named in full by [label].
 */
fun MealType.shortLabel(): String = when (this) {
    MealType.BREAKFAST -> "Früh"
    MealType.LUNCH -> "Mittag"
    MealType.DINNER -> "Abend"
    MealType.SNACK -> "Snack"
}

/** The picture of a meal — the three brightnesses run from morning to night, the cake is the snack. */
fun MealType.icon(): ImageVector = when (this) {
    MealType.BREAKFAST -> Icons.Filled.Brightness5
    MealType.LUNCH -> Icons.Filled.Brightness7
    MealType.DINNER -> Icons.Filled.Brightness1
    MealType.SNACK -> Icons.Filled.Cake
}
