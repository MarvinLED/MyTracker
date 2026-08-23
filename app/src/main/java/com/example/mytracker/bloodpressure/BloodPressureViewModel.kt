package com.example.mytracker.bloodpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.toLocaleDoubleOrNull
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
 * The values a cuff shows. Fixed, not a user-managed library: this *is* what gets measured.
 * [BloodPressureSeries] pairs each with a time of day for the chart.
 */
enum class BloodPressureMeasure { SYSTOLIC, DIASTOLIC, PULSE }

fun BloodPressureMeasure.label(): String = when (this) {
    BloodPressureMeasure.SYSTOLIC -> "Systolisch"
    BloodPressureMeasure.DIASTOLIC -> "Diastolisch"
    BloodPressureMeasure.PULSE -> "Puls"
}

/** The pulse is the one measure that is not in mmHg, so the unit belongs to the measure. */
fun BloodPressureMeasure.unit(): String = when (this) {
    BloodPressureMeasure.SYSTOLIC, BloodPressureMeasure.DIASTOLIC -> BLOOD_PRESSURE_UNIT
    BloodPressureMeasure.PULSE -> PULSE_UNIT
}

/** This measure's value for one slot: the mean of its measurements, null when it was not measured. */
fun BloodPressureMeasure.valueOf(entry: BloodPressureEntry): Double? = when (this) {
    BloodPressureMeasure.SYSTOLIC -> entry.averageSystolic
    BloodPressureMeasure.DIASTOLIC -> entry.averageDiastolic
    BloodPressureMeasure.PULSE -> entry.averagePulse
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
            BloodPressureMeasure.PULSE -> 4
        } + if (timeOfDay == BloodPressureTimeOfDay.MORNING) 0 else 1
}

/** A past reading, ready for display. Always the mean — see [BloodPressureEntry]. */
data class BloodPressureHistoryRow(val entry: BloodPressureEntry) {
    val values: String
        get() = "${entry.averageSystolic.formatCompact()}/${entry.averageDiastolic.formatCompact()} " +
            BLOOD_PRESSURE_UNIT

    /** Null when no measurement of this slot carried a pulse. */
    val pulse: String? get() = entry.averagePulse?.let { "${it.formatCompact()}$PULSE_UNIT" }

    /**
     * Only for a slot measured twice: says that the numbers above are a mean, and what it was taken
     * from. Without it a 125,5 would look like a cuff reading nobody's cuff ever showed.
     */
    val averagedFrom: String?
        get() = if (!entry.hasSecondMeasurement) {
            null
        } else {
            "Ø aus ${entry.systolic.formatCompact()}/${entry.diastolic.formatCompact()} und " +
                "${entry.systolic2?.formatCompact()}/${entry.diastolic2?.formatCompact()}"
        }
}

/**
 * One cuff reading as it is being typed. A slot's form holds two of these — the second one empty
 * until the user asks for it.
 *
 * [pulse] is optional: plenty of readings are written down without one, and demanding it would make
 * the save button depend on a number the meter may not even show. What it must not be is *wrong*,
 * so a pulse field with something unparseable in it does block the save.
 */
data class BloodPressureReadingDraft(
    val systolic: String = "",
    val diastolic: String = "",
    val pulse: String = "",
) {
    val systolicValue: Double? get() = systolic.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
    val diastolicValue: Double? get() = diastolic.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
    val pulseValue: Double? get() = pulse.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }

    val isBlank: Boolean get() = systolic.isBlank() && diastolic.isBlank() && pulse.isBlank()

    /** Both cuff values or nothing: half a reading isn't a blood pressure. */
    val isComplete: Boolean
        get() = systolicValue != null && diastolicValue != null && (pulse.isBlank() || pulseValue != null)
}

data class BloodPressureUiState(
    val isAddExpanded: Boolean = false,
    /** The day being logged. Defaults to today; freely pickable, so readings can be entered later. */
    val epochDay: Long = DateUtils.todayEpochDay(),
    val timeOfDay: BloodPressureTimeOfDay = BloodPressureTimeOfDay.MORNING,
    val first: BloodPressureReadingDraft = BloodPressureReadingDraft(),
    val second: BloodPressureReadingDraft = BloodPressureReadingDraft(),
    /** Whether the second measurement's fields are on screen — see [BloodPressureViewModel.toggleSecondMeasurement]. */
    val isSecondShown: Boolean = false,
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
    /** A second measurement that was started but left half-typed blocks the save rather than being dropped. */
    val canSave: Boolean get() = first.isComplete && (second.isBlank || second.isComplete)

    /**
     * What saving would file, spelled out — but only once two measurements make that something other
     * than what is already in the fields. A mean nobody typed has to be visible before it is stored.
     */
    val averagePreview: String? get() = averageLabelOf(first, second)
}

/**
 * The mean of two complete measurements, formatted for the form: null while there is only one
 * measurement, or while either of them is still incomplete and no mean is defined yet.
 */
