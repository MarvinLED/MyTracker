package com.example.mytracker.measurement

import com.example.mytracker.core.datastore.DEFAULT_WATER_GOAL_ML
import com.example.mytracker.core.datastore.UserPreferences
import com.example.mytracker.core.datastore.UserPreferencesSource
import com.example.mytracker.core.datastore.WeightUnit
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.weight.BodyWeightDao
import com.example.mytracker.weight.BodyWeightEntry
import com.example.mytracker.weight.BodyWeightRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeBodySiteDao : BodySiteDao {
    val sites = MutableStateFlow<List<BodySite>>(emptyList())

    override fun observeAll(): Flow<List<BodySite>> = sites.map { list -> list.sortedBy { it.sortOrder } }
    override suspend fun getAllOnce(): List<BodySite> = sites.value.sortedBy { it.sortOrder }
    override suspend fun getById(id: String): BodySite? = sites.value.firstOrNull { it.id == id }
    override suspend fun upsert(site: BodySite) {
        sites.value = sites.value.filterNot { it.id == site.id } + site
    }
    override suspend fun delete(site: BodySite) {
        sites.value = sites.value.filterNot { it.id == site.id }
    }
    override suspend fun deleteAll() {
        sites.value = emptyList()
    }
}

private class FakeBodyMeasurementDao : BodyMeasurementDao {
    val measurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())

    override fun observeAll(): Flow<List<BodyMeasurement>> = measurements.map { list -> list.sortedBy { it.epochDay } }
    override suspend fun countForSite(bodySiteId: String): Int = measurements.value.count { it.bodySiteId == bodySiteId }
    override suspend fun getAllOnce(): List<BodyMeasurement> = measurements.value.sortedBy { it.epochDay }
    override suspend fun getForSiteAndDay(bodySiteId: String, epochDay: Long) =
        measurements.value.firstOrNull { it.bodySiteId == bodySiteId && it.epochDay == epochDay }
    override suspend fun upsert(measurement: BodyMeasurement) {
        measurements.value = measurements.value.filterNot { it.id == measurement.id } + measurement
    }
    override suspend fun delete(measurement: BodyMeasurement) {
        measurements.value = measurements.value.filterNot { it.id == measurement.id }
    }
    override suspend fun getForDay(epochDay: Long) = measurements.value.filter { it.epochDay == epochDay }
    override suspend fun deleteForSiteAndDay(bodySiteId: String, epochDay: Long) {
        measurements.value = measurements.value
            .filterNot { it.bodySiteId == bodySiteId && it.epochDay == epochDay }
    }
    override suspend fun deleteForDay(epochDay: Long) {
        measurements.value = measurements.value.filterNot { it.epochDay == epochDay }
    }
    override suspend fun deleteAll() {
        measurements.value = emptyList()
    }
}

private class FakeBodyWeightDao : BodyWeightDao {
    val entries = MutableStateFlow<List<BodyWeightEntry>>(emptyList())

    override fun observeAll(): Flow<List<BodyWeightEntry>> = entries.map { list -> list.sortedBy { it.epochDay } }
    override fun observeForDay(epochDay: Long) = entries.map { list -> list.firstOrNull { it.epochDay == epochDay } }
    override fun observeRange(startInclusive: Long, endInclusive: Long) =
        entries.map { list -> list.filter { it.epochDay in startInclusive..endInclusive }.sortedBy { it.epochDay } }
    override fun observeFirstLoggedDay() = entries.map { list -> list.minOfOrNull { it.epochDay } }
    override fun observeLatest() = entries.map { list -> list.maxByOrNull { it.epochDay } }
    override suspend fun getAllOnce() = entries.value.sortedBy { it.epochDay }
    override suspend fun getForDayOnce(epochDay: Long) = entries.value.firstOrNull { it.epochDay == epochDay }
    override suspend fun upsert(entry: BodyWeightEntry) {
        entries.value = entries.value.filterNot { it.epochDay == entry.epochDay } + entry
    }
    override suspend fun delete(entry: BodyWeightEntry) {
        entries.value = entries.value.filterNot { it.id == entry.id }
    }
    override suspend fun deleteAll() {
        entries.value = emptyList()
    }
}

/** The read-only slice the Maße screen needs; the real one is bound to a DataStore. */
private class FakePreferences(unit: WeightUnit = WeightUnit.KG) : UserPreferencesSource {
    override val userPreferences = MutableStateFlow(
        UserPreferences(dailyWaterGoalMl = DEFAULT_WATER_GOAL_ML, weightUnit = unit),
    )
}

