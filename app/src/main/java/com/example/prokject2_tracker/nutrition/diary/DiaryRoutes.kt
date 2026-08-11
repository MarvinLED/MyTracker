package com.example.prokject2_tracker.nutrition.diary

import kotlinx.serialization.Serializable

@Serializable
data object DiaryRoute

@Serializable
data class DiaryAddEntryRoute(val epochDay: Long, val mealType: MealType)

@Serializable
data class DiaryEditEntryRoute(val entryId: String)

/**
 * The Verlauf. A plain detail destination, not a top-level one: it is reached from the Tagebuch's
 * own button and appears in neither the bottom bar nor the drawer, so it navigates with a plain
 * `navigate` rather than through `navigateToTopLevel`.
 */
@Serializable
data object DiaryHistoryRoute
