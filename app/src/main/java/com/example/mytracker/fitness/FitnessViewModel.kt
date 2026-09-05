package com.example.mytracker.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.formatSigned
import com.example.mytracker.core.util.label
import com.example.mytracker.fitness.cardio.CardioRepository
import com.example.mytracker.fitness.strength.StrengthExerciseRepository
import com.example.mytracker.fitness.strength.StrengthLogRepository
import com.example.mytracker.fitness.strength.formatTopSets
import com.example.mytracker.fitness.strength.label
import com.example.mytracker.fitness.strength.toDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which list the Fitness screen is showing. Strength is the default — it is the one logged mid-set. */
enum class FitnessTab { STRENGTH, CARDIO }

/** One row of either list: a name plus when it was last done. */
data class FitnessListItem(
    val id: String,
    val name: String,
    val lastTrainedEpochDay: Long?,
    /** Muscle groups / movement direction for strength; empty for cardio. */
    val subtitle: String,
    /** The last session's top set ("70 kg × 5, 5, 5"); null for cardio and untrained exercises. */
    val topSets: String? = null,
)

/**
 * One goal of the current week or month, ready to draw. Built here rather than in the screen because
 * what a number means differs per metric: a Steigerung is signed and read against the period before,
 * a count is read against zero.
 */
data class FitnessGoalProgressRow(
    val id: String,
    val label: String,
    val valueText: String,
    val fraction: Float,
    val isMet: Boolean,
    /** "6 von 8 Wochen erreicht · 3 in Folge", or null while the goal has no finished periods yet. */
    val streakText: String? = null,
)

/**
 * One exercise's long-term max-weight goal on the Fitness screen. [statusText] is the part that
 * makes it worth showing at all: months before the date, "auf Kurs" is the only usable answer.
 */
data class MaxWeightGoalProgressRow(
    val id: String,
    val label: String,
    val valueText: String,
    val statusText: String,
    val fraction: Float,
    val isOnTrack: Boolean,
)

data class FitnessUiState(
    val selectedTab: FitnessTab = FitnessTab.STRENGTH,
    val items: List<FitnessListItem> = emptyList(),
    val goalRows: List<FitnessGoalProgressRow> = emptyList(),
    val maxWeightGoalRows: List<MaxWeightGoalProgressRow> = emptyList(),
)

