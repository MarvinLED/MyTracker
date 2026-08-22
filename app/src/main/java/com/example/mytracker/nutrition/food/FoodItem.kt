package com.example.mytracker.nutrition.food

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class BaseUnit { G, ML }

/**
 * A user-created Lebensmittel. Macros are stored per 100 [baseUnit] (100 g or 100 ml).
 *
 * Named servings ("Scheibe", "Stück") are not fields here — a food can have any number of them, see
 * [FoodUnit].
 */
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
    /**
     * Optional link into the Getränkearten library: this food (partly) *consists of* that fluid,
     * so logging it to the diary also logs a fluid entry. Null means the food isn't a drink.
     */
    val fluidTypeId: String? = null,
    /**
     * How many ml of [fluidTypeId] 100 [baseUnit] of this food contain — 100.0 means "consists
     * entirely of it" (a drink), less covers e.g. a soup. Null is read as 100.0.
     */
    val fluidMlPer100: Double? = null,
    /**
     * Optional price in €, for the amount [priceUnitName] names. Null means no price recorded —
     * 0.0 would claim the food is free.
     */
    val price: Double? = null,
    /**
     * What [price] is the price *of*: null means 100 [baseUnit], otherwise the name of one of this
     * food's [FoodUnit]s ("Packung"). The name rather than the unit id, because [FoodUnit] rows are
     * replaced wholesale on every save (see `FoodRepository.setUnits`) and their ids don't survive
     * it — the same reason logged entries snapshot the name.
     */
    val priceUnitName: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