fun averageLabelOf(first: BloodPressureReadingDraft, second: BloodPressureReadingDraft): String? {
    if (second.isBlank || !first.isComplete || !second.isComplete) return null
    val systolic = (first.systolicValue!! + second.systolicValue!!) / 2.0
    val diastolic = (first.diastolicValue!! + second.diastolicValue!!) / 2.0
    val pulses = listOfNotNull(first.pulseValue, second.pulseValue)
    val pulse = pulses.takeIf { it.isNotEmpty() }?.average()
    return buildString {
        append("${systolic.formatCompact()}/${diastolic.formatCompact()} $BLOOD_PRESSURE_UNIT")
        if (pulse != null) append(" · Puls ${pulse.formatCompact()}$PULSE_UNIT")
    }
}

/**
 * Which reading the form starts on. Before midday the next thing you'd log is the morning one; after
 * it, the evening one. Split out as a pure function of the hour so it can be tested without a clock.
 */
fun defaultTimeOfDay(hourOfDay: Int): BloodPressureTimeOfDay =
    if (hourOfDay < 12) BloodPressureTimeOfDay.MORNING else BloodPressureTimeOfDay.EVENING

/**
 * Every field of the form as typed. null per field means "not typed yet", which is what makes it
 * fall back to the prefilled last value; an empty string is a field the user actually cleared.
 */
private data class BloodPressureDrafts(
    val systolic: String? = null,
    val diastolic: String? = null,
    val pulse: String? = null,
    val systolic2: String? = null,
    val diastolic2: String? = null,
    val pulse2: String? = null,
    val comment: String? = null,
)

/** The pickers and folds of the form, bundled so the state flows stay inside combine's arity. */
private data class BloodPressureFormState(
    val isAddExpanded: Boolean = false,
    val epochDay: Long = DateUtils.todayEpochDay(),
    val timeOfDay: BloodPressureTimeOfDay,
    val isSecondRequested: Boolean = false,
)

