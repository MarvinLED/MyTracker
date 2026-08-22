package com.example.mytracker.nutrition.diary

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultMealTypeTest {
    @Test
    fun defaultMealType_followsTheTimeOfDay() {
        assertEquals(MealType.BREAKFAST, defaultMealType(LocalTime.of(5, 0)))
        assertEquals(MealType.BREAKFAST, defaultMealType(LocalTime.of(9, 59)))
        assertEquals(MealType.LUNCH, defaultMealType(LocalTime.of(10, 0)))
        assertEquals(MealType.LUNCH, defaultMealType(LocalTime.of(13, 59)))
        assertEquals(MealType.DINNER, defaultMealType(LocalTime.of(16, 30)))
        assertEquals(MealType.DINNER, defaultMealType(LocalTime.of(19, 59)))
    }

    @Test
    fun defaultMealType_isSnackBetweenAndAroundTheMeals() {
        assertEquals(MealType.SNACK, defaultMealType(LocalTime.of(0, 0)))
        assertEquals(MealType.SNACK, defaultMealType(LocalTime.of(4, 59)))
        assertEquals(MealType.SNACK, defaultMealType(LocalTime.of(14, 0)))
        assertEquals(MealType.SNACK, defaultMealType(LocalTime.of(16, 29)))
        assertEquals(MealType.SNACK, defaultMealType(LocalTime.of(20, 0)))
        assertEquals(MealType.SNACK, defaultMealType(LocalTime.of(23, 59)))
    }
}
