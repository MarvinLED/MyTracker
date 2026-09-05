package com.example.mytracker.nutrition.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Bezugsgröße: a packet that states 230 kcal per bar and nothing per 100 g.
 *
 * Two outcomes hang on one blank field, so both are pinned here — with a weight the values are
 * converted and the food stays an ordinary, comparable one; without a weight the food loses grams
 * altogether and its portion becomes the only amount it has.
 */
class FoodReferencePortionTest {
    private fun state(
        kcal: String = "230",
        units: List<UnitRow> = emptyList(),
        referenceUnitIndex: Int? = null,
        price: String = "",
        priceUnitName: String? = null,
    ) = FoodEditState(
        name = "Proteinriegel",
        kcalPer100 = kcal,
        units = units,
        referenceUnitIndex = referenceUnitIndex,
        price = price,
        priceUnitName = priceUnitName,
    )

    private val weighed = listOf(UnitRow(name = "Riegel", amount = "45"))
    private val weightless = listOf(UnitRow(name = "Riegel", amount = ""))

    @Test
    fun withoutAReferenceNothingChanges() {
        val s = state(units = weighed)

        assertEquals("100 g", s.valueBasisLabel)
        assertFalse(s.isPortionOnly)
        assertEquals(1.0, s.storageFactor, 0.0001)
        assertNull(s.savedPortionUnitName)
        assertTrue(s.isValid)
    }

    @Test
    fun aWeighedPortionIsConvertedToPer100() {
        val s = state(units = weighed, referenceUnitIndex = 0)

        // 230 kcal per 45 g is 511,1 kcal per 100 g — the food is an ordinary one afterwards.
        assertEquals(100.0 / 45.0, s.storageFactor, 0.0001)
        assertEquals(511.1, 230.0 * s.storageFactor, 0.05)
        assertFalse(s.isPortionOnly)
        assertNull(s.savedPortionUnitName)
        assertTrue(s.isValid)
    }

    @Test
    fun theConversionIsShownBeforeItHappens() {
        val s = state(units = weighed, referenceUnitIndex = 0)

        assertEquals("≈ 511,1 kcal / 100 g", s.kcalPer100Hint)
    }

    @Test
    fun aPortionWithoutAWeightIsStoredExactlyAsTyped() {
        val s = state(units = weightless, referenceUnitIndex = 0)

        assertTrue(s.isPortionOnly)
        // One portion is 100 base units, so "per portion" and "per 100" are the same number and
        // nothing downstream has to know such foods exist.
        assertEquals(1.0, s.storageFactor, 0.0001)
        assertEquals("Riegel", s.savedPortionUnitName)
        assertEquals("Riegel", s.valueBasisLabel)
        assertTrue(s.isValid)
    }

    @Test
    fun thereIsNothingToConvertWithoutAWeight() {
        assertNull(state(units = weightless, referenceUnitIndex = 0).kcalPer100Hint)
    }

    @Test
    fun theReferencePortionStillNeedsAName() {
        val s = state(units = listOf(UnitRow(name = "", amount = "")), referenceUnitIndex = 0)

        // The name is what the portion is called everywhere afterwards, so it is the one part that
        // cannot be left out.
        assertFalse(s.isValid)
        assertFalse(s.isPortionOnly)
    }

    @Test
    fun aWeightlessPortionLeavesNoRoomForOtherUnits() {
        val s = state(
            units = listOf(UnitRow(name = "Riegel", amount = ""), UnitRow(name = "Packung", amount = "135")),
            referenceUnitIndex = 0,
        )

        // Grams are gone, so a gram amount beside the portion is a number nobody can convert.
        assertFalse(s.isValid)
        assertEquals(listOf("Packung"), s.otherUnits.map { it.name })
    }

    @Test
    fun aWeighedPortionHappilyStandsBesideOtherUnits() {
        val s = state(
            units = listOf(UnitRow(name = "Riegel", amount = "45"), UnitRow(name = "Packung", amount = "135")),
            referenceUnitIndex = 0,
        )

        // Everything is still in grams here, so everything is still comparable.
        assertTrue(s.isValid)
    }

    @Test
    fun aHalfFilledRowStillBlocksSaving() {
        val s = state(
            units = listOf(UnitRow(name = "Riegel", amount = "45"), UnitRow(name = "Packung", amount = "")),
            referenceUnitIndex = 0,
        )

        assertFalse(s.isValid)
    }

    @Test
    fun theReferencePortionCanCarryThePrice() {
        val s = state(units = weightless, referenceUnitIndex = 0, price = "1,29", priceUnitName = "Riegel")

        // It is the only basis such a food has — there is no per-100-g price to fall back to.
        assertEquals(listOf("Riegel"), s.priceUnitOptions.map { it.name })
        assertTrue(s.isValid)
        // And nothing to convert it to, so no comparison value is claimed.
        assertNull(s.pricePer100Hint)
    }
}
