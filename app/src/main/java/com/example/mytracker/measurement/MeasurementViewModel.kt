package com.example.mytracker.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.datastore.UserPreferencesSource
import com.example.mytracker.core.datastore.WeightUnit
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.label
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.core.util.toWeightUnit
import com.example.mytracker.weight.BodyWeightEntry
import com.example.mytracker.weight.BodyWeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One row of the editor: the site on the left, [draft] the text in the field on the right. [draft]
 * starts prefilled — from the day being corrected, or from the site's last value for a fresh day
 * (see [measurementRows]) — so an unchanged measurement is one tap to confirm; it is *not* stored
 * until [MeasurementViewModel.save].
 */
data class MeasurementRow(
    val site: BodySite,
    val draft: String,
    /** The site's last value *before* the day being edited — what a change is measured from. */
    val referenceValueCm: Double?,
    val referenceEpochDay: Long?,
    /** What the day being edited already holds for this site, null when it holds nothing. */
    val savedValueCm: Double? = null,
) {
    val value: Double? get() = draft.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }

    /** Clearing a field that had a stored value is how a single measurement gets deleted. */
    val isCleared: Boolean get() = value == null && savedValueCm != null

    /**
     * How far what is currently typed sits from the last measurement — live, while typing, which is
     * the moment the number is worth knowing. Null before there is anything to compare.
     */
    val deltaCm: Double? get() = value?.let { typed -> referenceValueCm?.let { typed - it } }
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
    /** The day the editor writes to. Today unless a past session was opened for correction. */
    val editingEpochDay: Long = DateUtils.todayEpochDay(),
    /** True while a day that already has measurements is open — the editor then corrects instead of adds. */
    val isEditingExisting: Boolean = false,
    val chartRange: ChartRange = ChartRange.MONTH,
    /** Sites toggled off in the chart. Kept as "hidden" so a newly created site shows up by default. */
    val hiddenSiteIds: Set<String> = emptySet(),
    /** Only the visible sites, already windowed to [chartRange]. */
    val series: List<MeasurementSeries> = emptyList(),
    /** Every site with at least one measurement — the chips, which must also show hidden ones. */
    val chartableSites: List<MeasurementSeries> = emptyList(),
    /** The logged sessions, newest first — tap one to correct it. */
    val history: List<MeasurementDayRow> = emptyList(),
    /** Body weight ridden along in the chart on its own axis, off by default. */
    val isWeightShown: Boolean = false,
    val weightSeries: List<MetricPoint> = emptyList(),
    val weightUnitLabel: String = "kg",
    /** The two sites read against each other, if any — see [ratioPoints]. */
    val ratioNumeratorSiteId: String? = null,
    val ratioDenominatorSiteId: String? = null,
    val ratioSeries: List<MetricPoint> = emptyList(),
    /** "Taille / Hüfte", for the line's legend. Null while the pair is incomplete. */
    val ratioLabel: String? = null,
) {
    val hasSites: Boolean get() = rows.isNotEmpty()

    /**
     * Saving writes every row that holds a usable number, so one tap can confirm them all — and
     * clearing a stored value is a change worth saving too, since that is how one is deleted.
     */
    val canSave: Boolean get() = rows.any { it.value != null || it.isCleared }
}

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
    bodyWeightRepository: BodyWeightRepository,
    userPreferencesSource: UserPreferencesSource,
) : ViewModel() {
    private val _isAddExpanded = MutableStateFlow(false)
    private val _chartRange = MutableStateFlow(ChartRange.MONTH)
    private val _hiddenSiteIds = MutableStateFlow(emptySet<String>())
    private val _editingEpochDay = MutableStateFlow(DateUtils.todayEpochDay())
    private val _isWeightShown = MutableStateFlow(false)
    private val _ratio = MutableStateFlow(RatioChoice())

    /** Which two sites the Verhältnis line divides, null until each end is picked. */
    private data class RatioChoice(val numeratorSiteId: String? = null, val denominatorSiteId: String? = null)

    /**
     * The extra series the chart can carry beyond the sites themselves. Bundled and kept apart from
     * the measurements so the weight and the unit preference arrive as one value — [combine] takes a
     * fixed number of sources, and the editor already uses four of them.
     */
    private val extraSeries = combine(
        bodyWeightRepository.observeAll(),
        userPreferencesSource.userPreferences,
        _isWeightShown,
        _ratio,
    ) { weights, prefs, weightShown, ratio ->
        ExtraSeriesState(weights, prefs.weightUnit, weightShown, ratio)
    }

    private data class ExtraSeriesState(
        val weights: List<BodyWeightEntry>,
        val weightUnit: WeightUnit,
        val isWeightShown: Boolean,
        val ratio: RatioChoice,
    )

    /** Only the fields the user actually typed in; everything else falls back to the prefill. */
    private val _drafts = MutableStateFlow(emptyMap<String, String>())

    val uiState: StateFlow<MeasurementUiState> = combine(
        measurementRepository.observeSites(),
        measurementRepository.observeMeasurements(),
        combine(_drafts, extraSeries) { drafts, extras -> drafts to extras },
        _hiddenSiteIds,
        combine(_isAddExpanded, _chartRange, _editingEpochDay) { expanded, range, editingDay ->
            EditorState(expanded, range, editingDay)
        },
    ) { sites, measurements, (drafts, extras), hidden, editor ->
        val bySite = measurements.groupBy { it.bodySiteId }

        // The window ends at the last measurement, not today: measuring is sporadic, and a "Woche"
        // anchored on today would show an empty chart two weeks after the last session.
        val lastDay = measurements.maxOfOrNull { it.epochDay }
        val cutoff = editor.chartRange.days?.let { days -> lastDay?.minus(days - 1) }

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

        val siteNames = sites.associate { it.id to it.name }
        val ratio = extras.ratio
        val ratioLabel = ratio.numeratorSiteId?.let { numerator ->
            ratio.denominatorSiteId?.let { denominator ->
                siteNames[numerator]?.let { first -> siteNames[denominator]?.let { "$first / $it" } }
            }
        }

        MeasurementUiState(
            rows = measurementRows(
                sites = sites,
                measurements = measurements,
                editingEpochDay = editor.editingEpochDay,
                drafts = drafts,
            ),
            isAddExpanded = editor.isAddExpanded,
            editingEpochDay = editor.editingEpochDay,
            isEditingExisting = measurements.any { it.epochDay == editor.editingEpochDay },
            chartRange = editor.chartRange,
            hiddenSiteIds = hidden,
            series = allSeries.filter { it.siteId !in hidden && it.points.isNotEmpty() },
            chartableSites = allSeries.filter { !bySite[it.siteId].isNullOrEmpty() },
            history = measurementDayRows(measurements, sites),
            isWeightShown = extras.isWeightShown,
            // Windowed the same way the sites are: an axis that ran further than the cm lines would
            // stretch the chart around data the rest of it does not cover.
            weightSeries = if (extras.isWeightShown) {
                extras.weights
                    .filter { cutoff == null || it.epochDay >= cutoff }
                    .sortedBy { it.epochDay }
                    .map { MetricPoint(it.epochDay, it.weightKg.toWeightUnit(extras.weightUnit)) }
            } else {
                emptyList()
            },
            weightUnitLabel = extras.weightUnit.label(),
            ratioNumeratorSiteId = ratio.numeratorSiteId,
            ratioDenominatorSiteId = ratio.denominatorSiteId,
            ratioSeries = ratioLabel?.let {
                ratioPoints(
                    measurements = measurements.filter { m -> cutoff == null || m.epochDay >= cutoff },
                    numeratorSiteId = ratio.numeratorSiteId!!,
                    denominatorSiteId = ratio.denominatorSiteId!!,
                )
            }.orEmpty(),
            ratioLabel = ratioLabel,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeasurementUiState())

    /** The editor's own state, bundled because [combine] takes a fixed number of sources. */
    private data class EditorState(
        val isAddExpanded: Boolean,
        val chartRange: ChartRange,
        val editingEpochDay: Long,
    )

    fun toggleAddExpanded() {
        // Folding the editor away ends whatever correction was open: reopening it later on a day
        // chosen minutes ago, with no indication of which, is how a value lands on the wrong date.
        if (_isAddExpanded.value) resetEditor()
        _isAddExpanded.value = !_isAddExpanded.value
    }

    fun onDraftChange(siteId: String, value: String) {
        _drafts.value = _drafts.value + (siteId to value)
    }

    fun onChartRangeChange(range: ChartRange) {
        _chartRange.value = range
    }

    fun toggleWeightShown() {
        _isWeightShown.value = !_isWeightShown.value
    }

    /** Either end of the Verhältnis; null clears it and the line disappears. */
    fun onRatioNumeratorChange(siteId: String?) {
        _ratio.value = _ratio.value.copy(numeratorSiteId = siteId)
    }

    fun onRatioDenominatorChange(siteId: String?) {
        _ratio.value = _ratio.value.copy(denominatorSiteId = siteId)
    }

    fun toggleSiteVisibility(siteId: String) {
        val hidden = _hiddenSiteIds.value
        _hiddenSiteIds.value = if (siteId in hidden) hidden - siteId else hidden + siteId
    }

    /**
     * Opens a logged session for correction: the editor switches to that day and every field falls
     * back to what that day holds. Typed drafts are dropped — they belonged to the day being left.
     */
    fun editDay(epochDay: Long) {
        _drafts.value = emptyMap()
        _editingEpochDay.value = epochDay
        _isAddExpanded.value = true
    }

    /** Back to logging today, with the fields prefilled from each site's last value again. */
    fun resetToToday() {
        resetEditor()
    }

    private fun resetEditor() {
        _drafts.value = emptyMap()
        _editingEpochDay.value = DateUtils.todayEpochDay()
    }

    /** Removes a whole session. The chart loses that day's points for every site measured in it. */
    fun deleteDay(epochDay: Long) {
        viewModelScope.launch {
            measurementRepository.deleteDay(epochDay)
            // Nothing left to correct on that day, so the editor must not stay pointed at it.
            if (_editingEpochDay.value == epochDay) resetEditor()
        }
    }

    /**
     * Makes the stored day match the editor: every row holding a usable number is written to the day
     * being edited — including the ones left at their prefilled value, which is the point of
     * prefilling them — and every field cleared of a value that *was* stored is deleted.
     *
     * A row left empty that had nothing stored is skipped rather than written as zero, so a site
     * that wasn't measured doesn't get a made-up point.
     */
    fun save() {
        // The drafts are read directly, not through [uiState]: that flow is derived and only
        // recomputes once its collector is resumed, so a value typed and saved in the same frame
        // would otherwise be saved at its *previous* value. The rows supply the prefill fallback for
        // fields the user never touched, and what the day already holds.
        val drafts = _drafts.value
        val rows = uiState.value.rows
        val day = _editingEpochDay.value
        viewModelScope.launch {
            rows.forEach { row ->
                val text = drafts[row.site.id] ?: row.draft
                val value = text.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
                when {
                    value != null -> measurementRepository.logMeasurement(row.site.id, day, value)
                    row.savedValueCm != null -> measurementRepository.deleteMeasurement(row.site.id, day)
                }
            }
            // Closed before the day is reset, so a screen watching the editing day for "a session
            // was opened for correction" never sees a half-updated state that looks like one.
            _isAddExpanded.value = false
            // Back to logging today, prefilled from the last entry — which may be what was just saved.
            resetEditor()
        }
    }
}
