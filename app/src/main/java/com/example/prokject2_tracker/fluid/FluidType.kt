package com.example.prokject2_tracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A user-manageable drink type (Wasser, Kaffee, Milch, ...), part of the Getränkearten library.
 * [sortOrder] keeps the user's/default ordering stable regardless of creation order.
 */
@Entity(tableName = "fluid_types", indices = [Index("name")])
data class FluidType(
    @PrimaryKey val id: String,
    val name: String,
    val defaultQuickAddMl: Double,
    val sortOrder: Int,
    val createdAt: Instant,
)
