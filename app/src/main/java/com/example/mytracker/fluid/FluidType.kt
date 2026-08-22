package com.example.mytracker.fluid

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
    /**
     * Colour this type is drawn with in the Flüssigkeiten pie charts, as a packed ARGB int. Null
     * means "not chosen yet" and falls back to a fixed palette slot — see
     * [com.example.mytracker.fluid.chartColor].
     */
    val colorArgb: Int? = null,
)
