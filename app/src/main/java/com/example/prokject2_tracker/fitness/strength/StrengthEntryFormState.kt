package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.DateUtils

data class StrengthSetInput(
    val reps: String = "",
    val weightText: String = "",
    val weightManuallyEdited: Boolean = false,
)

data class StrengthEntryFormState(
    val epochDay: Long = DateUtils.todayEpochDay(),
    val exerciseId: String? = null,
    val exerciseName: String = "",
    val sets: List<StrengthSetInput> = listOf(StrengthSetInput()),
    val note: String = "",
) {
    val isValid: Boolean
        get() = exerciseId != null && sets.isNotEmpty() &&
            sets.all { it.reps.toIntOrNull()?.let { r -> r > 0 } == true } &&
            sets.all { it.weightText.isBlank() || it.weightText.toDoubleOrNull()?.let { w -> w >= 0.0 } == true }
}
