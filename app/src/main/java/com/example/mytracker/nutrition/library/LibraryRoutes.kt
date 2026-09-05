package com.example.mytracker.nutrition.library

import com.example.mytracker.nutrition.diary.MealType
import kotlinx.serialization.Serializable

/**
 * The Bibliothek as reached from the drawer: no day of its own, so anything logged from it lands on
 * today, in the meal the clock suggests.
 */
@Serializable
data object LibraryRoute

/**
 * The same screen, reached from a meal's "+" in the Tagebuch and carrying that day and meal with it.
 *
 * Deliberately a second route rather than optional fields on [LibraryRoute]: this one is a detail
 * destination that Zurück returns from, while [LibraryRoute] is a top-level one the drawer marks as
 * selected. One route trying to be both would have to be either pushed or switched to depending on
 * where it came from.
 */
@Serializable
data class LibraryLogRoute(val epochDay: Long, val mealType: MealType)