@HiltViewModel
class BloodPressureViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository,
) : ViewModel() {
    private val _form = MutableStateFlow(
        BloodPressureFormState(timeOfDay = defaultTimeOfDay(LocalTime.now().hour)),
    )
    private val _drafts = MutableStateFlow(BloodPressureDrafts())
    private val _chartRange = MutableStateFlow(ChartRange.MONTH)
    private val _hiddenSeriesKeys = MutableStateFlow(emptySet<String>())

    val uiState: StateFlow<BloodPressureUiState> = combine(
        bloodPressureRepository.observeAll(),
        _drafts,
        _form,
        _chartRange,
        _hiddenSeriesKeys,
    ) { entries, drafts, form, range, hidden ->
        // Prefill follows both pickers: the newest reading of the selected time of day that is not
        // *after* the selected day. Picking a past date must not offer numbers from a later one, and
        // when that day already holds a reading this resolves to exactly it — so opening a filled
        // slot shows what is stored there instead of silently overwriting it with another day's values.
        val last = entries
            .filter { it.timeOfDay == form.timeOfDay && it.epochDay <= form.epochDay }
            .maxByOrNull { it.epochDay }
        val isEditingExisting = last?.epochDay == form.epochDay

        val first = BloodPressureReadingDraft(
            systolic = drafts.systolic ?: last?.systolic?.formatCompact().orEmpty(),
            diastolic = drafts.diastolic ?: last?.diastolic?.formatCompact().orEmpty(),
            pulse = drafts.pulse ?: last?.pulse?.formatCompact().orEmpty(),
        )
        val second = BloodPressureReadingDraft(
            systolic = drafts.systolic2 ?: last?.systolic2?.formatCompact().orEmpty(),
            diastolic = drafts.diastolic2 ?: last?.diastolic2?.formatCompact().orEmpty(),
            pulse = drafts.pulse2 ?: last?.pulse2?.formatCompact().orEmpty(),
        )

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
                        // The pulse is optional, so its line skips the slots without one rather than
                        // drawing a hole at zero.
                        .mapNotNull { entry ->
                            measure.valueOf(entry)?.let { MetricPoint(entry.epochDay, it) }
                        },
                )
            }
        }

        BloodPressureUiState(
            isAddExpanded = form.isAddExpanded,
            epochDay = form.epochDay,
            timeOfDay = form.timeOfDay,
            first = first,
            second = second,
            // A prefilled second measurement shows itself: the fold is for adding one, never for
            // hiding one that is already stored.
            isSecondShown = form.isSecondRequested || !second.isBlank,
            // A comment belongs to its reading and is never carried over to a new one — but when the
            // selected slot *is* that reading, hiding its comment would quietly drop it on save.
            commentDraft = drafts.comment ?: last?.comment.takeIf { isEditingExisting }.orEmpty(),
            prefilledFrom = last.takeIf { drafts.systolic == null && drafts.diastolic == null },
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
        _form.value = _form.value.copy(isAddExpanded = !_form.value.isAddExpanded)
    }

    /** Switching the time of day re-prefills from that half of the day's last reading. */
    fun onTimeOfDayChange(timeOfDay: BloodPressureTimeOfDay) {
        if (timeOfDay == _form.value.timeOfDay) return
        _form.value = _form.value.copy(timeOfDay = timeOfDay)
        clearDrafts()
    }

    /** Same as [onTimeOfDayChange]: a new day means a new slot, so the prefill is re-derived for it. */
    fun onDateChange(epochDay: Long) {
        if (epochDay == _form.value.epochDay) return
        _form.value = _form.value.copy(epochDay = epochDay)
        clearDrafts()
    }

    fun onSystolicChange(value: String) { _drafts.value = _drafts.value.copy(systolic = value) }
    fun onDiastolicChange(value: String) { _drafts.value = _drafts.value.copy(diastolic = value) }
    fun onPulseChange(value: String) { _drafts.value = _drafts.value.copy(pulse = value) }
    fun onSecondSystolicChange(value: String) { _drafts.value = _drafts.value.copy(systolic2 = value) }
    fun onSecondDiastolicChange(value: String) { _drafts.value = _drafts.value.copy(diastolic2 = value) }
    fun onSecondPulseChange(value: String) { _drafts.value = _drafts.value.copy(pulse2 = value) }
    fun onCommentChange(value: String) { _drafts.value = _drafts.value.copy(comment = value) }

    /**
     * Opens the second measurement's fields, or takes that measurement back off the slot.
     *
     * Closing writes empty strings rather than nulls: a null draft means "not typed yet" and would
     * fall straight back to the stored second measurement, which is exactly what closing is meant to
     * remove. Saving afterwards leaves the slot with one measurement again.
     */
    fun toggleSecondMeasurement() {
        val shown = uiState.value.isSecondShown
        _form.value = _form.value.copy(isSecondRequested = !shown)
        if (shown) {
            _drafts.value = _drafts.value.copy(systolic2 = "", diastolic2 = "", pulse2 = "")
        }
    }

    fun onChartRangeChange(range: ChartRange) {
        _chartRange.value = range
    }

    fun toggleSeriesVisibility(key: String) {
        val hidden = _hiddenSeriesKeys.value
        _hiddenSeriesKeys.value = if (key in hidden) hidden - key else hidden + key
    }

    /**
     * Logs the reading for the selected day and time of day, storing both measurements as they were
     * taken — the mean the app goes by is derived from them, see [BloodPressureEntry].
     *
     * The drafts are read directly rather than through [uiState], which is derived and would still
     * hold the previous value for anything typed in the same frame; [uiState] only supplies the
     * prefill for fields left untouched.
     */
    fun save() {
        val state = uiState.value
        val drafts = _drafts.value
        val first = BloodPressureReadingDraft(
            systolic = drafts.systolic ?: state.first.systolic,
            diastolic = drafts.diastolic ?: state.first.diastolic,
            pulse = drafts.pulse ?: state.first.pulse,
        )
        val second = BloodPressureReadingDraft(
            systolic = drafts.systolic2 ?: state.second.systolic,
            diastolic = drafts.diastolic2 ?: state.second.diastolic,
            pulse = drafts.pulse2 ?: state.second.pulse,
        )
        if (!first.isComplete) return
        // Half a second measurement is a slip, not a reading: filing it would silently turn typed
        // numbers into a mean of something the user never finished entering.
        if (!second.isBlank && !second.isComplete) return

        val comment = drafts.comment ?: state.commentDraft
        val timeOfDay = _form.value.timeOfDay
        val epochDay = _form.value.epochDay
        viewModelScope.launch {
            bloodPressureRepository.logEntry(
                epochDay = epochDay,
                timeOfDay = timeOfDay,
                systolic = first.systolicValue!!,
                diastolic = first.diastolicValue!!,
                pulse = first.pulseValue,
                systolic2 = second.systolicValue.takeIf { !second.isBlank },
                diastolic2 = second.diastolicValue.takeIf { !second.isBlank },
                pulse2 = second.pulseValue.takeIf { !second.isBlank },
                comment = comment,
            )
            clearDrafts()
            // Back to today: the form is done, and a date left on last Tuesday would quietly file
            // tomorrow's reading there. Picking a past day again is two taps.
            _form.value = _form.value.copy(
                epochDay = DateUtils.todayEpochDay(),
                isAddExpanded = false,
            )
        }
    }

    fun delete(entry: BloodPressureEntry) {
        viewModelScope.launch { bloodPressureRepository.delete(entry) }
    }

    private fun clearDrafts() {
        _drafts.value = BloodPressureDrafts()
        _form.value = _form.value.copy(isSecondRequested = false)
    }
}
