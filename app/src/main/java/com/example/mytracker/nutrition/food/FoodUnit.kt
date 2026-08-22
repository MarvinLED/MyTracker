package com.example.mytracker.nutrition.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A named amount of one specific Lebensmittel (e.g. "Scheibe" = 25 g), so logging can say
 * "2 × Scheibe" instead of "50 g". Purely a logging-UI convenience: everything downstream still
 * computes in [FoodItem.baseUnit], the unit only decides what the user types.
 *
 * Belongs to exactly one food and is never referenced by id from anywhere else — logged entries
 * snapshot the name instead — so unlike `FluidUnit` this needs no manage screen and no delete guard.
 */
@Entity(
    tableName = "food_units",
    foreignKeys = [
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("foodItemId")],
)
data class FoodUnit(
    @PrimaryKey val id: String,
    val foodItemId: String,
    val name: String,
    /** How many of the food's base units (g/ml) one of these is. */
    val amountBaseUnits: Double,
    val sortOrder: Int = 0,
)