@HiltViewModel
class FitnessViewModel @Inject constructor(
    private val cardioRepository: CardioRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
    private val strengthLogRepository: StrengthLogRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(FitnessTab.STRENGTH)

    val uiState: StateFlow<FitnessUiState> = combine(
        selectedTab,
        combine(
            strengthExerciseRepository.observeAllWithMuscleGroups(),
            strengthLogRepository.observeLastSessionSetsPerExercise(),
        ) { exercises, lastSessions -> exercises to lastSessions },
        combine(
            cardioRepository.observeActivityTypesAlphabetical(),
            cardioRepository.observeLastSessionDayPerActivityType(),
        ) { types, lastTrained -> types to lastTrained },
        combine(
            fitnessGoalRepository.observeAll(),
            fitnessGoalRepository.observeMaxWeightGoals(),
            fitnessGoalRepository.observeMaxWeightPerExercise(),
            fitnessGoalRepository.observeLatestBodyWeightKg(),
        ) { goals, maxWeightGoals, maxWeightPerExercise, bodyWeightKg ->
            GoalData(goals, maxWeightGoals, maxWeightPerExercise, bodyWeightKg)
        },
        strengthExerciseRepository.observeMuscleGroups(),
    ) { tab, (exercises, lastSessions), (activityTypes, lastCardio), goalData, muscleGroups ->
        val items = when (tab) {
            // Already sorted by `name COLLATE NOCASE` in the DAO.
            FitnessTab.STRENGTH -> exercises.map { item ->
                val lastSession = lastSessions[item.exercise.id].orEmpty()
                FitnessListItem(
                    id = item.exercise.id,
                    name = item.exercise.name,
                    // Every row of a session carries its day, so the day comes free with the sets.
                    lastTrainedEpochDay = lastSession.firstOrNull()?.epochDay,
                    subtitle = item.muscleGroups.joinToString(" · ") { it.name },
                    topSets = formatTopSets(lastSession.map { it.toDraft() }),
                )
            }
            FitnessTab.CARDIO -> activityTypes.map { type ->
                FitnessListItem(
                    id = type.id,
                    name = type.name,
                    lastTrainedEpochDay = lastCardio[type.id],
                    subtitle = "",
                )
            }
        }
        val today = DateUtils.todayEpochDay()
        val muscleGroupNames = muscleGroups.associate { it.id to it.name }
        val exerciseNames = exercises.associate { it.exercise.id to it.exercise.name }

        FitnessUiState(
            selectedTab = tab,
            items = items,
            goalRows = goalData.goals
                .sortedWith(compareBy({ it.metric.ordinal }, { it.period.ordinal }))
                .map { goal ->
                    goal.toProgressRow(
                        progress = fitnessGoalRepository.getProgress(goal, today),
                        muscleGroupNames = muscleGroupNames,
                        exerciseNames = exerciseNames,
                        streak = fitnessGoalRepository.getStreak(goal, today),
                    )
                },
            maxWeightGoalRows = goalData.maxWeightGoals
                .sortedBy { exerciseNames[it.exerciseId].orEmpty() }
                .map { goal ->
                    goal.toProgressRow(
                        exerciseName = exerciseNames[goal.exerciseId] ?: "Übung",
                        currentMaxKg = goalData.maxWeightPerExercise[goal.exerciseId],
                        bodyWeightKg = goalData.bodyWeightKg,
                        today = today,
                    )
                },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FitnessUiState())

    init {
        viewModelScope.launch {
            cardioRepository.ensureDefaultActivityTypesSeeded()
            strengthExerciseRepository.ensureDefaultMuscleGroupsSeeded()
        }
    }

    fun onTabSelected(tab: FitnessTab) {
        selectedTab.value = tab
    }
}

/** The goal-related flows, bundled: combine has no five-argument tuple to hand them over in. */
private data class GoalData(
    val goals: List<FitnessGoal>,
    val maxWeightGoals: List<StrengthMaxWeightGoal>,
    val maxWeightPerExercise: Map<String, Double>,
    val bodyWeightKg: Double?,
)

/** The scope a goal is about, spelled out — "Bankdrücken", "Brust", "Druck" — or nothing. */
private fun FitnessGoal.scopeLabel(
    muscleGroupNames: Map<String, String>,
    exerciseNames: Map<String, String>,
): String? = exerciseId?.let { exerciseNames[it] }
    ?: muscleGroupId?.let { muscleGroupNames[it] }
    ?: movementDirection?.label()

fun FitnessGoal.toProgressRow(
    progress: FitnessGoalProgress,
    muscleGroupNames: Map<String, String>,
    exerciseNames: Map<String, String>,
    streak: FitnessGoalStreak? = null,
): FitnessGoalProgressRow = FitnessGoalProgressRow(
    id = id,
    label = listOfNotNull(metric.label(), scopeLabel(muscleGroupNames, exerciseNames), period.label())
        .joinToString(" · "),
    valueText = progress.valueText(unit()),
    fraction = progress.fraction,
    isMet = progress.isMet,
    streakText = streak?.summaryText(period),
)

/**
 * A streak in words. One period is not a record of anything, so nothing is claimed until there are
 * at least two finished ones to look back over.
 *
 * The best run is only worth saying once it is longer than the one running now — otherwise it would
 * repeat the clause before it. It is deliberately phrased as "beste Serie" rather than "Rekord":
 * [FitnessGoalStreak.bestRun] reaches exactly as far back as the "von n" this sentence opens with,
 * and calling a best-of-eight an all-time record would be a claim the number cannot back.
 */
fun FitnessGoalStreak.summaryText(period: GoalPeriod): String? {
    if (!hasHistory || considered < 2) return null
    val unit = when (period) {
        GoalPeriod.WEEKLY -> "Wochen"
        GoalPeriod.MONTHLY -> "Monaten"
        GoalPeriod.DAILY -> "Tagen"
    }
    return buildString {
        append("$met von $considered $unit erreicht")
        if (currentRun >= 2) append(" · $currentRun in Folge")
        if (bestRun >= 2 && bestRun > currentRun) append(" · beste Serie $bestRun")
    }
}

/**
 * What one goal's standing reads as. A Steigerung carries its sign — "±0 von +5 kg" says "nothing
 * gained over the period before", where a plain "0 von 5" would read as "nothing done yet" — and
 * the one case that is neither met nor missed says so in words rather than as an empty bar.
 */
fun FitnessGoalProgress.valueText(unit: String): String {
    val suffix = if (unit.isBlank()) "" else " $unit"
    return when {
        !hasReference -> "Kein Vergleichszeitraum"
        isIncrease -> "${value.formatSigned()} von ${target.formatSigned()}$suffix"
        else -> "${value.formatCompact()} / ${target.formatCompact()}$suffix"
    }
}

fun StrengthMaxWeightGoal.toProgressRow(
    exerciseName: String,
    currentMaxKg: Double?,
    bodyWeightKg: Double?,
    today: Long,
): MaxWeightGoalProgressRow {
    val progress = maxWeightGoalProgress(this, currentMaxKg, bodyWeightKg, today)
    val dateFormatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN)
    val behind = progress.expectedKg - (currentMaxKg ?: startWeightKg)
    // A relative goal names the multiple as well as the kilos it currently works out to: the
    // multiple is the goal, the kilos are only what it means at today's body weight.
    val targetLabel = targetBodyweightMultiple
        ?.let { "${it.formatCompact()} × KG (${progress.targetKg.formatCompact()} kg)" }
        ?: "${progress.targetKg.formatCompact()} kg"
    return MaxWeightGoalProgressRow(
        id = id,
        label = "$exerciseName · $targetLabel bis " +
            DateUtils.localDateOfEpochDay(targetEpochDay).format(dateFormatter),
        valueText = "Aktuell ${(currentMaxKg ?: startWeightKg).formatCompact()} kg" +
            progress.relativeStrength?.let { " (${it.formatDecimal(2)} × KG)" }.orEmpty() +
            " · Soll heute ${progress.expectedKg.formatCompact()} kg",
        statusText = when {
            progress.isReached -> "Erreicht"
            progress.daysRemaining < 0 -> "Zieldatum überschritten"
            progress.isOnTrack -> "Auf Kurs · noch ${progress.remainingKg.formatCompact()} kg " +
                "in ${progress.daysRemaining} Tagen"
            else -> "${behind.formatCompact()} kg im Rückstand · noch ${progress.daysRemaining} Tage"
        },
        fraction = progress.fraction,
        isOnTrack = progress.isOnTrack,
    )
}
