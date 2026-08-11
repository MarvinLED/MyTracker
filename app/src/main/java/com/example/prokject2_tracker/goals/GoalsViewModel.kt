package com.example.prokject2_tracker.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.GoalPeriod
import com.example.prokject2_tracker.core.util.minutesAsHours
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fitness.FitnessGoal
import com.example.prokject2_tracker.fitness.FitnessGoalMetric
import com.example.prokject2_tracker.fitness.FitnessGoalRepository
import com.example.prokject2_tracker.fitness.strength.MovementDirection
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
    val movementDirection: MovementDirection?,
    val targetText: String,
)

/** One nutrient's goal row: the two bounds as typed. Either may be blank, or both. */
data class NutrientGoalInput(
    val nutrient: Nutrient,
    val minText: String,
    val maxText: String,
)

data class GoalsUiState(
    val waterGoal: String = "",
    /** One row per [Nutrient], in enum order; a blank value means "no goal". */
    val nutrientGoals: List<NutrientGoalInput> = emptyList(),
    val fluidTypeGoals: List<FluidTypeGoalInput> = emptyList(),
    val fitnessGoals: List<FitnessGoalRow> = emptyList(),
    val availableMuscleGroups: List<MuscleGroup> = emptyList(),
    /** Sleep length in **hours** as typed ("7,5"); the repository stores minutes. */
    val sleepDurationMinHours: String = "",
    val sleepDurationMaxHours: String = "",
    /** Minutes since midnight, or null for "kein Ziel" — the field is a clock, not a number. */
    val bedtimeGoalMinuteOfDay: Int? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val nutrientGoalHistoryRepository: NutrientGoalHistoryRepository,
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
                        minText = goal?.min?.toString().orEmpty(),
                        maxText = goal?.max?.toString().orEmpty(),
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
                sleepDurationMinHours = prefs.sleepDurationGoalMinutes?.min
                    ?.let { it.toInt().minutesAsHours().formatGoalHours() }.orEmpty(),
                sleepDurationMaxHours = prefs.sleepDurationGoalMinutes?.max
                    ?.let { it.toInt().minutesAsHours().formatGoalHours() }.orEmpty(),
                bedtimeGoalMinuteOfDay = prefs.bedtimeGoalMinuteOfDay,
            )
        }
    }

    private fun FitnessGoal.toRow(muscleGroups: List<MuscleGroup>) = FitnessGoalRow(
        id = id,
        metric = metric,
        period = period,
        muscleGroupId = muscleGroupId,
        muscleGroupName = muscleGroups.firstOrNull { it.id == muscleGroupId }?.name,
        movementDirection = movementDirection,
        targetText = targetValue.toString(),
    )

    fun onWaterGoalChange(value: String) { _state.value = _state.value.copy(waterGoal = value) }

    fun onSleepDurationMinChange(value: String) { _state.value = _state.value.copy(sleepDurationMinHours = value) }

    fun onSleepDurationMaxChange(value: String) { _state.value = _state.value.copy(sleepDurationMaxHours = value) }

    /** Null clears the bedtime goal — the screen offers that next to the picker. */
    fun onBedtimeGoalChange(minuteOfDay: Int?) { _state.value = _state.value.copy(bedtimeGoalMinuteOfDay = minuteOfDay) }

    fun onNutrientGoalMinChange(nutrient: Nutrient, value: String) {
        updateNutrient(nutrient) { it.copy(minText = value) }
    }

    fun onNutrientGoalMaxChange(nutrient: Nutrient, value: String) {
        updateNutrient(nutrient) { it.copy(maxText = value) }
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

    fun addFitnessGoal(
        metric: FitnessGoalMetric,
        period: GoalPeriod,
        muscleGroupId: String?,
        movementDirection: MovementDirection?,
        targetValue: Double,
    ) {
        viewModelScope.launch {
            fitnessGoalRepository.setGoal(metric, period, muscleGroupId, movementDirection, targetValue)
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
            // Read once, before anything is written: the history needs the value each goal is
            // moving away from, and it is about to be overwritten.
            val previousGoals = userPreferencesRepository.userPreferences.first().nutrientGoals
            s.nutrientGoals.forEach { row ->
                // Blank or unparseable clears that bound; both blank clears the goal outright.
                val goal = NutrientGoal(
                    min = row.minText.toLocaleDoubleOrNull()?.takeIf { it > 0.0 },
                    max = row.maxText.toLocaleDoubleOrNull()?.takeIf { it > 0.0 },
                )
                // Goes through the history repository rather than straight to DataStore, so a
                // changed target is written down as well as applied — see its KDoc.
                nutrientGoalHistoryRepository.setGoal(
                    nutrient = row.nutrient,
                    oldGoal = previousGoals[row.nutrient],
                    newGoal = goal.takeUnless { it.isEmpty },
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
                    fitnessGoalRepository.setGoal(row.metric, row.period, row.muscleGroupId, row.movementDirection, it)
                }
            }
            // Typed in hours, stored in minutes: comparing a night to its goal is minute arithmetic,
            // and rounding a 7,5 h goal to hours on the way in would lose the half.
            userPreferencesRepository.setSleepDurationGoal(
                NutrientGoal(
                    min = s.sleepDurationMinHours.toGoalMinutes(),
                    max = s.sleepDurationMaxHours.toGoalMinutes(),
                ),
            )
            userPreferencesRepository.setBedtimeGoal(s.bedtimeGoalMinuteOfDay)
            _saved.emit(Unit)
        }
    }
}

/** Hours as typed to whole minutes; blank, unparseable or non-positive means "kein Ziel". */
private fun String.toGoalMinutes(): Double? =
    toLocaleDoubleOrNull()?.takeIf { it > 0.0 }?.let { (it * 60).toInt().toDouble() }

/** "7" / "7,5" — the goal read back into the field it was typed in. */
private fun Double.formatGoalHours(): String = formatDecimal(2)
