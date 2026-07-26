package com.example.prokject2_tracker.fitness.cardio

import kotlinx.serialization.Serializable

@Serializable
data object CardioActivityTypeManageRoute

/**
 * The per-activity page. [sessionId] preselects one of a day's sessions when arriving from the
 * training history — a day can legitimately hold several runs, unlike a strength session.
 */
@Serializable
data class CardioActivityDetailRoute(
    val activityTypeId: String,
    val epochDay: Long,
    val sessionId: String? = null,
)
