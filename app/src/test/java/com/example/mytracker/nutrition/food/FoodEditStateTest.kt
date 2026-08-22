package com.example.prokject2_tracker.nutrition.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun state(
    price: String = "",
    priceUnitName: String? = null,
    units: List<UnitRow> = emptyList(),
) = FoodEditState(name = "Toastbrot", kcalPer100 = "250", price = price, priceUnitName = priceUnitName, units = units)

/** The price half of the Lebensmittel form: which units it can refer to, and what it converts to. */
class FoodEditStateTest {
    @Test
    fun priceUnitOptions_offerOnlyFullyFilledUnitRows() {
        val options = state(
            units = listOf(
                UnitRow(name = "Scheibe", amount = "25"),
                UnitRow(name = "Packung", amount = ""),
                UnitRow(name = "", amount = "500"),
            ),
        ).priceUnitOptions

        assertEquals(listOf("Scheibe"), options.map { it.name })
    }

    @Test
    fun pricePer100Hint_convertsAUnitPriceToThe100gComparison() {
        val hint = state(
            price = "2,49",
            priceUnitName = "Packung",
            units = listOf(UnitRow(name = "Packung", amount = "500")),
        ).pricePer100Hint

        assertEquals("≈ 0,50 € / 100 g", hint)
    }

    @Test
    fun pricePer100Hint_staysAwayWhileThereIsNothingToConvert() {
        // Preis pro 100 g braucht keine Umrechnung.
        assertNull(state(price = "0,89").pricePer100Hint)
        // Einheit ausgewählt, aber (noch) kein Preis getippt.
        assertNull(
            state(priceUnitName = "Packung", units = listOf(UnitRow(name = "Packung", amount = "500")))
                .pricePer100Hint,
        )
        // Die Einheit hat (noch) keine Menge, also gibt es keine Basis.
        assertNull(
            state(price = "2,49", priceUnitName = "Packung", units = listOf(UnitRow(name = "Packung")))
                .pricePer100Hint,
        )
    }

    @Test
    fun aPriceMustParseButMayBeLeftEmpty() {
        assertTrue(state().isValid)
        assertTrue(state(price = "2,49").isValid)
        assertTrue(state(price = "2.49").isValid)
        assertFalse(state(price = "teuer").isValid)
    }
}
