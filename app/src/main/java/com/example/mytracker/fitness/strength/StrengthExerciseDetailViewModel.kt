package com.example.mytracker.fitness.strength

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.Granularity
import com.example.mytracker.core.metrics.MetricAggregation
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.bucketBy
import com.example.mytracker.core.metrics.granularityFor
import com.example.mytracker.core.util.DateUtils
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
    val recentSessions: List<SessionStats> = emptyList(),
    /** The steppers' weight: the whole load normally, the *added* weight while [isBodyweight]. */
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
    /**
     * The two lower blocks fold away so the session comparison — the answer to "war das gut?" —
     * needs no scrolling. The Eingabe starts open because it is why the screen gets opened; the
     * Verlauf starts closed because it is for reviewing, not for logging.
     */
    val isEntryExpanded: Boolean = true,
    val isChartExpanded: Boolean = false,
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
    private val _panels = MutableStateFlow(PanelState())

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
        combine(_exerciseName, _note, _chartRange, _panels) { name, note, range, panels ->
            ScreenState(name, note, range, panels)
        },
    ) { sets, day, draft, stepper, screen ->
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
        val window = sets.chartWindow(screen.chartRange)
        val granularity = screen.chartRange.granularityFor(window.spanDays())

        StrengthExerciseDetailUiState(
            exerciseName = screen.exerciseName,
            selectedEpochDay = day,
            currentSession = current,
            previousSession = previous,
            // Excludes the selected day: that one is already the "Dieses Training" column.
            recentSessions = sets.filter { it.epochDay != day }.recentSessions(limit = 5),
            weightKg = stepper.weightKg,
            isBodyweight = stepper.isBodyweight,
            reps = stepper.reps,
            note = screen.note,
            canUndoRemoval = stepper.canUndo,
            chartRange = screen.chartRange,
            chartGranularity = granularity,
            volumeSeries = window.dailyVolumePoints().bucketBy(granularity, MetricAggregation.SUM),
            maxWeightSeries = window.dailyMaxWeightPoints().bucketBy(granularity, MetricAggregation.MAX),
            isEntryExpanded = screen.panels.entryExpanded,
            isChartExpanded = screen.panels.chartExpanded,
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

    /** Which of the two foldable blocks are open; see [StrengthExerciseDetailUiState.isEntryExpanded]. */
    private data class PanelState(val entryExpanded: Boolean = true, val chartExpanded: Boolean = false)

    /** The screen-level bits, bundled because [combine] takes a fixed number of sources. */
    private data class ScreenState(
        val exerciseName: String,
        val note: String,
        val chartRange: ChartRange,
        val panels: PanelState,
    )

    fun toggleEntryExpanded() {
        _panels.value = _panels.value.copy(entryExpanded = !_panels.value.entryExpanded)
    }

    fun toggleChartExpanded() {
        _panels.value = _panels.value.copy(chartExpanded = !_panels.value.chartExpanded)
    }

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
        // Nothing logged yet: the exercise's own kind decides. Klimmzüge open in bodyweight mode
        // with no added weight instead of at a made-up 20 kg.
        val exerciseIsBodyweight = strengthExerciseRepository.getById(route.exerciseId)?.isBodyweight == true
        _isBodyweight.value = seed?.isBodyweight ?: exerciseIsBodyweight
        _weightKg.value = seed?.weightKg ?: if (_isBodyweight.value) 0.0 else DEFAULT_WEIGHT_KG
        _reps.value = seed?.reps ?: DEFAULT_REPS
        _note.value = strengthLogRepository.observeEntriesForExercise(route.exerciseId).first()
            .firstOrNull { it.epochDay == epochDay }?.note.orEmpty()
    }

    // --- steppers --------------------------------------------------------------------------

    /** In bodyweight mode this steps the *added* weight — the belt on a weighted pull-up. */
    fun adjustWeight(deltaKg: Double) {
        _weightKg.value = (_weightKg.value + deltaKg).coerceAtLeast(0.0)
    }

    /** Same reading as [adjustWeight]: the typed number is the added weight while in bodyweight mode. */
    fun setWeight(weightKg: Double) {
        _weightKg.value = weightKg.coerceAtLeast(0.0)
    }

    /**
     * Switching modes resets the number rather than carrying it over: 60 kg of bench press is not
     * 60 kg hanging off a belt, and reading it as such would log a set nobody did.
     */
    fun toggleBodyweight() {
        val bodyweight = !_isBodyweight.value
        _isBodyweight.value = bodyweight
        _weightKg.value = if (bodyweight) 0.0 else DEFAULT_WEIGHT_KG
    }

    fun adjustReps(delta: Int) {
        _reps.value = (_reps.value + delta).coerceAtLeast(1)
    }

    // --- set list --------------------------------------------------------------------------

    /** The one-tap action: append a set at the current weight and rep count. */
    fun commitSet() {
        val bodyweight = _isBodyweight.value
        val set = SetDraft(
            reps = _reps.value,
            // Null, not 0.0, when nothing is added: "no external weight" is what keeps a plain
            // pull-up out of the Max-Gewicht series (see [maxWeightOf]).
            weightKg = if (bodyweight) _weightKg.value.takeIf { it > 0.0 } else _weightKg.value,
            isBodyweight = bodyweight,
        )
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
        _isBodyweight.value = set.isBodyweight
        _weightKg.value = set.weightKg ?: if (set.isBodyweight) 0.0 else DEFAULT_WEIGHT_KG
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
                        sets = newSets,
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
