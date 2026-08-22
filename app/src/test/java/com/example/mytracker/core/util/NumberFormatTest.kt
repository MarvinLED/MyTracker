package com.example.mytracker.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Parsing and formatting are two halves of one contract: whatever [formatCompact] and [formatDecimal]
 * put into a text field, [toLocaleDoubleOrNull] has to read back. That round trip is what these pin
 * down — a German decimal comma used to be rejected by most of the app's number fields.
 */
class NumberFormatTest {
    @Test
    fun formatCompact_dropsTheDecimalsOnWholeNumbers() {
        assertEquals("52", 52.0.formatCompact())
        assertEquals("0", 0.0.formatCompact())
        assertEquals("-3", (-3.0).formatCompact())
    }

    @Test
    fun formatCompact_usesAGermanDecimalCommaRegardlessOfTheSystemLocale() {
        assertEquals("52,3", 52.3.formatCompact())
        assertEquals("0,5", 0.5.formatCompact())
    }

    @Test
    fun formatDecimal_trimsTrailingZerosButKeepsSignificantOnes() {
        assertEquals("1,5", 1.5.formatDecimal(3))
        assertEquals("0,03", 0.03.formatDecimal(3))
        assertEquals("2", 2.0.formatDecimal(3))
        assertEquals("250", 250.0.formatDecimal(3))
    }

    @Test
    fun toLocaleDoubleOrNull_acceptsBothSeparators() {
        assertEquals(2.5, "2,5".toLocaleDoubleOrNull()!!, 0.0001)
        assertEquals(2.5, "2.5".toLocaleDoubleOrNull()!!, 0.0001)
        assertEquals(100.0, "100".toLocaleDoubleOrNull()!!, 0.0001)
    }

    @Test
    fun toLocaleDoubleOrNull_toleratesSurroundingWhitespace() {
        assertEquals(7.0, " 7 ".toLocaleDoubleOrNull()!!, 0.0001)
    }

    @Test
    fun toLocaleDoubleOrNull_rejectsWhatIsntANumber() {
        assertNull("".toLocaleDoubleOrNull())
        assertNull("   ".toLocaleDoubleOrNull())
        assertNull("abc".toLocaleDoubleOrNull())
        assertNull("2,5,5".toLocaleDoubleOrNull())
    }

    @Test
    fun formattedValuesParseBackToThemselves() {
        listOf(0.0, 1.0, 2.5, 52.3, 250.0, 1234.5).forEach { value ->
            assertEquals(value, value.formatCompact().toLocaleDoubleOrNull()!!, 0.05)
            assertEquals(value, value.formatDecimal(3).toLocaleDoubleOrNull()!!, 0.0001)
        }
    }
}
