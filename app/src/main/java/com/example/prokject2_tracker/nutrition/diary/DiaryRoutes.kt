package com.example.prokject2_tracker.nutrition.diary

import kotlinx.serialization.Serializable

@Serializable
data object DiaryRoute

@Serializable
data class DiaryAddEntryRoute(val epochDay: Long, val mealType: MealType)

@Serializable
data class DiaryEditEntryRoute(val entryId: String)
