package com.example.mytracker.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How many decimals a measurement is shown with — a tape measure reads to the millimetre. */
private const val MEASUREMENT_DECIMALS = 1

/**
 * One row of the Hinzufügen panel: the site on the left, [draft] the text in the field on the right.
 * [draft] starts prefilled with [lastValueCm] so an unchanged measurement is one tap to confirm; it
 * is *not* stored until [MeasurementViewModel.save].
 */
data class MeasurementRow(
    val site: BodySite,
    val draft: String,
    val lastValueCm: Double?,
    val lastEpochDay: Long?,
) {
    val value: Double? get() = draft.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
}

/**
 * One site's line in the chart. [paletteIndex] is the site's position in the library, not its rank
 * in the chart, so a site keeps its colour when others are hidden — same rule as the fluid charts.
 */
data class MeasurementSeries(
    val siteId: String,
    val name: String,
    val paletteIndex: Int,
    val points: List<MetricPoint>,
)

data class MeasurementUiState(
    val rows: List<MeasurementRow> = emptyList(),
    val isAddExpanded: Boolean = false,
    val chartRange: ChartRange = ChartRange.MONTH,
    /** Sites toggled off in the chart. Kept as "hidden" so a newly created site shows up by default. */
    val hiddenSiteIds: Set<String> = emptySet(),
    /** Only the visible sites, already windowed to [chartRange]. */
    val series: List<MeasurementSeries> = emptyList(),
    /** Every site with at least one measurement — the chips, which must also show hidden ones. */
    val chartableSites: List<MeasurementSeries> = emptyList(),
) {
    val hasSites: Boolean get() = rows.isNotEmpty()

    /** Saving writes every row that holds a usable number, so one tap can confirm them all. */
    val canSave: Boolean get() = rows.any { it.value != null }
}

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {
    private val _isAddExpanded = MutableStateFlow(false)
    private val _chartRange = MutableStateFlow(ChartRange.MONTH)
    private val _hiddenSiteIds = MutableStateFlow(emptySet<String>())

    /** Only the fields the user actually typed in; everything else falls back to the last value. */
    private val _drafts = MutableStateFlow(emptyMap<String, String>())

    val uiState: StateFlow<MeasurementUiState> = combine(
        measurementRepository.observeSites(),
        measurementRepository.observeMeasurements(),
        _drafts,
        _hiddenSiteIds,
        combine(_isAddExpanded, _chartRange) { expanded, range -> expanded to range },
    ) { sites, measurements, drafts, hidden, (expanded, range) ->
        val bySite = measurements.groupBy { it.bodySiteId }

        // The window ends at the last measurement, not today: measuring is sporadic, and a "Woche"
        // anchored on today would show an empty chart two weeks after the last session.
        val lastDay = measurements.maxOfOrNull { it.epochDay }
        val cutoff = range.days?.let { days -> lastDay?.minus(days - 1) }

        val allSeries = sites.mapIndexed { index, site ->
            MeasurementSeries(
                siteId = site.id,
                name = site.name,
                paletteIndex = index,
                points = bySite[site.id].orEmpty()
                    .filter { cutoff == null || it.epochDay >= cutoff }
                    .sortedBy { it.epochDay }
                    .map { MetricPoint(it.epochDay, it.valueCm) },
            )
        }

        MeasurementUiState(
            rows = sites.map { site ->
                val last = bySite[site.id]?.maxByOrNull { it.epochDay }
                MeasurementRow(
                    site = site,
                    draft = drafts[site.id] ?: last?.valueCm?.formatDecimal(MEASUREMENT_DECIMALS).orEmpty(),
                    lastValueCm = last?.valueCm,
                    lastEpochDay = last?.epochDay,
                )
            },
            isAddExpanded = expanded,
            chartRange = range,
            hiddenSiteIds = hidden,
            series = allSeries.filter { it.siteId !in hidden && it.points.isNotEmpty() },
            chartableSites = allSeries.filter { !bySite[it.siteId].isNullOrEmpty() },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeasurementUiState())

    fun toggleAddExpanded() {
        _isAddExpanded.value = !_isAddExpanded.value
    }

    fun onDraftChange(siteId: String, value: String) {
        _drafts.value = _drafts.value + (siteId to value)
    }

    fun onChartRangeChange(range: ChartRange) {
        _chartRange.value = range
    }

    fun toggleSiteVisibility(siteId: String) {
        val hidden = _hiddenSiteIds.value
        _hiddenSiteIds.value = if (siteId in hidden) hidden - siteId else hidden + siteId
    }

    /**
     * Logs today's value for every row that holds a usable number — including the ones left at their
     * prefilled value, which is the point of prefilling them. Rows left empty are skipped, so a site
     * that wasn't measured today doesn't get a made-up point.
     */
    fun save() {
        // The drafts are read directly, not through [uiState]: that flow is derived and only
        // recomputes once its collector is resumed, so a value typed and saved in the same frame
        // would otherwise be saved at its *previous* value. The rows only supply the prefill
        // fallback for fields the user never touched.
        val drafts = _drafts.value
        val rows = uiState.value.rows
        val today = DateUtils.todayEpochDay()
        viewModelScope.launch {
            rows.forEach { row ->
                val text = drafts[row.site.id] ?: row.draft
                val value = text.toLocaleDoubleOrNull()?.takeIf { it > 0.0 } ?: return@forEach
                measurementRepository.logMeasurement(row.site.id, today, value)
            }
            // Back to "prefilled from the last entry" — which is now what was just saved.
            _drafts.value = emptyMap()
            _isAddExpanded.value = false
        }
    }
}
