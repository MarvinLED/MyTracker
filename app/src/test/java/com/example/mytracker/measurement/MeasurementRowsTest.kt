package com.example.mytracker.measurement

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that decides what a field opens on. It is the difference between confirming a session and
 * correcting one, and getting it wrong writes a number onto a day it was never measured on.
 */
class MeasurementRowsTest {
    private fun day(date: String): Long = LocalDate.parse(date).toEpochDay()

    private val sites = listOf(
        BodySite("s1", "Oberarm links", null, sortOrder = 0, createdAt = Instant.EPOCH),
        BodySite("s2", "Taille", null, sortOrder = 1, createdAt = Instant.EPOCH),
    )

    private fun measurement(siteId: String, date: String, valueCm: Double) = BodyMeasurement(
        id = "measurement-$siteId-${day(date)}",
        bodySiteId = siteId,
        epochDay = day(date),
        valueCm = valueCm,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun aFreshDayOpensOnEachSitesLastValue() {
        val rows = measurementRows(
            sites = sites,
            measurements = listOf(
                measurement("s1", "2026-06-01", 34.0),
                measurement("s1", "2026-07-01", 35.5),
                measurement("s2", "2026-07-01", 82.0),
            ),
            editingEpochDay = day("2026-08-01"),
            drafts = emptyMap(),
        )

        assertEquals(listOf("35,5", "82"), rows.map { it.draft })
        // Nothing stored on the day being written to, so nothing can be cleared away either.
        assertEquals(listOf(null, null), rows.map { it.savedValueCm })
        assertEquals(listOf(day("2026-07-01"), day("2026-07-01")), rows.map { it.referenceEpochDay })
    }

    @Test
    fun aDayThatWasMeasuredOpensOnWhatThatDayHolds() {
        val rows = measurementRows(
            sites = sites,
            measurements = listOf(
                measurement("s1", "2026-07-01", 35.5),
                measurement("s2", "2026-07-01", 82.0),
                measurement("s1", "2026-08-01", 36.0),
                measurement("s2", "2026-08-01", 81.0),
            ),
            editingEpochDay = day("2026-07-01"),
            drafts = emptyMap(),
        )

        // The July values, not the newer August ones — correcting July must show July.
        assertEquals(listOf("35,5", "82"), rows.map { it.draft })
        assertEquals(listOf(35.5, 82.0), rows.map { it.savedValueCm })
    }

    @Test
    fun aSiteMissingFromAMeasuredDayOpensEmptyRatherThanBorrowingAnotherDaysValue() {
        val rows = measurementRows(
            sites = sites,
            measurements = listOf(
                measurement("s1", "2026-07-01", 35.5),
                measurement("s2", "2026-06-01", 82.0),
            ),
            editingEpochDay = day("2026-07-01"),
            drafts = emptyMap(),
        )

        // The waist was not measured on the 1st of July. Prefilling it from June and saving would
        // invent a July measurement that never happened.
        assertEquals(listOf("35,5", ""), rows.map { it.draft })
    }

    @Test
    fun whatWasTypedWinsOverEitherPrefill() {
        val rows = measurementRows(
            sites = sites,
            measurements = listOf(measurement("s1", "2026-07-01", 35.5)),
            editingEpochDay = day("2026-07-01"),
            drafts = mapOf("s1" to ""),
        )

        // Including an empty draft: a field cleared on purpose must not refill itself.
        assertEquals("", rows.first().draft)
        assertTrue(rows.first().isCleared)
    }

    @Test
    fun clearingCountsAsAChangeOnlyWhenSomethingWasStored() {
        val stored = measurementRows(
            sites = sites,
            measurements = listOf(measurement("s1", "2026-07-01", 35.5)),
            editingEpochDay = day("2026-07-01"),
            drafts = mapOf("s1" to ""),
        )
        val never = measurementRows(
            sites = sites,
            measurements = emptyList(),
            editingEpochDay = day("2026-07-01"),
            drafts = mapOf("s1" to ""),
        )

        assertTrue(stored.first().isCleared)
        // Nothing was ever written here, so there is nothing to delete and nothing to save.
        assertEquals(false, never.first().isCleared)
    }

    @Test
    fun historyGroupsADayIntoOneRowInLibraryOrder() {
        val rows = measurementDayRows(
            measurements = listOf(
                measurement("s2", "2026-07-01", 82.0),
                measurement("s1", "2026-07-01", 35.5),
                measurement("s1", "2026-08-01", 36.0),
            ),
            sites = sites,
        )

        assertEquals(listOf(day("2026-08-01"), day("2026-07-01")), rows.map { it.epochDay })
        // Ordered as the library orders the sites, not as the rows came out of the database.
        assertEquals("Oberarm links 35,5 cm · Taille 82 cm", rows.last().summary)
    }

    @Test
    fun historyIsCappedAtTheNewestSessions() {
        val many = (1..MEASUREMENT_HISTORY_LIMIT + 5).map { index ->
            BodyMeasurement(
                id = "m$index",
                bodySiteId = "s1",
                epochDay = day("2026-01-01") + index,
                valueCm = 30.0 + index,
                createdAt = Instant.EPOCH,
            )
        }
        val rows = measurementDayRows(many, sites)

        assertEquals(MEASUREMENT_HISTORY_LIMIT, rows.size)
        assertEquals(day("2026-01-01") + MEASUREMENT_HISTORY_LIMIT + 5, rows.first().epochDay)
    }

    @Test
    fun afreshDayInAGapPrefillsFromBeforeItRatherThanFromAfterIt() {
        val rows = measurementRows(
            sites = sites,
            measurements = listOf(
                measurement("s1", "2026-06-01", 34.0),
                measurement("s1", "2026-08-01", 36.0),
            ),
            editingEpochDay = day("2026-07-01"),
            drafts = emptyMap(),
        )

        // Logging a session forgotten in July: the June value is what preceded it. Prefilling from
        // August would carry a number backwards in time and call it a measurement.
        assertEquals("34", rows.first().draft)
        assertEquals(day("2026-06-01"), rows.first().referenceEpochDay)
    }

    @Test
    fun theChangeIsAgainstTheLastMeasurementAndFollowsWhatIsTyped() {
        val rows = measurementRows(
            sites = sites,
            measurements = listOf(measurement("s1", "2026-07-01", 35.5)),
            editingEpochDay = day("2026-08-01"),
            drafts = mapOf("s1" to "37"),
        )

        assertEquals(1.5, rows.first().deltaCm!!, 0.0001)
        assertEquals("+1,5 cm", rows.first().deltaCm!!.signedCm())
        assertEquals("−1,5 cm", (-1.5).signedCm())
    }

    @Test
    fun historyStatesEachValuesChangeAgainstThatSitesPreviousSession() {
        val rows = measurementDayRows(
            measurements = listOf(
                measurement("s1", "2026-06-01", 34.0),
                measurement("s2", "2026-06-01", 84.0),
                measurement("s1", "2026-07-01", 35.5),
                measurement("s2", "2026-07-01", 82.0),
            ),
            sites = sites,
        )

        val july = rows.first()
        // Per site, not against the row above it in the same session.
        assertEquals(listOf("+1,5 cm", "−2 cm"), july.values.map { it.deltaText })
        // A first-ever measurement has nothing to be a change from.
        assertEquals(listOf(null, null), rows.last().values.map { it.deltaText })
    }

    @Test
    fun aRatioIsOnlyDrawnForDaysBothSitesWereMeasured() {
        val points = ratioPoints(
            measurements = listOf(
                measurement("s1", "2026-06-01", 80.0),
                measurement("s2", "2026-06-01", 100.0),
                // Only the waist on this day: carrying June's hip forward would draw a ratio that
                // was never true.
                measurement("s1", "2026-07-01", 78.0),
            ),
            numeratorSiteId = "s1",
            denominatorSiteId = "s2",
        )

        assertEquals(listOf(day("2026-06-01")), points.map { it.epochDay })
        // Percent, not a bare quotient: 0,80 and 0,85 both round to "0,8" on the chart's axis.
        assertEquals(80.0, points.single().value, 0.0001)
    }

    @Test
    fun aRatioSkipsADayWhoseDenominatorIsUnusable() {
        val points = ratioPoints(
            measurements = listOf(
                measurement("s1", "2026-06-01", 80.0),
                measurement("s2", "2026-06-01", 0.0),
            ),
            numeratorSiteId = "s1",
            denominatorSiteId = "s2",
        )

        assertTrue(points.isEmpty())
    }
}
