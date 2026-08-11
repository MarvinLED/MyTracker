package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.ui.graphics.Color
import com.example.prokject2_tracker.core.datastore.Nutrient

/**
 * One line the Verlauf can draw. The enum name is the persisted key, so renaming an entry drops
 * that line from a saved selection — add rather than rename.
 *
 * Soll and Ist of the same nutrient deliberately share a [color] and are told apart by [dashed]
 * instead. Thirteen distinguishable hues do not exist in the app's eight-slot palette, and pairing
 * them by hue is also the more useful reading: the eye follows one nutrient's target and intake
 * together.
 */
enum class DiaryHistorySeries(
    val label: String,
    val unit: String,
    val color: Color,
    /** True for the goal lines — drawn as a dashed stroke. */
    val dashed: Boolean,
    /** The nutrient this line reads, or null for [WEIGHT], which is not one. */
    val nutrient: Nutrient?,
) {
    KCAL_GOAL("Kalorien Soll", "kcal", KcalColor, dashed = true, nutrient = Nutrient.KCAL),
    KCAL_ACTUAL("Kalorien Ist", "kcal", KcalColor, dashed = false, nutrient = Nutrient.KCAL),

    PROTEIN_GOAL("Protein Soll", "g", NutrientColors.getValue(Nutrient.PROTEIN), true, Nutrient.PROTEIN),
    PROTEIN_ACTUAL("Protein Ist", "g", NutrientColors.getValue(Nutrient.PROTEIN), false, Nutrient.PROTEIN),

    CARBS_GOAL("Kohlenhydrate Soll", "g", NutrientColors.getValue(Nutrient.CARBS), true, Nutrient.CARBS),
    CARBS_ACTUAL("Kohlenhydrate Ist", "g", NutrientColors.getValue(Nutrient.CARBS), false, Nutrient.CARBS),

    FAT_GOAL("Fett Soll", "g", NutrientColors.getValue(Nutrient.FAT), true, Nutrient.FAT),
    FAT_ACTUAL("Fett Ist", "g", NutrientColors.getValue(Nutrient.FAT), false, Nutrient.FAT),

    SUGAR_GOAL("Zucker Soll", "g", NutrientColors.getValue(Nutrient.SUGAR), true, Nutrient.SUGAR),
    SUGAR_ACTUAL("Zucker Ist", "g", NutrientColors.getValue(Nutrient.SUGAR), false, Nutrient.SUGAR),

    SALT_GOAL("Salz Soll", "g", NutrientColors.getValue(Nutrient.SALT), true, Nutrient.SALT),
    SALT_ACTUAL("Salz Ist", "g", NutrientColors.getValue(Nutrient.SALT), false, Nutrient.SALT),

    /**
     * No Soll counterpart: the app has no weight goal to draw one from. Kept out of [NutrientRows]
     * for that reason and given its own row on the screen.
     */
    WEIGHT("Gewicht", "kg", WeightColor, dashed = false, nutrient = null),
    ;

    val isGoal: Boolean get() = dashed
}

/**
 * The checkbox rows, in the order they are shown: one nutrient per row, its Soll and Ist beside
 * each other. Gewicht is not here — it is a single checkbox below these.
 */
val NutrientRows: List<Pair<DiaryHistorySeries, DiaryHistorySeries>> = listOf(
    DiaryHistorySeries.KCAL_GOAL to DiaryHistorySeries.KCAL_ACTUAL,
    DiaryHistorySeries.PROTEIN_GOAL to DiaryHistorySeries.PROTEIN_ACTUAL,
    DiaryHistorySeries.CARBS_GOAL to DiaryHistorySeries.CARBS_ACTUAL,
    DiaryHistorySeries.FAT_GOAL to DiaryHistorySeries.FAT_ACTUAL,
    DiaryHistorySeries.SUGAR_GOAL to DiaryHistorySeries.SUGAR_ACTUAL,
    DiaryHistorySeries.SALT_GOAL to DiaryHistorySeries.SALT_ACTUAL,
)

/** The row label — "Protein Soll" and "Protein Ist" share it, so the checkboxes carry only Soll/Ist. */
fun rowLabel(row: Pair<DiaryHistorySeries, DiaryHistorySeries>): String =
    row.second.label.removeSuffix(" Ist")
