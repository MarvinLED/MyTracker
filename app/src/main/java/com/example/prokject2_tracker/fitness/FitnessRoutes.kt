package com.example.prokject2_tracker.fitness

import kotlinx.serialization.Serializable

@Serializable
data object FitnessRoute

@Serializable
data class TrainingEntryRoute(val cardioSessionId: String? = null, val strengthLogEntryId: String? = null)
