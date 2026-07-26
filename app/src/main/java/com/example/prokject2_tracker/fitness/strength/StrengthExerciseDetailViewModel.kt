package com.example.prokject2_tracker.fitness.strength

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.metrics.ChartRange
import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.bucketBy
import com.example.prokject2_tracker.core.metrics.granularityFor
import com.example.prokject2_tracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Where the steppers start when the exercise has never been logged. */
private const val DEFAULT_WEIGHT_KG = 20.0
private const val DEFAULT_REPS = 8

data class StrengthExerciseDetailUiState(
    val exerciseName: String = "",
    val selectedEpochDay: Long = DateUtils.todayEpochDay(),
    val currentSession: SessionStats? = null,
    val previousSession: SessionStats? = null,
    /** Current minus previous session volume; null while either side is missing. */
    val volumeDeltaKg: Double? = null,
    val recentSessions: List<SessionStats> = emptyList(),
    val weightKg: Double = DEFAULT_WEIGHT_KG,
    val isBodyweight: Boolean = false,
    val reps: Int = DEFAULT_REPS,
    val note: String = "",
    val canUndoRemoval: Boolean = false,
    val chartRange: ChartRange = ChartRange.YEAR,
    /** What one chart point covers at the current range — shown in the header. */
    val chartGranularity: Granularity = Granularity.WEEKLY,
    val volumeSeries: List<MetricPoint> = emptyList(),
    val maxWeightSeries: List<MetricPoint> = emptyList(),
    val setCountSeries: List<MetricPoint> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StrengthExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strengthLogRepository: StrengthLogRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    private val route: StrengthExerciseDetailRoute = savedStateHandle.toRoute()

    private val _selectedEpochDay = MutableStateFlow(route.epochDay)
    private val _exerciseName = MutableStateFlow("")
    private val _note = MutableStateFlow("")

    /**
     * The working copy of the selected day's sets, mutated synchronously on every tap; the database
     * is a mirror of it. Null means "no local edits, follow the database".
     *
     * A [Mutex] around the writes alone would not be enough: if a second tap computed its new list
     * from a stale database snapshot, the serialized second write would still drop the first tap's
     * set. Mutating this in the tap itself is what makes rapid tapping safe.
     */
    private val _draftSets = MutableStateFlow<List<SetDraft>?>(null)
    private val writeMutex = Mutex()

    private val _weightKg = MutableStateFlow(DEFAULT_WEIGHT_KG)
    private val _isBodyweight = MutableStateFlow(false)
    private val _reps = MutableStateFlow(DEFAULT_REPS)
    private val _lastRemoved = MutableStateFlow<Pair<Int, SetDraft>?>(null)
    private val _chartRange = MutableStateFlow(ChartRange.YEAR)

    private val allSets: StateFlow<List<StrengthSet>> =
        strengthLogRepository.observeSetsForExercise(route.exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<StrengthExerciseDetailUiState> = combine(
        allSets,
        _selectedEpochDay,
        _draftSets,
        combine(_weightKg, _isBodyweight, _reps, _lastRemoved) { weight, bodyweight, reps, removed ->
            StepperState(weight, bodyweight, reps, removed != null)
        },
        combine(_exerciseName, _note, _chartRange) { name, note, range -> Triple(name, note, range) },
    ) { sets, day, draft, stepper, (name, note, range) ->
        val persisted = sets.sessionOn(day)
        // The draft wins while it exists, so the UI never waits for a database round-trip.
        val current = draft?.let { drafted ->
            if (drafted.isEmpty()) {
                null
            } else {
                SessionStats(day, drafted, maxWeightOf(drafted), volumeOf(drafted))
            }
        } ?: persisted
        val previous = sets.previousSessionDay(before = day)?.let { sets.sessionOn(it) }
        val window = sets.chartWindow(range)
        val granularity = range.granularityFor(window.spanDays())

        StrengthExerciseDetailUiState(
            exerciseName = name,
            selectedEpochDay = day,
            currentSession = current,
            previousSession = previous,
            volumeDeltaKg = if (current != null && previous != null) current.volumeKg - previous.volumeKg else null,
            // Excludes the selected day: that one is already the "Dieses Training" column.
            recentSessions = sets.filter { it.epochDay != day }.recentSessions(limit = 5),
            weightKg = stepper.weightKg,
            isBodyweight = stepper.isBodyweight,
            reps = stepper.reps,
            note = note,
            canUndoRemoval = stepper.canUndo,
            chartRange = range,
            chartGranularity = granularity,
            volumeSeries = window.dailyVolumePoints().bucketBy(granularity, MetricAggregation.SUM),
            maxWeightSeries = window.dailyMaxWeightPoints().bucketBy(granularity, MetricAggregation.MAX),
            setCountSeries = window.dailySetCountPoints().bucketBy(granularity, MetricAggregation.SUM),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StrengthExerciseDetailUiState(selectedEpochDay = route.epochDay),
    )

    private data class StepperState(
        val weightKg: Double,
        val isBodyweight: Boolean,
        val reps: Int,
        val canUndo: Boolean,
    )

    init {
        viewModelScope.launch {
            _exerciseName.value = strengthExerciseRepository.getById(route.exerciseId)?.name.orEmpty()
        }
        viewModelScope.launch { seedForDay(route.epochDay) }
    }

    // --- date navigation -------------------------------------------------------------------

    fun goToPreviousDay() = selectDay(_selectedEpochDay.value - 1)
    fun goToNextDay() = selectDay(_selectedEpochDay.value + 1)

    fun selectDay(epochDay: Long) {
        if (epochDay == _selectedEpochDay.value) return
        // Nothing to save — every committed set is already persisted, so a date change is pure
        // navigation. The draft and the undo belong to the day being left, though.
        _draftSets.value = null
        _lastRemoved.value = null
        _selectedEpochDay.value = epochDay
        viewModelScope.launch { seedForDay(epochDay) }
    }

    /**
     * Puts the steppers where the lifter most likely wants them: whatever they last did on this day,
     * else what they last did the session before it, else the most recent set ever logged for this
     * exercise, else a neutral default.
     */
    private suspend fun seedForDay(epochDay: Long) {
        // Queried straight from the repository rather than read off `allSets`: on the first pass
        // nothing is collecting that StateFlow yet, so it would still hold its empty initial value
        // and every open would fall through to "the newest set ever" — wrong when arriving on a
        // historic day from the training history.
        val sets = strengthLogRepository.observeSetsForExercise(route.exerciseId).first()
        val seed = sets.sessionOn(epochDay)?.sets?.lastOrNull()
            ?: sets.previousSessionDay(before = epochDay)?.let { sets.sessionOn(it)?.sets?.lastOrNull() }
            ?: strengthLogRepository.getMostRecentSetForExercise(route.exerciseId)?.toDraft()
        _weightKg.value = seed?.weightKg ?: DEFAULT_WEIGHT_KG
        _isBodyweight.value = seed != null && seed.weightKg == null
        _reps.value = seed?.reps ?: DEFAULT_REPS
        _note.value = strengthLogRepository.observeEntriesForExercise(route.exerciseId).first()
            .firstOrNull { it.epochDay == epochDay }?.note.orEmpty()
    }

    // --- steppers --------------------------------------------------------------------------

    fun adjustWeight(deltaKg: Double) {
        if (_isBodyweight.value) return
        _weightKg.value = (_weightKg.value + deltaKg).coerceAtLeast(0.0)
    }

    fun setWeight(weightKg: Double) {
        _weightKg.value = weightKg.coerceAtLeast(0.0)
        _isBodyweight.value = false
    }

    fun toggleBodyweight() {
        _isBodyweight.value = !_isBodyweight.value
    }

    fun adjustReps(delta: Int) {
        _reps.value = (_reps.value + delta).coerceAtLeast(1)
    }

    // --- set list --------------------------------------------------------------------------

    /** The one-tap action: append a set at the current weight and rep count. */
    fun commitSet() {
        val set = SetDraft(reps = _reps.value, weightKg = if (_isBodyweight.value) null else _weightKg.value)
        _lastRemoved.value = null
        writeSets(currentSets() + set)
    }

    fun removeSetAt(index: Int) {
        val sets = currentSets()
        if (index !in sets.indices) return
        _lastRemoved.value = index to sets[index]
        writeSets(sets.filterIndexed { i, _ -> i != index })
    }

    fun undoRemoval() {
        val (index, set) = _lastRemoved.value ?: return
        _lastRemoved.value = null
        val sets = currentSets()
        writeSets(sets.subList(0, index.coerceAtMost(sets.size)) + set + sets.drop(index))
    }

    /** Tapping a set chip puts its weight and reps back into the steppers. */
    fun resumeAt(index: Int) {
        val set = currentSets().getOrNull(index) ?: return
        _isBodyweight.value = set.weightKg == null
        set.weightKg?.let { _weightKg.value = it }
        _reps.value = set.reps
    }

    fun onChartRangeChange(range: ChartRange) {
        _chartRange.value = range
    }

    fun onNoteChange(value: String) {
        _note.value = value
    }

    /** Notes are written on focus loss rather than per keystroke. */
    fun persistNote() {
        if (currentSets().isEmpty()) return
        writeSets(currentSets())
    }

    private fun currentSets(): List<SetDraft> =
        _draftSets.value ?: allSets.value.sessionOn(_selectedEpochDay.value)?.sets.orEmpty()

    private fun writeSets(newSets: List<SetDraft>) {
        _draftSets.value = newSets
        val day = _selectedEpochDay.value
        val note = _note.value.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            writeMutex.withLock {
                // Backing out of the screen right after a tap must not cancel that tap's write.
                withContext(NonCancellable) {
                    strengthLogRepository.saveSession(
                        exerciseId = route.exerciseId,
                        exerciseName = _exerciseName.value,
                        epochDay = day,
                        note = note,
                        sets = newSets.map { it.reps to it.weightKg },
                    )
                }
            }
        }
    }
}

/**
 * The sets the chart's x-axis covers. The window ends at the **last logged day**, not today: after a
 * week off, a one-week window anchored on today would be empty, while anchored on the last session
 * it still shows that week of training. Older sets stay in the database, they're just off-screen.
 */
private fun List<StrengthSet>.chartWindow(range: ChartRange): List<StrengthSet> {
    val days = range.days ?: return this
    val lastDay = maxOfOrNull { it.epochDay } ?: return this
    return filter { it.epochDay > lastDay - days }
}

/** Days from the first to the last set in the window — what decides [ChartRange.ALL]'s resolution. */
private fun List<StrengthSet>.spanDays(): Long {
    val first = minOfOrNull { it.epochDay } ?: return 0
    val last = maxOfOrNull { it.epochDay } ?: return 0
    return last - first
}
