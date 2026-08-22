package com.example.mytracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A user-manageable measurement unit for logging fluids (e.g. "Glas" = 400 ml), part of the
 * Maßeinheiten library. [sortOrder] keeps ordering stable regardless of creation order.
 */
@Entity(tableName = "fluid_units", indices = [Index("name")])
data class FluidUnit(
    @PrimaryKey val id: String,
    val name: String,
    val amountMl: Double,
    val sortOrder: Int,
    val createdAt: Instant,
)
