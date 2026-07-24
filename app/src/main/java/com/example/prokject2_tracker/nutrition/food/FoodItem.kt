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
    /** Optional brand/manufacturer (e.g. "Alnatura") purely as a logging-UI convenience. */
    val brand: String? = null,
    val baseUnit: BaseUnit,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val saturatedFatPer100: Double = 0.0,
    val sugarPer100: Double = 0.0,
    val fiberPer100: Double = 0.0,
    val saltPer100: Double = 0.0,
    /** Optional named serving (e.g. "Scheibe") purely as a logging-UI convenience. */
    val servingName: String? = null,
    /** Amount of [baseUnit] that one [servingName] corresponds to. */
    val servingAmount: Double? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
