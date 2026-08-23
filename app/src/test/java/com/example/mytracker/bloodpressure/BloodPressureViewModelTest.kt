package com.example.mytracker.bloodpressure

import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.util.DateUtils
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeBloodPressureDao : BloodPressureDao {
    val entries = MutableStateFlow<List<BloodPressureEntry>>(emptyList())

    override fun observeAll(): Flow<List<BloodPressureEntry>> = entries.map { list -> list.sortedBy { it.epochDay } }
    override suspend fun getAllOnce(): List<BloodPressureEntry> = entries.value.sortedBy { it.epochDay }
    override suspend fun getForDayAndTime(epochDay: Long, timeOfDay: BloodPressureTimeOfDay) =
        entries.value.firstOrNull { it.epochDay == epochDay && it.timeOfDay == timeOfDay }
    override suspend fun upsert(entry: BloodPressureEntry) {
        entries.value = entries.value.filterNot { it.id == entry.id } + entry
    }
    override suspend fun delete(entry: BloodPressureEntry) {
        entries.value = entries.value.filterNot { it.id == entry.id }
    }
    override suspend fun deleteAll() {
        entries.value = emptyList()
    }
}

private fun entry(
    epochDay: Long,
    timeOfDay: BloodPressureTimeOfDay,
    systolic: Double,
    diastolic: Double,
    comment: String? = null,
    pulse: Double? = null,
    systolic2: Double? = null,
    diastolic2: Double? = null,
    pulse2: Double? = null,
) = BloodPressureEntry(
    id = "bloodpressure-$epochDay-$timeOfDay",
    epochDay = epochDay,
    timeOfDay = timeOfDay,
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    systolic2 = systolic2,
    diastolic2 = diastolic2,
    pulse2 = pulse2,
    comment = comment,
    createdAt = Instant.EPOCH,
)

