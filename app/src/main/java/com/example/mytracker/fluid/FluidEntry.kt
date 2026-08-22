package com.example.prokject2_tracker.fluid

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * [fluidTypeName] is snapshotted at logging time so renaming/deleting the type later never changes
 * history. [fluidUnitId]/[fluidUnitName] are null for entries added via a quick-add chip (which log
 * a type's default amount directly, with no Maßeinheit involved).
 *
 * [sourceDiaryEntryId] is set only for entries the app derived automatically from a Tagebuch entry
 * whose Lebensmittel is linked to a Getränkeart; it lets that fluid row follow the diary entry when
 * it is edited or deleted. Null for everything the user logged directly in the Flüssigkeiten tab.
 */
@Entity(
    tableName = "fluid_entries",
    indices = [Index("epochDay"), Index("fluidTypeId"), Index("fluidUnitId"), Index("sourceDiaryEntryId")],
)
data class FluidEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val fluidTypeId: String,
    val fluidTypeName: String,
    val amountMl: Double,
    val fluidUnitId: String? = null,
    val fluidUnitName: String? = null,
    val sourceDiaryEntryId: String? = null,
)
