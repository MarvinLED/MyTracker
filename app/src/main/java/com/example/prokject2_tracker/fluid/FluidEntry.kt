package com.example.prokject2_tracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** [fluidTypeName] is snapshotted at logging time so renaming/deleting the type later never changes history. */
@Entity(tableName = "fluid_entries", indices = [Index("epochDay"), Index("fluidTypeId")])
data class FluidEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val fluidTypeId: String,
    val fluidTypeName: String,
    val amountMl: Double,
)
