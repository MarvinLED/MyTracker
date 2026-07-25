package com.example.prokject2_tracker.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.datastore.WeightUnit
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.kgToLb
import com.example.prokject2_tracker.core.util.lbToKg
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A past entry paired with its value already converted to the user's preferred display unit. */
data class WeightHistoryRow(val entry: BodyWeightEntry, val displayValue: Double)

/** How far back the weight chart looks. [days] null means "every entry there is". */
enum class WeightChartRange(val days: Int?) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null),
}

fun WeightChartRange.label(): String = when (this) {
    WeightChartRange.WEEK -> "Woche"
    WeightChartRange.MONTH -> "Monat"
    WeightChartRange.YEAR -> "Jahr"
    WeightChartRange.ALL -> "Gesamt"
}

data class WeightUiState(
    val editingEpochDay: Long,
    val weightUnit: WeightUnit = WeightUnit.KG,
    /** null when there's no entry yet for [editingEpochDay]. Already converted for display. */
    val editingDisplayValue: Double? = null,
    val history: List<WeightHistoryRow> = emptyList(),
    val chartPoints: List<MetricPoint> = emptyList(),
    val chartRange: WeightChartRange = WeightChartRange.MONTH,
)

private fun Double.toDisplayUnit(unit: WeightUnit): Double = when (unit) {
    WeightUnit.KG -> this
    WeightUnit.LB -> kgToLb()
}

/**
 * Deliberately skips this app's usual prev/next-day navigation (see e.g.
 * [com.example.prokject2_tracker.fluid.FluidViewModel]) since there's at most one weight value per
 * day: instead an "editing day" defaulting to today, resettable via [resetToToday], and a history
 * list where tapping a past entry loads it for editing via [selectEntry].
 */
@HiltViewModel
class WeightViewModel @Inject constructor(
    private val bodyWeightRepository: BodyWeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _editingEpochDay = MutableStateFlow(DateUtils.todayEpochDay())
    val editingEpochDay: StateFlow<Long> = _editingEpochDay.asStateFlow()

    private val _chartRange = MutableStateFlow(WeightChartRange.MONTH)

    val uiState: StateFlow<WeightUiState> = combine(
        _editingEpochDay.flatMapLatest { epochDay ->
            combine(
                bodyWeightRepository.observeForDay(epochDay),
                bodyWeightRepository.observeAll(),
            ) { entryForDay, history -> Triple(epochDay, entryForDay, history) }
        },
        userPreferencesRepository.userPreferences,
        _chartRange,
    ) { (epochDay, entryForDay, history), prefs, range ->
        // The range is a window ending today, not ending at the last entry: "letzte Woche" should
        // look empty if nothing was logged in it, rather than silently showing older data.
        val cutoff = range.days?.let { DateUtils.todayEpochDay() - (it - 1) }
        WeightUiState(
            editingEpochDay = epochDay,
            weightUnit = prefs.weightUnit,
            editingDisplayValue = entryForDay?.weightKg?.toDisplayUnit(prefs.weightUnit),
            history = history
                .sortedByDescending { it.epochDay }
                .map { WeightHistoryRow(it, it.weightKg.toDisplayUnit(prefs.weightUnit)) },
            chartPoints = history
                .filter { cutoff == null || it.epochDay >= cutoff }
                .sortedBy { it.epochDay }
                .map { MetricPoint(it.epochDay, it.weightKg.toDisplayUnit(prefs.weightUnit)) },
            chartRange = range,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WeightUiState(editingEpochDay = _editingEpochDay.value),
        )

    fun onChartRangeChange(range: WeightChartRange) {
        _chartRange.value = range
    }

    fun resetToToday() {
        _editingEpochDay.value = DateUtils.todayEpochDay()
    }

    fun selectEntry(entry: BodyWeightEntry) {
        _editingEpochDay.value = entry.epochDay
    }

    /** [displayValue] is in the user's current preferred unit; converted to kg before storing. */
    fun save(displayValue: Double) {
        val weightKg = when (uiState.value.weightUnit) {
            WeightUnit.KG -> displayValue
            WeightUnit.LB -> displayValue.lbToKg()
        }
        viewModelScope.launch { bodyWeightRepository.logWeight(_editingEpochDay.value, weightKg) }
    }

    fun delete(entry: BodyWeightEntry) {
        viewModelScope.launch { bodyWeightRepository.delete(entry) }
    }
}
