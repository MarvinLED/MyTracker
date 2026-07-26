package com.example.prokject2_tracker.fitness

import kotlinx.serialization.Serializable

@Serializable
data object FitnessRoute

/** The chronological training log, moved off the Fitness screen when the exercise list took its place. */
@Serializable
data object TrainingHistoryRoute
