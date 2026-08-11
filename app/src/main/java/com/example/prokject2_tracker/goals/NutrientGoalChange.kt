package com.example.prokject2_tracker.goals

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.prokject2_tracker.core.datastore.Nutrient
import java.time.Instant

/**
 * One recorded change to a nutrient's daily goal, so the Verlauf can draw *when* the target moved
 * rather than pretending today's target always applied.
 *
 * The goal itself still lives in DataStore — this table is a log beside it, not the source of
 * truth. It exists because DataStore overwrites in place: before this, changing a goal erased the
 * previous one without trace, and nothing in the app could reconstruct it. Rows are therefore only
 * as old as the feature; everything before the first recorded change is genuinely unknown, and
 * [nutrientGoalTimeline] extends the oldest known value backwards rather than inventing one.
 *
 * [effectiveFromEpochDay] is the day the new bounds started applying, not the day they were typed
 * — a seed row written on a nutrient's first-ever change carries day 0, meaning "as far back as
 * this log goes". [minValue]/[maxValue] mirror
 * [NutrientGoal][com.example.prokject2_tracker.core.datastore.NutrientGoal]; both null records that
 * the goal was cleared.
 */
@Entity(
    tableName = "nutrient_goal_changes",
    indices = [Index("nutrient", "effectiveFromEpochDay")],
)
data class NutrientGoalChange(
    @PrimaryKey val id: String,
    val nutrient: Nutrient,
    val effectiveFromEpochDay: Long,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val changedAt: Instant,
)
