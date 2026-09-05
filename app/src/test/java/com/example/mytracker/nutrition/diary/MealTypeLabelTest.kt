package com.example.mytracker.nutrition.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The four meals as the Hinzufügen-Fenster shows them, side by side in one row. */
class MealTypeLabelTest {
    @Test
    fun everyMealHasAShortName() {
        // The chips are laid out from these, so an empty one would leave a chip with nothing in it.
        MealType.entries.forEach { type ->
            assertTrue(type.shortLabel().isNotBlank())
        }
    }

    @Test
    fun theShortNamesAreShorterThanTheFullOnes() {
        assertEquals("Früh", MealType.BREAKFAST.shortLabel())
        assertEquals("Mittag", MealType.LUNCH.shortLabel())
        assertEquals("Abend", MealType.DINNER.shortLabel())
    }

    @Test
    fun snackKeepsItsFullNameForTheScreenReader() {
        // Its chip is a picture, so the spoken name is the only name it has.
        assertEquals("Snack", MealType.SNACK.label())
    }
}
