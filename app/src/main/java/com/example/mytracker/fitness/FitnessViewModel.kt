package com.example.mytracker.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.fitness.cardio.CardioRepository
import com.example.mytracker.fitness.strength.StrengthExerciseRepository
import com.example.mytracker.fitness.strength.StrengthLogRepository
import com.example.mytracker.fitness.strength.formatTopSets
import com.example.mytracker.fitness.strength.toDraft
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class FitnessUiState(
    val selectedTab: FitnessTab = FitnessTab.STRENGTH,
    val items: List<FitnessListItem> = emptyList(),
    val goals: List<FitnessGoal> = emptyList(),
    val progressByGoalId: Map<String, Double> = emptyMap(),
    /** Names for the muscle groups goals are scoped to, so same-metric goals stay distinguishable. */
    val muscleGroupNamesById: Map<String, String> = emptyMap(),
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
        fitnessGoalRepository.observeAll(),
        strengthExerciseRepository.observeMuscleGroups(),
    ) { tab, (exercises, lastSessions), (activityTypes, lastCardio), goals, muscleGroups ->
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
        FitnessUiState(
            selectedTab = tab,
            items = items,
            goals = goals,
            progressByGoalId = goals.associate { it.id to fitnessGoalRepository.getPeriodProgress(it, today) },
            muscleGroupNamesById = muscleGroups.associate { it.id to it.name },
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
