package com.example.mytracker.nutrition.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun unit(amountBaseUnits: Double, name: String = "Stück") =
    FoodUnit(id = "u1", foodItemId = "f1", name = name, amountBaseUnits = amountBaseUnits)

/**
 * Switching the unit chips prefills the field instead of converting the old amount: tapping "Stück"
 * means "one of those", not "100 g re-expressed as 3,333 Stück". What the user then confirms still
 * has to land in the entry as base units, so both halves are pinned here.
 */
class FoodAmountInputTest {
    @Test
    fun defaultAmountText_prefills100ForTheBaseUnit() {
        assertEquals("100", defaultAmountText(null))
    }

    @Test
    fun defaultAmountText_prefills1ForANamedUnitWhateverItWeighs() {
        assertEquals("1", defaultAmountText(unit(30.0)))
        assertEquals("1", defaultAmountText(unit(0.5, name = "Prise")))
        assertEquals("1", defaultAmountText(unit(250.0, name = "Packung")))
    }

    @Test
    fun thePrefilledAmountsConvertToTheExpectedBaseUnits() {
        assertEquals(100.0, amountInBaseUnits(defaultAmountText(null), null)!!, 0.0001)
        val stueck = unit(30.0)
        assertEquals(30.0, amountInBaseUnits(defaultAmountText(stueck), stueck)!!, 0.0001)
    }

    @Test
    fun amountInBaseUnits_scalesByTheUnitAndAcceptsAGermanComma() {
        assertEquals(75.0, amountInBaseUnits("2,5", unit(30.0))!!, 0.0001)
        assertEquals(2.5, amountInBaseUnits("2.5", null)!!, 0.0001)
    }

    @Test
    fun amountInBaseUnits_isNullWhenTheFieldIsntANumber() {
        assertNull(amountInBaseUnits("", null))
        assertNull(amountInBaseUnits("abc", unit(30.0)))
    }

    @Test
    fun aWeightlessFoodCountsPortionsEvenWithNoUnitPicked() {
        // The failure this guards against is silent and hundredfold: without the portion name a
        // typed "2" reads as two grams, and a bar's 230 kcal arrives in the diary as 4,6.
        assertEquals(200.0, amountInBaseUnits("2", null, "Riegel")!!, 0.0001)
        assertEquals(100.0, amountInBaseUnits("1", null, "Riegel")!!, 0.0001)
        assertEquals(50.0, amountInBaseUnits("0,5", null, "Riegel")!!, 0.0001)
    }

    @Test
    fun aFoodWithAWeightIsUnaffectedByThePortionAwareOverload() {
        assertEquals(2.5, amountInBaseUnits("2,5", null, null)!!, 0.0001)
        assertEquals(75.0, amountInBaseUnits("2,5", unit(30.0), null)!!, 0.0001)
        // An explicitly picked unit always wins — that is the amount the user chose.
        assertEquals(75.0, amountInBaseUnits("2,5", unit(30.0), "Riegel")!!, 0.0001)
    }

    @Test
    fun aWeightlessFoodStartsAtOnePortionNotAtAHundred() {
        assertEquals("1", defaultAmountText(null, "Riegel"))
        assertEquals("100", defaultAmountText(null, null))
    }

    @Test
    fun aPortionAmountNamesNoGrams() {
        // "2 × Riegel (200 g)" would state a weight that was never known — the one thing such a
        // food exists to avoid.
        assertEquals("2 × Riegel", formatPortionAmount(200.0, "Riegel", 2.0, "Riegel"))
        // Even without the snapshotted count, which is what the base amount is there for.
        assertEquals("3 × Riegel", formatPortionAmount(300.0, null, null, "Riegel"))
    }
}
