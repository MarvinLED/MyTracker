package com.example.prokject2_tracker.core.database

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromEpochMillis(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()
}
