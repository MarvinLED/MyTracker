package com.example.mytracker.nutrition.diary

import androidx.compose.ui.graphics.Color
import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.ui.ChartLineStyle

/** What a line says about its nutrient: the target, what was logged, or the weekly mean of those. */
enum class DiaryHistorySeriesKind { GOAL, ACTUAL, AVERAGE }

/**
 * One line the Verlauf can draw. The enum name is the persisted key, so renaming an entry drops
 * that line from a saved selection — add rather than rename.
 *
 * The three lines of a nutrient deliberately share a [color] and are told apart by their stroke
 * pattern instead. Distinguishable hues for nineteen lines do not exist in the app's eight-slot
 * palette, and pairing them by hue is also the more useful reading: the eye follows one nutrient's
 * target, intake and average together.
 */
enum class DiaryHistorySeries(
    val label: String,
    val unit: String,
    val color: Color,
    val kind: DiaryHistorySeriesKind,
    /** The nutrient this line reads, or null for [WEIGHT], which is not one. */
    val nutrient: Nutrient?,
) {
    KCAL_GOAL("Kalorien Soll", "kcal", KcalColor, DiaryHistorySeriesKind.GOAL, Nutrient.KCAL),
    KCAL_ACTUAL("Kalorien Ist", "kcal", KcalColor, DiaryHistorySeriesKind.ACTUAL, Nutrient.KCAL),
    KCAL_AVERAGE("Kalorien Ø", "kcal", KcalColor, DiaryHistorySeriesKind.AVERAGE, Nutrient.KCAL),

    PROTEIN_GOAL("Protein Soll", "g", NutrientColors.getValue(Nutrient.PROTEIN), DiaryHistorySeriesKind.GOAL, Nutrient.PROTEIN),
    PROTEIN_ACTUAL("Protein Ist", "g", NutrientColors.getValue(Nutrient.PROTEIN), DiaryHistorySeriesKind.ACTUAL, Nutrient.PROTEIN),
    PROTEIN_AVERAGE("Protein Ø", "g", NutrientColors.getValue(Nutrient.PROTEIN), DiaryHistorySeriesKind.AVERAGE, Nutrient.PROTEIN),

    CARBS_GOAL("Kohlenhydrate Soll", "g", NutrientColors.getValue(Nutrient.CARBS), DiaryHistorySeriesKind.GOAL, Nutrient.CARBS),
    CARBS_ACTUAL("Kohlenhydrate Ist", "g", NutrientColors.getValue(Nutrient.CARBS), DiaryHistorySeriesKind.ACTUAL, Nutrient.CARBS),
    CARBS_AVERAGE("Kohlenhydrate Ø", "g", NutrientColors.getValue(Nutrient.CARBS), DiaryHistorySeriesKind.AVERAGE, Nutrient.CARBS),

    FAT_GOAL("Fett Soll", "g", NutrientColors.getValue(Nutrient.FAT), DiaryHistorySeriesKind.GOAL, Nutrient.FAT),
    FAT_ACTUAL("Fett Ist", "g", NutrientColors.getValue(Nutrient.FAT), DiaryHistorySeriesKind.ACTUAL, Nutrient.FAT),
    FAT_AVERAGE("Fett Ø", "g", NutrientColors.getValue(Nutrient.FAT), DiaryHistorySeriesKind.AVERAGE, Nutrient.FAT),

    SUGAR_GOAL("Zucker Soll", "g", NutrientColors.getValue(Nutrient.SUGAR), DiaryHistorySeriesKind.GOAL, Nutrient.SUGAR),
    SUGAR_ACTUAL("Zucker Ist", "g", NutrientColors.getValue(Nutrient.SUGAR), DiaryHistorySeriesKind.ACTUAL, Nutrient.SUGAR),
    SUGAR_AVERAGE("Zucker Ø", "g", NutrientColors.getValue(Nutrient.SUGAR), DiaryHistorySeriesKind.AVERAGE, Nutrient.SUGAR),

    SALT_GOAL("Salz Soll", "g", NutrientColors.getValue(Nutrient.SALT), DiaryHistorySeriesKind.GOAL, Nutrient.SALT),
    SALT_ACTUAL("Salz Ist", "g", NutrientColors.getValue(Nutrient.SALT), DiaryHistorySeriesKind.ACTUAL, Nutrient.SALT),
    SALT_AVERAGE("Salz Ø", "g", NutrientColors.getValue(Nutrient.SALT), DiaryHistorySeriesKind.AVERAGE, Nutrient.SALT),

    /**
     * No Soll or Ø counterpart: the app has no weight goal to draw one from, and a weekly mean of a
     * value that already moves in grams would say nothing. Kept out of [NutrientRows] for that
     * reason and given its own row on the screen.
     */
    WEIGHT("Gewicht", "kg", WeightColor, DiaryHistorySeriesKind.ACTUAL, null),
    ;

    val isGoal: Boolean get() = kind == DiaryHistorySeriesKind.GOAL

    val style: ChartLineStyle get() = when (kind) {
        DiaryHistorySeriesKind.GOAL -> ChartLineStyle.DASHED
        DiaryHistorySeriesKind.ACTUAL -> ChartLineStyle.SOLID
        DiaryHistorySeriesKind.AVERAGE -> ChartLineStyle.DOTTED
    }

    /**
     * A dot per day only for what was actually measured that day. Soll and Ø are levels carried
     * across a stretch of days, and dotting every one of them claims daily readings that never
     * happened — besides burying the Ist points they are meant to be read against.
     */
    val showsMarkers: Boolean get() = kind == DiaryHistorySeriesKind.ACTUAL
}

