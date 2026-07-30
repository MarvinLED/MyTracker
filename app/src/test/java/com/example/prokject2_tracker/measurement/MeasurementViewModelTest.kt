package com.example.prokject2_tracker.measurement

import com.example.prokject2_tracker.core.metrics.ChartRange
import com.example.prokject2_tracker.core.util.DateUtils
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
}

private class FakeBodyMeasurementDao : BodyMeasurementDao {
    val measurements = MutableStateFlow<List<BodyMeasurement>>(emptyList())

    override fun observeAll(): Flow<List<BodyMeasurement>> = measurements.map { list -> list.sortedBy { it.epochDay } }
    override suspend fun countForSite(bodySiteId: String): Int = measurements.value.count { it.bodySiteId == bodySiteId }
    override suspend fun upsert(measurement: BodyMeasurement) {
        measurements.value = measurements.value.filterNot { it.id == measurement.id } + measurement
    }
    override suspend fun delete(measurement: BodyMeasurement) {
        measurements.value = measurements.value.filterNot { it.id == measurement.id }
    }
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
    private val today = DateUtils.todayEpochDay()

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
        val viewModel = MeasurementViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("Oberarm links", "Taille"), rows.map { it.site.name })
        assertEquals(listOf("35,5", "82"), rows.map { it.draft })
        assertEquals(listOf(today - 7, today - 7), rows.map { it.lastEpochDay })
    }

    @Test
    fun rows_startEmptyForASiteThatWasNeverMeasured() = runTest(dispatcher) {
        val viewModel = MeasurementViewModel(repository)
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
        val viewModel = MeasurementViewModel(repository)
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
        val viewModel = MeasurementViewModel(repository)
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
        val viewModel = MeasurementViewModel(repository)
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
    fun series_windowOnTheLastMeasurementAndDropHiddenSites() = runTest(dispatcher) {
        measurementDao.measurements.value = listOf(
            measurement("s1", today - 90, 34.0),
            measurement("s1", today - 20, 35.0),
            measurement("s1", today - 10, 35.5),
            measurement("s2", today - 10, 82.0),
        )
        val viewModel = MeasurementViewModel(repository)
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