private fun site(id: String, name: String, sortOrder: Int) =
    BodySite(id = id, name = name, measuringHint = null, sortOrder = sortOrder, createdAt = Instant.EPOCH)

private fun measurement(siteId: String, epochDay: Long, valueCm: Double) = BodyMeasurement(
    id = "measurement-$siteId-$epochDay",
    bodySiteId = siteId,
    epochDay = epochDay,
    valueCm = valueCm,
    createdAt = Instant.EPOCH,
)

/**
 * The Maße screen's two load-bearing behaviours: every field opens on that site's last value so a
 * session can be confirmed rather than typed, and saving writes exactly the rows that hold a number.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val siteDao = FakeBodySiteDao()
    private val measurementDao = FakeBodyMeasurementDao()
    private val repository = MeasurementRepository(siteDao, measurementDao)
    private val weightDao = FakeBodyWeightDao()
    private val preferences = FakePreferences()
    private val today = DateUtils.todayEpochDay()

    private fun viewModel() = MeasurementViewModel(repository, BodyWeightRepository(weightDao), preferences)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        siteDao.sites.value = listOf(site("s1", "Oberarm links", 0), site("s2", "Taille", 1))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rows_prefillEachSiteWithItsMostRecentValue() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 30, 34.0),
            measurement("s1", today - 7, 35.5),
            measurement("s2", today - 7, 82.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("Oberarm links", "Taille"), rows.map { it.site.name })
        assertEquals(listOf("35,5", "82"), rows.map { it.draft })
        assertEquals(listOf(today - 7, today - 7), rows.map { it.referenceEpochDay })
    }

    @Test
    fun rows_startEmptyForASiteThatWasNeverMeasured() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("", ""), viewModel.uiState.value.rows.map { it.draft })
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun save_logsTodayForPrefilledAndTypedRowsAlike() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 7, 35.5),
            measurement("s2", today - 7, 82.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Only the waist actually changed; the arm is confirmed at its prefilled value.
        viewModel.onDraftChange("s2", "81,5")
        viewModel.save()
        advanceUntilIdle()

        val loggedToday = measurementDao.measurements.value.filter { it.epochDay == today }
        assertEquals(2, loggedToday.size)
        assertEquals(35.5, loggedToday.first { it.bodySiteId == "s1" }.valueCm, 0.0001)
        assertEquals(81.5, loggedToday.first { it.bodySiteId == "s2" }.valueCm, 0.0001)
        // The panel closes and the fields fall back to "last value", which is now today's.
        assertFalse(viewModel.uiState.value.isAddExpanded)
        assertEquals(listOf("35,5", "81,5"), viewModel.uiState.value.rows.map { it.draft })
    }

    @Test
    fun save_skipsRowsLeftEmptyInsteadOfInventingAPoint() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDraftChange("s1", "35")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canSave)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("s1"), measurementDao.measurements.value.map { it.bodySiteId })
    }

    @Test
    fun save_correctsTheSameDayInPlaceRatherThanAddingASecondPoint() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDraftChange("s1", "35")
        viewModel.save()
        advanceUntilIdle()
        viewModel.onDraftChange("s1", "34,5")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, measurementDao.measurements.value.size)
        assertEquals(34.5, measurementDao.measurements.value.single().valueCm, 0.0001)
    }

    @Test
    fun editDay_loadsThatDayAndSavesBackToIt() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 30, 34.0),
            measurement("s2", today - 30, 84.0),
            measurement("s1", today - 7, 35.5),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.editDay(today - 30)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditingExisting)
        assertTrue(viewModel.uiState.value.isAddExpanded)
        assertEquals(listOf("34", "84"), viewModel.uiState.value.rows.map { it.draft })

        viewModel.onDraftChange("s1", "34,5")
        viewModel.save()
        advanceUntilIdle()

        // Corrected on its own day — and emphatically not copied onto today.
        assertEquals(34.5, measurementDao.measurements.value.first { it.epochDay == today - 30 && it.bodySiteId == "s1" }.valueCm, 0.0001)
        assertTrue(measurementDao.measurements.value.none { it.epochDay == today })
        // And the editor is back on today, so the next entry is not written into the past.
        assertEquals(today, viewModel.uiState.value.editingEpochDay)
    }

    @Test
    fun clearingAStoredFieldDeletesThatValue() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 7, 35.5),
            measurement("s2", today - 7, 82.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.editDay(today - 7)
        advanceUntilIdle()
        viewModel.onDraftChange("s2", "")
        advanceUntilIdle()
        // An emptied field is a change worth saving; without this the button would be dead.
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("s1"), measurementDao.measurements.value.map { it.bodySiteId })
    }

    @Test
    fun deleteDay_removesTheWholeSession() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 7, 35.5),
            measurement("s2", today - 7, 82.0),
            measurement("s1", today - 30, 34.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.editDay(today - 7)
        advanceUntilIdle()
        viewModel.deleteDay(today - 7)
        advanceUntilIdle()

        assertEquals(listOf(today - 30), measurementDao.measurements.value.map { it.epochDay })
        // The editor was pointed at the day that just went away; leaving it there would have the
        // next save resurrect it.
        assertEquals(today, viewModel.uiState.value.editingEpochDay)
    }

    @Test
    fun history_isOneRowPerDayNewestFirst() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 7, 35.5),
            measurement("s2", today - 7, 82.0),
            measurement("s1", today - 30, 34.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val history = viewModel.uiState.value.history
        assertEquals(listOf(today - 7, today - 30), history.map { it.epochDay })
        assertEquals("Oberarm links 35,5 cm · Taille 82 cm", history.first().summary)
    }

    @Test
    fun weight_ridesAlongOnlyWhenSwitchedOnAndInTheUsersUnit() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(measurement("s1", today - 10, 35.0))
        weightDao.entries.value = listOf(
            BodyWeightEntry("w1", today - 10, 80.0, Instant.EPOCH),
            // Older than the MONTH window, which ends at the last measurement.
            BodyWeightEntry("w2", today - 200, 90.0, Instant.EPOCH),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.weightSeries.isEmpty())

        viewModel.toggleWeightShown()
        advanceUntilIdle()
        assertEquals(listOf(today - 10), viewModel.uiState.value.weightSeries.map { it.epochDay })
        assertEquals(80.0, viewModel.uiState.value.weightSeries.single().value, 0.0001)
        assertEquals("kg", viewModel.uiState.value.weightUnitLabel)

        preferences.userPreferences.value = preferences.userPreferences.value.copy(weightUnit = WeightUnit.LB)
        advanceUntilIdle()
        assertEquals("lb", viewModel.uiState.value.weightUnitLabel)
        assertEquals(176.37, viewModel.uiState.value.weightSeries.single().value, 0.01)
    }

    @Test
    fun ratio_appearsOnlyOnceBothEndsArePicked() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 10, 80.0),
            measurement("s2", today - 10, 100.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRatioNumeratorChange("s1")
        advanceUntilIdle()
        // Half a pair is not a ratio.
        assertEquals(null, viewModel.uiState.value.ratioLabel)
        assertTrue(viewModel.uiState.value.ratioSeries.isEmpty())

        viewModel.onRatioDenominatorChange("s2")
        advanceUntilIdle()
        assertEquals("Oberarm links / Taille", viewModel.uiState.value.ratioLabel)
        assertEquals(80.0, viewModel.uiState.value.ratioSeries.single().value, 0.0001)

        // And clearing an end takes the line away again.
        viewModel.onRatioNumeratorChange(null)
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.ratioLabel)
    }

    @Test
    fun series_windowOnTheLastMeasurementAndDropHiddenSites() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 90, 34.0),
            measurement("s1", today - 20, 35.0),
            measurement("s1", today - 10, 35.5),
            measurement("s2", today - 10, 82.0),
        )
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // MONTH ends at the last logged day, not today, so the 90-day-old point falls out and the
        // two recent ones stay.
        viewModel.onChartRangeChange(ChartRange.MONTH)
        advanceUntilIdle()
        assertEquals(listOf(today - 20, today - 10), viewModel.uiState.value.series.first().points.map { it.epochDay })

        viewModel.onChartRangeChange(ChartRange.ALL)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.series.first().points.size)

        // Hiding a site drops its line but keeps its chip, so it can be brought back.
        viewModel.toggleSiteVisibility("s2")
        advanceUntilIdle()
        assertEquals(listOf("s1"), viewModel.uiState.value.series.map { it.siteId })
        assertEquals(listOf("s1", "s2"), viewModel.uiState.value.chartableSites.map { it.siteId })
    }
}
