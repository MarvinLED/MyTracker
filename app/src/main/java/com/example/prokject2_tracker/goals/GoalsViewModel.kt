package com.example.prokject2_tracker.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.NutrientGoalType
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.GoalPeriod
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fitness.FitnessGoal
import com.example.prokject2_tracker.fitness.FitnessGoalMetric
import com.example.prokject2_tracker.fitness.FitnessGoalRepository
import com.example.prokject2_tracker.fitness.strength.MuscleGroup
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseRepository
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.fluid.FluidType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FluidTypeGoalInput(
    val type: FluidType,
    val minText: String,
    val maxText: String,
)

data class FitnessGoalRow(
    val id: String,
    val metric: FitnessGoalMetric,
    val period: GoalPeriod,
    val muscleGroupId: String?,
    val muscleGroupName: String?,
    val targetText: String,
)

/** One nutrient's goal row: the value as typed, plus how that value is meant to be read. */
data class NutrientGoalInput(
    val nutrient: Nutrient,
    val valueText: String,
    val type: NutrientGoalType,
)

data class GoalsUiState(
    val waterGoal: String = "",
    /** One row per [Nutrient], in enum order; a blank value means "no goal". */
    val nutrientGoals: List<NutrientGoalInput> = emptyList(),
    val fluidTypeGoals: List<FluidTypeGoalInput> = emptyList(),
    val fitnessGoals: List<FitnessGoalRow> = emptyList(),
    val availableMuscleGroups: List<MuscleGroup> = emptyList(),
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val fluidRepository: FluidRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved

    init {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferences.first()
            val types = fluidRepository.observeTypes().first()
            val muscleGroups = strengthExerciseRepository.observeMuscleGroups().first()
            val fitnessGoals = fitnessGoalRepository.observeAll().first()
            _state.value = GoalsUiState(
                waterGoal = prefs.dailyWaterGoalMl.toString(),
                nutrientGoals = Nutrient.entries.map { nutrient ->
                    val goal = prefs.nutrientGoals[nutrient]
                    NutrientGoalInput(
                        nutrient = nutrient,
                        valueText = goal?.value?.toString().orEmpty(),
                        type = goal?.type ?: NutrientGoalType.EXACT,
                    )
                },
                fluidTypeGoals = types.map { type ->
                    FluidTypeGoalInput(
                        type = type,
                        minText = type.dailyGoalMinMl?.toString().orEmpty(),
                        maxText = type.dailyGoalMaxMl?.toString().orEmpty(),
                    )
                },
                fitnessGoals = fitnessGoals.map { it.toRow(muscleGroups) },
                availableMuscleGroups = muscleGroups,
            )
        }
    }

    private fun FitnessGoal.toRow(muscleGroups: List<MuscleGroup>) = FitnessGoalRow(
        id = id,
        metric = metric,
        period = period,
        muscleGroupId = muscleGroupId,
        muscleGroupName = muscleGroups.firstOrNull { it.id == muscleGroupId }?.name,
        targetText = targetValue.toString(),
    )

    fun onWaterGoalChange(value: String) { _state.value = _state.value.copy(waterGoal = value) }

    fun onNutrientGoalValueChange(nutrient: Nutrient, value: String) {
        updateNutrient(nutrient) { it.copy(valueText = value) }
    }

    fun onNutrientGoalTypeChange(nutrient: Nutrient, type: NutrientGoalType) {
        updateNutrient(nutrient) { it.copy(type = type) }
    }

    private fun updateNutrient(nutrient: Nutrient, transform: (NutrientGoalInput) -> NutrientGoalInput) {
        _state.value = _state.value.copy(
            nutrientGoals = _state.value.nutrientGoals.map {
                if (it.nutrient == nutrient) transform(it) else it
            },
        )
    }

    fun onFluidTypeMinChange(typeId: String, value: String) {
        _state.value = _state.value.copy(
            fluidTypeGoals = _state.value.fluidTypeGoals.map {
                if (it.type.id == typeId) it.copy(minText = value) else it
            },
        )
    }

    fun onFluidTypeMaxChange(typeId: String, value: String) {
        _state.value = _state.value.copy(
            fluidTypeGoals = _state.value.fluidTypeGoals.map {
                if (it.type.id == typeId) it.copy(maxText = value) else it
            },
        )
    }

    fun onFitnessGoalTargetChange(goalId: String, value: String) {
        _state.value = _state.value.copy(
            fitnessGoals = _state.value.fitnessGoals.map {
                if (it.id == goalId) it.copy(targetText = value) else it
            },
        )
    }

    fun addFitnessGoal(metric: FitnessGoalMetric, period: GoalPeriod, muscleGroupId: String?, targetValue: Double) {
        viewModelScope.launch {
            fitnessGoalRepository.setGoal(metric, period, muscleGroupId, targetValue)
            reloadFitnessGoals()
        }
    }

    fun removeFitnessGoal(goalId: String) {
        viewModelScope.launch {
            fitnessGoalRepository.deleteGoal(goalId)
            reloadFitnessGoals()
        }
    }

    private suspend fun reloadFitnessGoals() {
        val muscleGroups = _state.value.availableMuscleGroups
        val fitnessGoals = fitnessGoalRepository.observeAll().first()
        _state.value = _state.value.copy(fitnessGoals = fitnessGoals.map { it.toRow(muscleGroups) })
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            s.waterGoal.toLocaleDoubleOrNull()?.let { userPreferencesRepository.setDailyWaterGoal(it) }
            s.nutrientGoals.forEach { row ->
                // A blank or unparseable value clears the goal — including its type, so re-entering
                // a value later doesn't silently inherit a "höchstens" from a goal long deleted.
                val value = row.valueText.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
                userPreferencesRepository.setNutrientGoal(
                    nutrient = row.nutrient,
                    goal = value?.let { NutrientGoal(value = it, type = row.type) },
                )
            }
            s.fluidTypeGoals.forEach { row ->
                fluidRepository.updateTypeGoals(
                    row.type,
                    dailyGoalMinMl = row.minText.toLocaleDoubleOrNull(),
                    dailyGoalMaxMl = row.maxText.toLocaleDoubleOrNull(),
                )
            }
            s.fitnessGoals.forEach { row ->
                row.targetText.toLocaleDoubleOrNull()?.let {
                    fitnessGoalRepository.setGoal(row.metric, row.period, row.muscleGroupId, it)
                }
            }
            _saved.emit(Unit)
        }
    }
}
