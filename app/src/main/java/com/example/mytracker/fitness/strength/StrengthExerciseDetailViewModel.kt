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
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalProgressRow
import com.example.mytracker.fitness.FitnessGoalRepository
import com.example.mytracker.fitness.MaxWeightGoalProgressRow
import com.example.mytracker.fitness.StrengthMaxWeightGoal
import com.example.mytracker.fitness.effectiveTargetKg
import com.example.mytracker.fitness.maxWeightGoalPlanPoints
import com.example.mytracker.fitness.toProgressRow
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    /** This session and the ones before it, newest first — see [sessionRows]. */
    val sessionRows: List<SessionRow> = emptyList(),
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
     * The long-term goal drawn into the chart: the straight line from where the plan started to what
     * it is due to be, clipped to the window on screen. Two points, which is all a plan is.
     */
    val goalPlanSeries: List<MetricPoint> = emptyList(),
    /** This exercise's Steigerungsziele, with where they stand this week and this month. */
    val goalRows: List<FitnessGoalProgressRow> = emptyList(),
    val maxWeightGoalRow: MaxWeightGoalProgressRow? = null,
    /** "seit 3. Mai 2026" — when the current target was set, out of the goal change log. */
    val goalSince: String? = null,
    val isGoalsExpanded: Boolean = true,
    /**
     * The lower blocks fold away so the session comparison — the answer to "war das gut?" — needs no
     * scrolling. The Eingabe starts open because it is why the screen gets opened; the Verlauf
     * starts closed because it is for reviewing, not for logging.
     */
    val isEntryExpanded: Boolean = true,
    val isChartExpanded: Boolean = false,
) {
    /** Nothing to draw a Ziele block for when this exercise has no goals at all. */
    val hasGoals: Boolean get() = goalRows.isNotEmpty() || maxWeightGoalRow != null
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StrengthExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strengthLogRepository: StrengthLogRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
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

    private val goalData: kotlinx.coroutines.flow.Flow<ExerciseGoalData> = combine(
        fitnessGoalRepository.observeAll(),
        fitnessGoalRepository.observeMaxWeightGoalForExercise(route.exerciseId),
        fitnessGoalRepository.observeLatestBodyWeightKg(),
        fitnessGoalRepository.observeGoalChanges(),
    ) { goals, maxWeightGoal, bodyWeightKg, changes ->
        val sinceFormatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN)
        ExerciseGoalData(
            goals = goals.filter { it.exerciseId == route.exerciseId },
            maxWeightGoal = maxWeightGoal,
            bodyWeightKg = bodyWeightKg,
            // The newest change that set (rather than cleared) this goal: what the target has been
            // in force since. Without the log this question has no answer at all — the goal row is
            // overwritten in place.
            goalSince = changes
                .filter { it.goalKey == "maxweight-${route.exerciseId}" && it.targetValue != null }
                .maxByOrNull { it.effectiveFromEpochDay }
                ?.let { DateUtils.localDateOfEpochDay(it.effectiveFromEpochDay).format(sinceFormatter) },
        )
    }

    /**
     * The goal block, prepared apart from the rest of the screen.
     *
     * Evaluating a goal — and especially its streak, which walks back over eight finished periods —
     * is a couple of dozen database queries. It depends on the goals and on the sets, and on nothing
     * else: computed inside the main [combine] it would be redone on every tap of the weight stepper,
     * which is the one place on this screen where taps come in bursts.
     */
    private val goalPresentation: kotlinx.coroutines.flow.Flow<GoalPresentation> =
        combine(goalData, allSets, _exerciseName) { goals, sets, exerciseName ->
            val today = DateUtils.todayEpochDay()
            GoalPresentation(
                data = goals,
                rows = goals.goals.map { goal ->
                    goal.toProgressRow(
                        progress = fitnessGoalRepository.getProgress(goal, today),
                        muscleGroupNames = emptyMap(),
                        exerciseNames = mapOf(route.exerciseId to exerciseName),
                        streak = fitnessGoalRepository.getStreak(goal, today),
                    )
                },
                maxWeightRow = goals.maxWeightGoal?.toProgressRow(
                    exerciseName = exerciseName,
                    currentMaxKg = sets.mapNotNull { it.weightKg }.maxOrNull(),
                    bodyWeightKg = goals.bodyWeightKg,
                    today = today,
                ),
            )
        }

    /** [ExerciseGoalData] plus what it works out to on screen — see [goalPresentation]. */
    private data class GoalPresentation(
        val data: ExerciseGoalData = ExerciseGoalData(),
        val rows: List<FitnessGoalProgressRow> = emptyList(),
        val maxWeightRow: MaxWeightGoalProgressRow? = null,
    )

    val uiState: StateFlow<StrengthExerciseDetailUiState> = combine(
        allSets,
        _selectedEpochDay,
        _draftSets,
        combine(_weightKg, _isBodyweight, _reps, _lastRemoved) { weight, bodyweight, reps, removed ->
            StepperState(weight, bodyweight, reps, removed != null)
        },
        combine(_exerciseName, _note, _chartRange, _panels, goalPresentation) { name, note, range, panels, goals ->
            ScreenState(name, note, range, panels, goals)
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
        // One more than the card lists: the oldest row still needs a session to be judged against.
        val older = sets.sessionsBefore(day, limit = MAX_EARLIER_SESSIONS + 1)
        val previous = older.firstOrNull()
        val window = sets.chartWindow(screen.chartRange)
        val granularity = screen.chartRange.granularityFor(window.spanDays())

        StrengthExerciseDetailUiState(
            exerciseName = screen.exerciseName,
            selectedEpochDay = day,
            currentSession = current,
            previousSession = previous,
            sessionRows = sessionRows(selectedEpochDay = day, current = current, older = older),
            weightKg = stepper.weightKg,
            isBodyweight = stepper.isBodyweight,
            reps = stepper.reps,
            note = screen.note,
            canUndoRemoval = stepper.canUndo,
            chartRange = screen.chartRange,
            chartGranularity = granularity,
            volumeSeries = window.dailyVolumePoints().bucketBy(granularity, MetricAggregation.SUM),
            maxWeightSeries = window.dailyMaxWeightPoints().bucketBy(granularity, MetricAggregation.MAX),
            goalPlanSeries = screen.goals.data.planPointsIn(window),
            goalRows = screen.goals.rows,
            maxWeightGoalRow = screen.goals.maxWeightRow,
            goalSince = screen.goals.data.goalSince,
            isGoalsExpanded = screen.panels.goalsExpanded,
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

    /** Which of the foldable blocks are open; see [StrengthExerciseDetailUiState.isEntryExpanded]. */
    private data class PanelState(
        val goalsExpanded: Boolean = true,
        val entryExpanded: Boolean = true,
        val chartExpanded: Boolean = false,
    )

    /** The screen-level bits, bundled because [combine] takes a fixed number of sources. */
    private data class ScreenState(
        val exerciseName: String,
        val note: String,
        val chartRange: ChartRange,
        val panels: PanelState,
        val goals: GoalPresentation,
    )

    /**
     * What this exercise is being trained *towards*, on the page where it is trained. A goal that
     * only lives on the Ziele screen is one nobody consults while deciding whether to add a plate.
     */
    private data class ExerciseGoalData(
        val goals: List<FitnessGoal> = emptyList(),
        val maxWeightGoal: StrengthMaxWeightGoal? = null,
        val bodyWeightKg: Double? = null,
        val goalSince: String? = null,
    ) {
        /** The plan clipped to the days the chart is showing — see [maxWeightGoalPlanPoints]. */
        fun planPointsIn(window: List<StrengthSet>): List<MetricPoint> {
            val goal = maxWeightGoal ?: return emptyList()
            val first = window.minOfOrNull { it.epochDay } ?: return emptyList()
            val last = window.maxOfOrNull { it.epochDay } ?: return emptyList()
            return maxWeightGoalPlanPoints(
                goal = goal,
                targetKg = goal.effectiveTargetKg(bodyWeightKg),
                windowStart = first,
                windowEnd = last,
            )?.map { (day, value) -> MetricPoint(day, value) }.orEmpty()
        }
    }

    fun toggleGoalsExpanded() {
        _panels.value = _panels.value.copy(goalsExpanded = !_panels.value.goalsExpanded)
    }

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
