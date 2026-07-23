package com.example.prokject2_tracker.habit

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A user-defined habit to check in on daily. Definitions are library data; check-ins are not. */
@Entity(tableName = "habits", indices = [Index("name")])
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
