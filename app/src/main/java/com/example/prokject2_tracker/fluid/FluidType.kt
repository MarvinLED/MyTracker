package com.example.prokject2_tracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A user-manageable drink type (Wasser, Kaffee, Milch, ...), part of the Getränkearten library.
 * [sortOrder] keeps the user's/default ordering stable regardless of creation order.
 * [dailyGoalMinMl]/[dailyGoalMaxMl] are optional per-type daily goals (e.g. "drink at least 500 ml
 * Wasser" / "drink at most 200 ml Limonade"), set from the Ziele screen; null means no goal.
 */
@Entity(tableName = "fluid_types", indices = [Index("name")])
data class FluidType(
    @PrimaryKey val id: String,
    val name: String,
    val defaultQuickAddMl: Double,
    val sortOrder: Int,
    val createdAt: Instant,
    val dailyGoalMinMl: Double? = null,
    val dailyGoalMaxMl: Double? = null,
)
