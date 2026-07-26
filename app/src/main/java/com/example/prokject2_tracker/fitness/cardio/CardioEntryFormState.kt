package com.example.prokject2_tracker.fitness.cardio

import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull

data class CardioEditState(
    val id: String? = null,
    val epochDay: Long = DateUtils.todayEpochDay(),
    val activityTypeId: String? = null,
    val activityTypeName: String = "",
    val durationMinutes: String = "",
    val distanceKm: String = "",
    val caloriesBurned: String = "",
    val avgHeartRateBpm: String = "",
    val note: String = "",
) {
    val isValid: Boolean
        get() = activityTypeId != null &&
            durationMinutes.toLocaleDoubleOrNull()?.let { it > 0.0 } == true &&
            (distanceKm.isBlank() || distanceKm.toLocaleDoubleOrNull()?.let { it >= 0.0 } == true) &&
            (caloriesBurned.isBlank() || caloriesBurned.toLocaleDoubleOrNull()?.let { it >= 0.0 } == true) &&
            (avgHeartRateBpm.isBlank() || avgHeartRateBpm.toIntOrNull()?.let { it > 0 } == true)
}
