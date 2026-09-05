package com.example.mytracker.achievements

import androidx.room.Entity
import java.time.Instant

/**
 * One day's earnings for one attribute, booked once and never rewritten.
 *
 * Booked rather than derived on the fly for two reasons. Goal history only exists for Nährwerte and
 * Fitness — lowering the water goal today would otherwise re-judge every past day and could erase a
 * record that has already been shown. And the all-time best needs the whole history evaluated, so
 * computing it once per day beats computing it on every screen open.
 *
 * A day is written with a row for **every** attribute, zero included, so the days that have been
 * settled are exactly the days present in the table — otherwise an empty day would be recomputed
 * forever.
 */
@Entity(tableName = "game_day_points", primaryKeys = ["epochDay", "attribute"])
data class GameDayPoints(
    val epochDay: Long,
    val attribute: AvatarAttribute,
    val points: Double,
    /** When the booking happened — the audit trail for a row that is deliberately never updated. */
    val bookedAt: Instant,
)
