package com.example.prokject2_tracker.nutrition.diary

import java.time.LocalTime

/**
 * Which meal the "Lebensmittel hinzufügen" button starts on: whatever one would be eating at
 * [time]. The gaps between the meals — and everything outside 5–20 Uhr — are Snack, because food
 * logged at 15 Uhr is a snack far more often than it is a late lunch.
 *
 * Boundaries are half-open, so 10:00 is already Mittagessen. A pure function of the time so it can
 * be tested without a clock.
 */
fun defaultMealType(time: LocalTime): MealType = when {
    time < LocalTime.of(5, 0) -> MealType.SNACK
    time < LocalTime.of(10, 0) -> MealType.BREAKFAST
    time < LocalTime.of(14, 0) -> MealType.LUNCH
    time < LocalTime.of(16, 30) -> MealType.SNACK
    time < LocalTime.of(20, 0) -> MealType.DINNER
    else -> MealType.SNACK
}
