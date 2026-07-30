package com.example.prokject2_tracker.measurement

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A place on the body that gets measured ("Oberarm links", "Taille"), part of the Körperstellen
 * library — user-created, never seeded, mirroring [com.example.prokject2_tracker.fluid.FluidType].
 *
 * [measuringHint] is the "so wird gemessen" note written when the site is created: where exactly to
 * put the tape, at which arm position, before or after training. It's shown on demand while logging
 * (tap the name), because measuring the same spot the same way is what makes the trend mean
 * anything. Null when nothing was written down.
 *
 * [sortOrder] keeps the entry rows and the chart's colour assignment stable regardless of creation
 * order.
 */
@Entity(tableName = "body_sites", indices = [Index("name")])
data class BodySite(
    @PrimaryKey val id: String,
    val name: String,
    val measuringHint: String?,
    val sortOrder: Int,
    val createdAt: Instant,
)
