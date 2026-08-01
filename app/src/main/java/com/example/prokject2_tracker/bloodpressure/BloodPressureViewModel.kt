package com.example.prokject2_tracker.bloodpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.metrics.ChartRange
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How many past readings the list under the chart shows — enough to read back a comment, not a log. */
private const val HISTORY_LIMIT = 10

/**
 * The two values a cuff shows. Fixed, not a user-managed library: this *is* what gets measured.
 * [BloodPressureSeries] pairs each with a time of day for the chart.
 */
enum class BloodPressureMeasure { SYSTOLIC, DIASTOLIC }

fun BloodPressureMeasure.label(): String = when (this) {
    BloodPressureMeasure.SYSTOLIC -> "Systolisch"
    BloodPressureMeasure.DIASTOLIC -> "Diastolisch"
}

/**
 * One line in the chart. Morning and evening stay separate series rather than being merged per day:
 * two readings on one day are two different measurements, and merging them would put two points on
 * the same x with only one of them readable in the crosshair.
 *
 * [paletteIndex] is fixed per series, so a line keeps its colour when the others are hidden.
 */
data class BloodPressureSeries(
    val measure: BloodPressureMeasure,
    val timeOfDay: BloodPressureTimeOfDay,
    val points: List<MetricPoint>,
) {
    val key: String get() = "$measure-$timeOfDay"

    val label: String get() = "${measure.label()} ${timeOfDay.label().lowercase()}"

    val paletteIndex: Int
        get() = when (measure) {
            BloodPressureMeasure.SYSTOLIC -> 0
            BloodPressureMeasure.DIASTOLIC -> 2
        } + if (timeOfDay == BloodPressureTimeOfDay.MORNING) 0 else 1
}

/** A past reading, ready for display. */
data class BloodPressureHistoryRow(val entry: BloodPressureEntry) {
    val values: String get() = "${entry.systolic.formatCompact()}/${entry.diastolic.formatCompact()} mmHg"
}

data class BloodPressureUiState(
    val isAddExpanded: Boolean = false,
    /** The day being logged. Defaults to today; freely pickable, so readings can be entered later. */
    val epochDay: Long = DateUtils.todayEpochDay(),
    val timeOfDay: BloodPressureTimeOfDay = BloodPressureTimeOfDay.MORNING,
    val systolicDraft: String = "",
    val diastolicDraft: String = "",
    val commentDraft: String = "",
    /** The reading the drafts were prefilled from, so the panel can say where the numbers came from. */
    val prefilledFrom: BloodPressureEntry? = null,
    /**
     * True when [prefilledFrom] *is* the reading of the selected day and time of day — saving then
     * corrects that entry rather than adding one, and the panel has to say so before it happens.
     */
    val isEditingExisting: Boolean = false,
    val chartRange: ChartRange = ChartRange.MONTH,
    val hiddenSeriesKeys: Set<String> = emptySet(),
    /** Only the visible series, already windowed to [chartRange]. */
    val series: List<BloodPressureSeries> = emptyList(),
    /** Every series that has data at all — the chips, which must also show the hidden ones. */
    val chartableSeries: List<BloodPressureSeries> = emptyList(),
    val history: List<BloodPressureHistoryRow> = emptyList(),
) {
    val systolic: Double? get() = systolicDraft.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
    val diastolic: Double? get() = diastolicDraft.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }

    /** Both values or nothing: half a reading isn't a blood pressure. */
    val canSave: Boolean get() = systolic != null && diastolic != null
}

/**
 * Which reading the form starts on. Before midday the next thing you'd log is the morning one; after
 * it, the evening one. Split out as a pure function of the hour so it can be tested without a clock.
 */
fun defaultTimeOfDay(hourOfDay: Int): BloodPressureTimeOfDay =
    if (hourOfDay < 12) BloodPressureTimeOfDay.MORNING else BloodPressureTimeOfDay.EVENING

