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
) = BloodPressureEntry(
    id = "bloodpressure-$epochDay-$timeOfDay",
    epochDay = epochDay,
    timeOfDay = timeOfDay,
    systolic = systolic,
    diastolic = diastolic,
    comment = comment,
    createdAt = Instant.EPOCH,
)

/**
 * The Blutdruck screen's own rules on top of the Maße pattern: the prefill follows the selected time
 * of day, a reading is only saved whole, and morning/evening stay separate chart series.
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

        assertEquals("128", viewModel.uiState.value.systolicDraft)
        assertEquals("84", viewModel.uiState.value.diastolicDraft)

        // Switching halves of the day re-prefills rather than keeping the morning numbers.
        viewModel.onTimeOfDayChange(BloodPressureTimeOfDay.EVENING)
        advanceUntilIdle()
        assertEquals("134", viewModel.uiState.value.systolicDraft)
        assertEquals("88", viewModel.uiState.value.diastolicDraft)
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
        assertEquals("140", viewModel.uiState.value.systolicDraft)
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
        assertEquals("140", state.systolicDraft)
        assertEquals("92", state.diastolicDraft)
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
}
