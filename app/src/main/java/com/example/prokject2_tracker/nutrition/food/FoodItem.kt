package com.example.prokject2_tracker.nutrition.food

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class BaseUnit { G, ML }

/** A user-created Lebensmittel. Macros are stored per 100 [baseUnit] (100 g or 100 ml). */
@Entity(tableName = "food_items", indices = [Index("name")])
data class FoodItem(
    @PrimaryKey val id: String,
    val name: String,
    val baseUnit: BaseUnit,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    /** Optional named serving (e.g. "Scheibe") purely as a logging-UI convenience. */
    val servingName: String? = null,
    /** Amount of [baseUnit] that one [servingName] corresponds to. */
    val servingAmount: Double? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
