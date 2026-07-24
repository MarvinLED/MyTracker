package com.example.prokject2_tracker.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class GoalsUiState(
    val calorieGoal: String = "",
    val waterGoal: String = "",
    val proteinGoal: String = "",
    val carbsGoal: String = "",
    val fatGoal: String = "",
    val saturatedFatGoal: String = "",
    val sugarGoal: String = "",
    val fiberGoal: String = "",
    val saltGoal: String = "",
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
                calorieGoal = prefs.dailyCalorieGoalKcal.toString(),
                waterGoal = prefs.dailyWaterGoalMl.toString(),
                proteinGoal = prefs.dailyProteinGoalG?.toString().orEmpty(),
                carbsGoal = prefs.dailyCarbsGoalG?.toString().orEmpty(),
                fatGoal = prefs.dailyFatGoalG?.toString().orEmpty(),
                saturatedFatGoal = prefs.dailySaturatedFatGoalG?.toString().orEmpty(),
                sugarGoal = prefs.dailySugarGoalG?.toString().orEmpty(),
                fiberGoal = prefs.dailyFiberGoalG?.toString().orEmpty(),
                saltGoal = prefs.dailySaltGoalG?.toString().orEmpty(),
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

    fun onCalorieGoalChange(value: String) { _state.value = _state.value.copy(calorieGoal = value) }
    fun onWaterGoalChange(value: String) { _state.value = _state.value.copy(waterGoal = value) }
    fun onProteinGoalChange(value: String) { _state.value = _state.value.copy(proteinGoal = value) }
    fun onCarbsGoalChange(value: String) { _state.value = _state.value.copy(carbsGoal = value) }
    fun onFatGoalChange(value: String) { _state.value = _state.value.copy(fatGoal = value) }
    fun onSaturatedFatGoalChange(value: String) { _state.value = _state.value.copy(saturatedFatGoal = value) }
    fun onSugarGoalChange(value: String) { _state.value = _state.value.copy(sugarGoal = value) }
    fun onFiberGoalChange(value: String) { _state.value = _state.value.copy(fiberGoal = value) }
    fun onSaltGoalChange(value: String) { _state.value = _state.value.copy(saltGoal = value) }

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
            s.calorieGoal.toLocaleDoubleOrNull()?.let { userPreferencesRepository.setDailyCalorieGoal(it) }
            s.waterGoal.toLocaleDoubleOrNull()?.let { userPreferencesRepository.setDailyWaterGoal(it) }
            userPreferencesRepository.setDailyProteinGoal(s.proteinGoal.toLocaleDoubleOrNull())
            userPreferencesRepository.setDailyCarbsGoal(s.carbsGoal.toLocaleDoubleOrNull())
            userPreferencesRepository.setDailyFatGoal(s.fatGoal.toLocaleDoubleOrNull())
            userPreferencesRepository.setDailySaturatedFatGoal(s.saturatedFatGoal.toLocaleDoubleOrNull())
            userPreferencesRepository.setDailySugarGoal(s.sugarGoal.toLocaleDoubleOrNull())
            userPreferencesRepository.setDailyFiberGoal(s.fiberGoal.toLocaleDoubleOrNull())
            userPreferencesRepository.setDailySaltGoal(s.saltGoal.toLocaleDoubleOrNull())
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
