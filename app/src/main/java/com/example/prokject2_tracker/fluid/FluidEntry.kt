package com.example.prokject2_tracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class FluidType { WATER, COFFEE, TEA, JUICE, SODA, OTHER }

@Entity(tableName = "fluid_entries", indices = [Index("epochDay")])
data class FluidEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val type: FluidType,
    val amountMl: Double,
)