/**
 * The Blutdruck screen's own rules on top of the Maße pattern: the prefill follows the selected time
 * of day, a reading is only saved whole, morning/evening stay separate chart series, and a slot
 * measured twice is charted and listed as the mean of its two measurements.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BloodPressureViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val dao = FakeBloodPressureDao()
    private val repository = BloodPressureRepository(dao)
    private val today = DateUtils.todayEpochDay()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultTimeOfDay_followsTheHalfOfTheDay() {
        assertEquals(BloodPressureTimeOfDay.MORNING, defaultTimeOfDay(0))
        assertEquals(BloodPressureTimeOfDay.MORNING, defaultTimeOfDay(11))
        assertEquals(BloodPressureTimeOfDay.EVENING, defaultTimeOfDay(12))
        assertEquals(BloodPressureTimeOfDay.EVENING, defaultTimeOfDay(23))
    }

    @Test
    fun prefill_usesTheLastReadingOfTheSelectedTimeOfDay() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(today - 2, BloodPressureTimeOfDay.MORNING, 128.0, 84.0),
            entry(today - 1, BloodPressureTimeOfDay.EVENING, 134.0, 88.0),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.MORNING)
        advanceUntilIdle()

        assertEquals("128", viewModel.uiState.value.first.systolic)
        assertEquals("84", viewModel.uiState.value.first.diastolic)

        // Switching halves of the day re-prefills rather than keeping the morning numbers.
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.EVENING)
        advanceUntilIdle()
        assertEquals("134", viewModel.uiState.value.first.systolic)
        assertEquals("88", viewModel.uiState.value.first.diastolic)
    }

    @Test
    fun prefill_neverOffersAReadingFromAfterTheSelectedDay() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(today - 10, BloodPressureTimeOfDay.MORNING, 140.0, 92.0),
            entry(today - 1, BloodPressureTimeOfDay.MORNING, 120.0, 78.0),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.MORNING)
        viewModel.onDateChange(today - 5)
        advanceUntilIdle()

        // Logging a forgotten day offers the last reading *before* it, not the newer one.
        assertEquals("140", viewModel.uiState.value.first.systolic)
        assertFalse(viewModel.uiState.value.isEditingExisting)
    }

    @Test
    fun prefill_showsTheStoredReadingWhenTheSelectedSlotIsAlreadyTaken() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(today - 5, BloodPressureTimeOfDay.MORNING, 140.0, 92.0, comment = "Kaffee vorher"),
            entry(today - 1, BloodPressureTimeOfDay.MORNING, 120.0, 78.0),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.MORNING)
        viewModel.onDateChange(today - 5)
        advanceUntilIdle()

        // Opening a filled slot shows what is stored there — comment included, so saving can't
        // silently drop it — and flags that saving overwrites.
        val state = viewModel.uiState.value
        assertEquals("140", state.first.systolic)
        assertEquals("92", state.first.diastolic)
        assertEquals("Kaffee vorher", state.commentDraft)
        assertTrue(state.isEditingExisting)
    }

    @Test
    fun save_writesToThePickedDayAndThenReturnsToToday() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onDateChange(today - 3)
        advanceUntilIdle()

        viewModel.onSystolicChange("122")
        viewModel.onDiastolicChange("79")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(today - 3, dao.entries.value.single().epochDay)
        // The form goes back to today: a date left on last week would quietly file the next reading there.
        assertEquals(today, viewModel.uiState.value.epochDay)
    }

    @Test
    fun save_correctsTheReadingOfThePickedDayInsteadOfAddingOne() = runTest(dispatcher) {
        dao.entries.value = listOf(entry(today - 3, BloodPressureTimeOfDay.MORNING, 140.0, 92.0))
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.MORNING)
        viewModel.onDateChange(today - 3)
        advanceUntilIdle()

        viewModel.onSystolicChange("138")
        viewModel.save()
        advanceUntilIdle()

        val saved = dao.entries.value.single()
        assertEquals(today - 3, saved.epochDay)
        assertEquals(138.0, saved.systolic, 0.0001)
        // The untouched field kept the stored value rather than falling back to empty.
        assertEquals(92.0, saved.diastolic, 0.0001)
    }

    @Test
    fun save_writesBothValuesTheCommentAndTheSelectedTimeOfDay() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.EVENING)
        advanceUntilIdle()

        viewModel.onSystolicChange("126")
        viewModel.onDiastolicChange("81,5")
        viewModel.onCommentChange("  nach dem Sport  ")
        viewModel.save()
        advanceUntilIdle()

        val saved = dao.entries.value.single()
        assertEquals(today, saved.epochDay)
        assertEquals(BloodPressureTimeOfDay.EVENING, saved.timeOfDay)
        assertEquals(126.0, saved.systolic, 0.0001)
        assertEquals(81.5, saved.diastolic, 0.0001)
        assertEquals("nach dem Sport", saved.comment)
        assertFalse(viewModel.uiState.value.isAddExpanded)
    }

    @Test
    fun save_keepsABlankCommentOutOfTheEntry() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onCommentChange("   ")
        viewModel.save()
        advanceUntilIdle()

        assertNull(dao.entries.value.single().comment)
    }

    @Test
    fun save_needsBothValues() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("120")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.save()
        advanceUntilIdle()
        assertTrue(dao.entries.value.isEmpty())

        viewModel.onDiastolicChange("80")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun save_correctsTheSameHalfOfTheDayInPlace() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.MORNING)
        advanceUntilIdle()

        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.save()
        advanceUntilIdle()
        viewModel.onSystolicChange("118")
        viewModel.onDiastolicChange("79")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, dao.entries.value.size)
        assertEquals(118.0, dao.entries.value.single().systolic, 0.0001)

        // The evening reading of the same day is a second entry, not an overwrite.
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.EVENING)
        advanceUntilIdle()
        viewModel.onSystolicChange("131")
        viewModel.onDiastolicChange("86")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(2, dao.entries.value.size)
    }

    @Test
    fun series_keepMorningAndEveningApartAndFollowTheChips() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(today - 40, BloodPressureTimeOfDay.MORNING, 140.0, 92.0),
            entry(today - 3, BloodPressureTimeOfDay.MORNING, 128.0, 84.0),
            entry(today - 3, BloodPressureTimeOfDay.EVENING, 134.0, 88.0),
            entry(today - 1, BloodPressureTimeOfDay.MORNING, 126.0, 82.0),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onChartRangeChange(ChartRange.ALL)
        advanceUntilIdle()

        // Four series, and the two readings of the same day land on different lines rather than on
        // the same x of one line.
        assertEquals(4, viewModel.uiState.value.chartableSeries.size)
        val sysMorning = viewModel.uiState.value.series
            .single { it.measure == BloodPressureMeasure.SYSTOLIC && it.timeOfDay == BloodPressureTimeOfDay.MORNING }
        assertEquals(listOf(140.0, 128.0, 126.0), sysMorning.points.map { it.value })
        val sysEvening = viewModel.uiState.value.series
            .single { it.measure == BloodPressureMeasure.SYSTOLIC && it.timeOfDay == BloodPressureTimeOfDay.EVENING }
        assertEquals(listOf(134.0), sysEvening.points.map { it.value })

        // MONTH ends at the last reading, so the 40-day-old morning point drops out.
        viewModel.onChartRangeChange(ChartRange.MONTH)
        advanceUntilIdle()
        assertEquals(
            listOf(128.0, 126.0),
            viewModel.uiState.value.series
                .single { it.measure == BloodPressureMeasure.SYSTOLIC && it.timeOfDay == BloodPressureTimeOfDay.MORNING }
                .points.map { it.value },
        )

        // Hiding a line drops it from the chart but keeps its chip.
        viewModel.toggleSeriesVisibility(sysEvening.key)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.series.any { it.key == sysEvening.key })
        assertTrue(viewModel.uiState.value.chartableSeries.any { it.key == sysEvening.key })
    }

    @Test
    fun history_showsTheNewestReadingFirstWithItsComment() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(today - 1, BloodPressureTimeOfDay.MORNING, 128.0, 84.0, comment = "schlecht geschlafen"),
            entry(today, BloodPressureTimeOfDay.MORNING, 126.0, 82.0),
            entry(today, BloodPressureTimeOfDay.EVENING, 134.0, 88.0),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val history = viewModel.uiState.value.history
        assertEquals(
            listOf(BloodPressureTimeOfDay.EVENING, BloodPressureTimeOfDay.MORNING, BloodPressureTimeOfDay.MORNING),
            history.map { it.entry.timeOfDay },
        )
        assertEquals("134/88 mmHg", history.first().values)
        assertEquals("schlecht geschlafen", history.last().entry.comment)
    }

    @Test
    fun save_keepsThePulseWithTheReadingAndLeavesItOptional() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("124")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("68")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canSave)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(68.0, dao.entries.value.single().pulse)
    }

    @Test
    fun save_withoutAPulseStoresNoneRatherThanZero() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("124")
        viewModel.onDiastolicChange("80")
        advanceUntilIdle()
        // The pulse never gates the save — not every meter shows one.
        assertTrue(viewModel.uiState.value.canSave)
        viewModel.save()
        advanceUntilIdle()

        val saved = dao.entries.value.single()
        assertNull(saved.pulse)
        assertNull(saved.averagePulse)
    }

    @Test
    fun save_aPulseThatDoesNotParseBlocksTheSave() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("124")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("sechzig")
        advanceUntilIdle()

        // Optional means "may be left out", not "may be nonsense": silently dropping it would file a
        // reading whose pulse the user believes they entered.
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.save()
        advanceUntilIdle()
        assertTrue(dao.entries.value.isEmpty())
    }

    @Test
    fun save_storesBothMeasurementsAndTheSlotReadsAsTheirMean() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("130")
        viewModel.onDiastolicChange("86")
        viewModel.onPulseChange("74")
        viewModel.toggleSecondMeasurement()
        viewModel.onSecondSystolicChange("121")
        viewModel.onSecondDiastolicChange("81")
        viewModel.onSecondPulseChange("70")
        advanceUntilIdle()

        // The mean is announced before it is filed, so a stored 125,5 can't read as a typo.
        assertEquals("125,5/83,5 mmHg · Puls 72/min", viewModel.uiState.value.averagePreview)

        viewModel.save()
        advanceUntilIdle()

        val saved = dao.entries.value.single()
        // Stored raw, both of them — the mean is derived, so a correction can't leave them disagreeing.
        assertEquals(130.0, saved.systolic, 0.0001)
        assertEquals(121.0, saved.systolic2!!, 0.0001)
        assertEquals(125.5, saved.averageSystolic, 0.0001)
        assertEquals(83.5, saved.averageDiastolic, 0.0001)
        assertEquals(72.0, saved.averagePulse!!, 0.0001)
    }

    @Test
    fun save_aHalfTypedSecondMeasurementBlocksTheSave() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("130")
        viewModel.onDiastolicChange("86")
        viewModel.toggleSecondMeasurement()
        viewModel.onSecondSystolicChange("121")
        advanceUntilIdle()

        // Dropping the half-typed second reading would file a single measurement the user believes
        // was averaged; there is no mean without both cuff values.
        assertFalse(viewModel.uiState.value.canSave)
        assertNull(viewModel.uiState.value.averagePreview)
        viewModel.save()
        advanceUntilIdle()
        assertTrue(dao.entries.value.isEmpty())
    }

    @Test
    fun save_anEmptySecondMeasurementStaysOffTheSlot() = runTest(dispatcher) {
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSystolicChange("124")
        viewModel.onDiastolicChange("80")
        viewModel.toggleSecondMeasurement()
        advanceUntilIdle()

        // Opening the fields is not entering a measurement.
        assertTrue(viewModel.uiState.value.canSave)
        viewModel.save()
        advanceUntilIdle()

        val saved = dao.entries.value.single()
        assertFalse(saved.hasSecondMeasurement)
        assertEquals(124.0, saved.averageSystolic, 0.0001)
    }

    @Test
    fun secondMeasurement_prefillsItselfAndCanBeTakenBackOff() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(
                today - 1,
                BloodPressureTimeOfDay.MORNING,
                130.0,
                86.0,
                pulse = 74.0,
                systolic2 = 120.0,
                diastolic2 = 80.0,
                pulse2 = 70.0,
            ),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.MORNING)
        viewModel.onDateChange(today - 1)
        advanceUntilIdle()

        // A stored second measurement shows itself: the fold is for adding one, not for hiding one.
        val state = viewModel.uiState.value
        assertTrue(state.isSecondShown)
        assertEquals("120", state.second.systolic)
        assertEquals("70", state.second.pulse)

        // Removing it has to actually clear the fields rather than falling back to the stored values.
        viewModel.toggleSecondMeasurement()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSecondShown)
        assertTrue(viewModel.uiState.value.second.isBlank)

        viewModel.save()
        advanceUntilIdle()
        val saved = dao.entries.value.single { it.epochDay == today - 1 }
        assertFalse(saved.hasSecondMeasurement)
        assertEquals(130.0, saved.averageSystolic, 0.0001)
        // The first measurement's pulse survives the removal of the second one.
        assertEquals(74.0, saved.averagePulse!!, 0.0001)
    }

    @Test
    fun series_chartTheMeanAndGiveThePulseALineOfItsOwn() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(
                today - 2,
                BloodPressureTimeOfDay.MORNING,
                130.0,
                86.0,
                pulse = 74.0,
                systolic2 = 120.0,
                diastolic2 = 80.0,
                pulse2 = 70.0,
            ),
            // No pulse on this one: the pulse line skips it instead of drawing a hole at zero.
            entry(today - 1, BloodPressureTimeOfDay.MORNING, 126.0, 82.0),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onChartRangeChange(ChartRange.ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val systolic = state.series.single {
            it.measure == BloodPressureMeasure.SYSTOLIC && it.timeOfDay == BloodPressureTimeOfDay.MORNING
        }
        assertEquals(listOf(125.0, 126.0), systolic.points.map { it.value })

        val pulse = state.series.single {
            it.measure == BloodPressureMeasure.PULSE && it.timeOfDay == BloodPressureTimeOfDay.MORNING
        }
        assertEquals(listOf(72.0), pulse.points.map { it.value })
        assertEquals("Puls morgens", pulse.label)
        assertEquals(PULSE_UNIT, pulse.measure.unit())

        // The evening pulse has no data at all, so it is not offered as a chip either.
        assertFalse(
            state.chartableSeries.any {
                it.measure == BloodPressureMeasure.PULSE && it.timeOfDay == BloodPressureTimeOfDay.EVENING
            },
        )
    }

    @Test
    fun history_showsTheMeanThePulseAndWhatItWasAveragedFrom() = runTest(dispatcher) {
        dao.entries.value = listOf(
            entry(
                today,
                BloodPressureTimeOfDay.MORNING,
                130.0,
                86.0,
                pulse = 74.0,
                systolic2 = 121.0,
                diastolic2 = 81.0,
                pulse2 = 70.0,
            ),
        )
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val row = viewModel.uiState.value.history.single()
        assertEquals("125,5/83,5 mmHg", row.values)
        assertEquals("72/min", row.pulse)
        // Without this line a 125,5 would look like a reading no cuff ever showed.
        assertEquals("Ø aus 130/86 und 121/81", row.averagedFrom)
    }

    @Test
    fun history_saysNothingAboutAveragingForASingleMeasurement() = runTest(dispatcher) {
        dao.entries.value = listOf(entry(today, BloodPressureTimeOfDay.MORNING, 134.0, 88.0))
        val viewModel = BloodPressureViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val row = viewModel.uiState.value.history.single()
        assertEquals("134/88 mmHg", row.values)
        assertNull(row.pulse)
        assertNull(row.averagedFrom)
    }
}