@HiltViewModel
class BloodPressureViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository,
) : ViewModel() {
    private val _isAddExpanded = MutableStateFlow(false)
    private val _epochDay = MutableStateFlow(DateUtils.todayEpochDay())
    private val _timeOfDay = MutableStateFlow(defaultTimeOfDay(LocalTime.now().hour))
    private val _chartRange = MutableStateFlow(ChartRange.MONTH)
    private val _hiddenSeriesKeys = MutableStateFlow(emptySet<String>())

    /** null = "not typed yet", which is what makes the field fall back to the prefilled last value. */
    private val _systolicDraft = MutableStateFlow<String?>(null)
    private val _diastolicDraft = MutableStateFlow<String?>(null)
    private val _commentDraft = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BloodPressureUiState> = combine(
        bloodPressureRepository.observeAll(),
        combine(_systolicDraft, _diastolicDraft, _commentDraft) { sys, dia, comment -> Triple(sys, dia, comment) },
        combine(_isAddExpanded, _epochDay, _timeOfDay) { expanded, epochDay, timeOfDay ->
            Triple(expanded, epochDay, timeOfDay)
        },
        _chartRange,
        _hiddenSeriesKeys,
    ) { entries, (sysDraft, diaDraft, commentDraft), (expanded, epochDay, timeOfDay), range, hidden ->
        // Prefill follows both pickers: the newest reading of the selected time of day that is not
        // *after* the selected day. Picking a past date must not offer numbers from a later one, and
        // when that day already holds a reading this resolves to exactly it — so opening a filled
        // slot shows what is stored there instead of silently overwriting it with another day's values.
        val last = entries
            .filter { it.timeOfDay == timeOfDay && it.epochDay <= epochDay }
            .maxByOrNull { it.epochDay }
        val isEditingExisting = last?.epochDay == epochDay

        // The window ends at the last reading rather than today, so a chart still shows the last
        // month that was actually measured after a break. Same rule as the Maße screen.
        val lastDay = entries.maxOfOrNull { it.epochDay }
        val cutoff = range.days?.let { days -> lastDay?.minus(days - 1) }

        val allSeries = BloodPressureMeasure.entries.flatMap { measure ->
            BloodPressureTimeOfDay.entries.map { seriesTime ->
                BloodPressureSeries(
                    measure = measure,
                    timeOfDay = seriesTime,
                    points = entries
                        .filter { it.timeOfDay == seriesTime }
                        .filter { cutoff == null || it.epochDay >= cutoff }
                        .sortedBy { it.epochDay }
                        .map { entry ->
                            MetricPoint(
                                entry.epochDay,
                                when (measure) {
                                    BloodPressureMeasure.SYSTOLIC -> entry.systolic
                                    BloodPressureMeasure.DIASTOLIC -> entry.diastolic
                                },
                            )
                        },
                )
            }
        }

        BloodPressureUiState(
            isAddExpanded = expanded,
            epochDay = epochDay,
            timeOfDay = timeOfDay,
            systolicDraft = sysDraft ?: last?.systolic?.formatCompact().orEmpty(),
            diastolicDraft = diaDraft ?: last?.diastolic?.formatCompact().orEmpty(),
            // A comment belongs to its reading and is never carried over to a new one — but when the
            // selected slot *is* that reading, hiding its comment would quietly drop it on save.
            commentDraft = commentDraft ?: last?.comment.takeIf { isEditingExisting }.orEmpty(),
            prefilledFrom = last.takeIf { sysDraft == null && diaDraft == null },
            isEditingExisting = isEditingExisting,
            chartRange = range,
            hiddenSeriesKeys = hidden,
            series = allSeries.filter { it.key !in hidden && it.points.isNotEmpty() },
            chartableSeries = allSeries.filter { it.points.isNotEmpty() },
            history = entries
                .sortedWith(compareByDescending<BloodPressureEntry> { it.epochDay }.thenByDescending { it.timeOfDay })
                .take(HISTORY_LIMIT)
                .map(::BloodPressureHistoryRow),
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BloodPressureUiState())

    fun toggleAddExpanded() {
        _isAddExpanded.value = !_isAddExpanded.value
    }

    /** Switching the time of day re-prefills from that half of the day's last reading. */
    fun onTimeOfDayChange(timeOfDay: BloodPressureTimeOfDay) {
        if (timeOfDay == _timeOfDay.value) return
        _timeOfDay.value = timeOfDay
        clearDrafts()
    }

    /** Same as [onTimeOfDayChange]: a new day means a new slot, so the prefill is re-derived for it. */
    fun onDateChange(epochDay: Long) {
        if (epochDay == _epochDay.value) return
        _epochDay.value = epochDay
        clearDrafts()
    }

    fun onSystolicChange(value: String) { _systolicDraft.value = value }
    fun onDiastolicChange(value: String) { _diastolicDraft.value = value }
    fun onCommentChange(value: String) { _commentDraft.value = value }

    fun onChartRangeChange(range: ChartRange) {
        _chartRange.value = range
    }

    fun toggleSeriesVisibility(key: String) {
        val hidden = _hiddenSeriesKeys.value
        _hiddenSeriesKeys.value = if (key in hidden) hidden - key else hidden + key
    }

    /**
     * Logs the reading for the selected day and time of day. The drafts are read directly rather than
     * through [uiState], which is derived and would still hold the previous value for anything typed
     * in the same frame; [uiState] only supplies the prefill for fields left untouched.
     */
    fun save() {
        val state = uiState.value
        val systolic = (_systolicDraft.value ?: state.systolicDraft).toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
        val diastolic = (_diastolicDraft.value ?: state.diastolicDraft).toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
        if (systolic == null || diastolic == null) return
        val comment = _commentDraft.value ?: state.commentDraft
        val timeOfDay = _timeOfDay.value
        val epochDay = _epochDay.value
        viewModelScope.launch {
            bloodPressureRepository.logEntry(
                epochDay = epochDay,
                timeOfDay = timeOfDay,
                systolic = systolic,
                diastolic = diastolic,
                comment = comment,
            )
            clearDrafts()
            // Back to today: the form is done, and a date left on last Tuesday would quietly file
            // tomorrow's reading there. Picking a past day again is two taps.
            _epochDay.value = DateUtils.todayEpochDay()
            _isAddExpanded.value = false
        }
    }

    fun delete(entry: BloodPressureEntry) {
        viewModelScope.launch { bloodPressureRepository.delete(entry) }
    }

    private fun clearDrafts() {
        _systolicDraft.value = null
        _diastolicDraft.value = null
        _commentDraft.value = null
    }
}
