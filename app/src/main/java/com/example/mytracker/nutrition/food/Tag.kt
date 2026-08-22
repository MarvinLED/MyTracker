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
    /**
     * The colour its dot is drawn in, or null for "automatisch" — then it falls back to the palette
     * slot for its position in the library, see [displayColor]. Defaulted because tags are still
     * created as a by-product of typing a name in the Lebensmittel editor, which picks no colour.
     */
    val colorArgb: Int? = null,
)
