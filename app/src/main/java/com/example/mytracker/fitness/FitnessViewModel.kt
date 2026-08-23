package com.example.mytracker.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
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
        ) { goals, maxWeightGoals, maxWeightPerExercise ->
            Triple(goals, maxWeightGoals, maxWeightPerExercise)
        },
        strengthExerciseRepository.observeMuscleGroups(),
    ) { tab, (exercises, lastSessions), (activityTypes, lastCardio), (goals, maxWeightGoals, maxWeightPerExercise), muscleGroups ->
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
            goalRows = goals
                .sortedWith(compareBy({ it.metric.ordinal }, { it.period.ordinal }))
                .map { goal ->
                    goal.toProgressRow(
                        progress = fitnessGoalRepository.getPeriodProgress(goal, today),
                        muscleGroupNames = muscleGroupNames,
                        exerciseNames = exerciseNames,
                    )
                },
            maxWeightGoalRows = maxWeightGoals
                .sortedBy { exerciseNames[it.exerciseId].orEmpty() }
                .map { goal ->
                    goal.toProgressRow(
                        exerciseName = exerciseNames[goal.exerciseId] ?: "Übung",
                        currentMaxKg = maxWeightPerExercise[goal.exerciseId],
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

/** The scope a goal is about, spelled out — "Bankdrücken", "Brust", "Druck" — or nothing. */
private fun FitnessGoal.scopeLabel(
    muscleGroupNames: Map<String, String>,
    exerciseNames: Map<String, String>,
): String? = exerciseId?.let { exerciseNames[it] }
    ?: muscleGroupId?.let { muscleGroupNames[it] }
    ?: movementDirection?.label()

private fun FitnessGoal.toProgressRow(
    progress: Double,
    muscleGroupNames: Map<String, String>,
    exerciseNames: Map<String, String>,
): FitnessGoalProgressRow {
    val scope = scopeLabel(muscleGroupNames, exerciseNames)
    val unit = metric.unit().let { if (it.isBlank()) "" else " $it" }
    return FitnessGoalProgressRow(
        id = id,
        label = listOfNotNull(metric.label(), scope, period.label()).joinToString(" · "),
        // Signed for the Steigerungen: "0 von 5 kg" would read as "nothing trained yet", while
        // "±0 von +5 kg" says what actually happened — trained, but no heavier than before.
        valueText = if (metric.isIncrease) {
            "${progress.formatSigned()} von ${targetValue.formatSigned()}$unit"
        } else {
            "${progress.formatCompact()} / ${targetValue.formatCompact()}$unit"
        },
        fraction = if (targetValue > 0) (progress / targetValue).toFloat().coerceIn(0f, 1f) else 0f,
        isMet = targetValue > 0 && progress >= targetValue,
    )
}

private fun StrengthMaxWeightGoal.toProgressRow(
    exerciseName: String,
    currentMaxKg: Double?,
    today: Long,
): MaxWeightGoalProgressRow {
    val progress = maxWeightGoalProgress(this, currentMaxKg, today)
    val dateFormatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN)
    val behind = progress.expectedKg - (currentMaxKg ?: startWeightKg)
    return MaxWeightGoalProgressRow(
        id = id,
        label = "$exerciseName · ${targetWeightKg.formatCompact()} kg bis " +
            DateUtils.localDateOfEpochDay(targetEpochDay).format(dateFormatter),
        valueText = "Aktuell ${(currentMaxKg ?: startWeightKg).formatCompact()} kg · " +
            "Soll heute ${progress.expectedKg.formatCompact()} kg",
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

/** "+2,5" / "±0" / "-1" — a gain has to carry its sign to be read as one. */
private fun Double.formatSigned(): String = when {
    this > 0 -> "+${formatCompact()}"
    this == 0.0 -> "±0"
    else -> formatCompact()
}