/**
 * One nutrient's three lines. The name is shared — "Protein Soll", "Protein Ist", "Protein Ø" — so
 * the row carries it once and the checkboxes only their mark.
 */
data class NutrientSeriesRow(
    val goal: DiaryHistorySeries,
    val actual: DiaryHistorySeries,
    val average: DiaryHistorySeries,
) {
    val label: String get() = actual.label.removeSuffix(" Ist")
}

/**
 * The checkbox rows, in the order they are shown: one nutrient per row, its Soll, Ist and Ø beside
 * each other. Gewicht is not here — it is a single checkbox below these.
 */
val NutrientRows: List<NutrientSeriesRow> = listOf(
    NutrientSeriesRow(
        DiaryHistorySeries.KCAL_GOAL,
        DiaryHistorySeries.KCAL_ACTUAL,
        DiaryHistorySeries.KCAL_AVERAGE,
    ),
    NutrientSeriesRow(
        DiaryHistorySeries.PROTEIN_GOAL,
        DiaryHistorySeries.PROTEIN_ACTUAL,
        DiaryHistorySeries.PROTEIN_AVERAGE,
    ),
    NutrientSeriesRow(
        DiaryHistorySeries.CARBS_GOAL,
        DiaryHistorySeries.CARBS_ACTUAL,
        DiaryHistorySeries.CARBS_AVERAGE,
    ),
    NutrientSeriesRow(
        DiaryHistorySeries.FAT_GOAL,
        DiaryHistorySeries.FAT_ACTUAL,
        DiaryHistorySeries.FAT_AVERAGE,
    ),
    NutrientSeriesRow(
        DiaryHistorySeries.SUGAR_GOAL,
        DiaryHistorySeries.SUGAR_ACTUAL,
        DiaryHistorySeries.SUGAR_AVERAGE,
    ),
    NutrientSeriesRow(
        DiaryHistorySeries.SALT_GOAL,
        DiaryHistorySeries.SALT_ACTUAL,
        DiaryHistorySeries.SALT_AVERAGE,
    ),
)

/**
 * True when everything selected reads the same nutrient — Kalorien Soll, Ist and Ø, say. Only then
 * can the chart put all lines on one scale and label it in fine steps: mixing two nutrients (or
 * Gewicht) back onto one axis would flatten the smaller of them.
 */
fun isSingleNutrientSelection(selected: Set<DiaryHistorySeries>): Boolean =
    selected.isNotEmpty() && selected.mapTo(mutableSetOf()) { it.nutrient }.singleOrNull() != null
