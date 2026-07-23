package com.example.prokject2_tracker.nutrition.food

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A user-created label (e.g. "vegan", "Obst") attachable to Lebensmittel. */
@Entity(tableName = "tags", indices = [Index("name", unique = true)])
data class Tag(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Instant,
)
