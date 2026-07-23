package com.example.prokject2_tracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * [fluidTypeName] is snapshotted at logging time so renaming/deleting the type later never changes
 * history. [fluidUnitId]/[fluidUnitName] are null for entries added via a quick-add chip (which log
 * a type's default amount directly, with no Maßeinheit involved).
 */
@Entity(tableName = "fluid_entries", indices = [Index("epochDay"), Index("fluidTypeId"), Index("fluidUnitId")])
data class FluidEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val fluidTypeId: String,
    val fluidTypeName: String,
    val amountMl: Double,
    val fluidUnitId: String? = null,
    val fluidUnitName: String? = null,
)
