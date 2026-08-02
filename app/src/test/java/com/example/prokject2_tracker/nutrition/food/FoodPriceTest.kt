package com.example.prokject2_tracker.nutrition.food

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val AnInstant: Instant = Instant.ofEpochMilli(1_700_000_000_000)

private fun food(price: Double?, priceUnitName: String? = null, baseUnit: BaseUnit = BaseUnit.G) = FoodItem(
    id = "f1",
    name = "Toastbrot",
    baseUnit = baseUnit,
    kcalPer100 = 250.0,
    proteinPer100 = 9.0,
    carbsPer100 = 46.0,
    fatPer100 = 3.0,
    price = price,
    priceUnitName = priceUnitName,
    createdAt = AnInstant,
    updatedAt = AnInstant,
)

/**
 * A price is stored as entered ("2,49 pro Packung"), so the €/100 g needed to compare two foods is
 * always a conversion — that conversion and the two ways a price reads back are pinned here.
 */
class FoodPriceTest {
    @Test
    fun pricePer100_returnsThePriceUnchangedWhenItAlreadyIsPer100() {
        assertEquals(0.89, pricePer100(0.89, null)!!, 0.0001)
    }

    @Test
    fun pricePer100_scalesAUnitPriceToTheBaseAmount() {
        // 2,49 € für eine Packung à 500 g -> 0,498 € pro 100 g.
        assertEquals(0.498, pricePer100(2.49, 500.0)!!, 0.0001)
        // Eine Einheit unter 100 g macht 100 g teurer als die Einheit.
        assertEquals(9.96, pricePer100(2.49, 25.0)!!, 0.0001)
    }

    @Test
    fun pricePer100_isNullWithoutAPriceOrWithAnUnusableBasis() {
        assertNull(pricePer100(null, 500.0))
        assertNull(pricePer100(2.49, 0.0))
        assertNull(pricePer100(2.49, -10.0))
    }

    @Test
    fun formatPrice_namesWhatThePriceIsFor() {
        assertEquals("0,89 € / 100 g", food(price = 0.89).formatPrice())
        assertEquals("2,49 € / Packung", food(price = 2.49, priceUnitName = "Packung").formatPrice())
        assertEquals("1,20 € / 100 ml", food(price = 1.2, baseUnit = BaseUnit.ML).formatPrice())
    }

    @Test
    fun formatPrice_isNullForAFoodWithoutAPrice() {
        assertNull(food(price = null).formatPrice())
    }

    @Test
    fun priceBasisLabel_fallsBackToTheFoodsOwnBaseUnit() {
        assertEquals("100 g", priceBasisLabel(null, BaseUnit.G))
        assertEquals("100 ml", priceBasisLabel(null, BaseUnit.ML))
        assertEquals("Scheibe", priceBasisLabel("Scheibe", BaseUnit.G))
    }
}
