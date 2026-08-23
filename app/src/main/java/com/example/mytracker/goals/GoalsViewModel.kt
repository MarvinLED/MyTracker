package com.example.mytracker.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.datastore.UserPreferencesRepository
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.label
import com.example.mytracker.core.util.minutesAsHours
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalMetric
import com.example.mytracker.fitness.FitnessGoalRepository
import com.example.mytracker.fitness.isIncrease
import com.example.mytracker.fitness.unit
import com.example.mytracker.fitness.strength.MovementDirection
import com.example.mytracker.fitness.strength.MuscleGroup
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fitness.strength.label
import com.example.mytracker.fitness.strength.StrengthExerciseRepository
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.fluid.FluidType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
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

/**
 * One goal that *can* be set, whether or not it is — the screen lists them all, so an empty pair of
 * fields is a row too. [weeklyText] and [monthlyText] sit side by side because the same goal is
 * asked for in both periods; blank means "kein Ziel", which on save deletes the row rather than
 * storing a zero.
 */
data class FitnessGoalRow(
    /** Stable per row, not per stored goal: the row exists before either period has a target. */
    val key: String,
    val metric: FitnessGoalMetric,
    val label: String,
    /** Label including the section, for the change log — a history row has no section around it. */
    val logLabel: String,
    val unit: String,
    val muscleGroupId: String? = null,
    val movementDirection: MovementDirection? = null,
    val exerciseId: String? = null,
    val weeklyText: String = "",
    val monthlyText: String = "",
    /** Increase goals only: whether the two targets are read as percent instead of kilos. */
    val isPercent: Boolean = false,
) {
    val isIncrease: Boolean get() = metric.isIncrease
}

/** The rows grouped the way the screen shows them, so a long list stays navigable. */
data class FitnessGoalSection(val title: String, val rows: List<FitnessGoalRow>)

/**
 * One exercise's long-term max-weight goal as typed. [targetEpochDay] null means no date has been
 * picked yet — and without a date there is no goal to save, since the date is what makes it a plan
 * rather than a wish.
 */
data class MaxWeightGoalRow(
    val exerciseId: String,
    val exerciseName: String,
    val targetText: String = "",
    val targetEpochDay: Long? = null,
    /** The exercise's all-time top set, shown beside the field so the target has something to beat. */
    val currentMaxKg: Double? = null,
    /** Where the plan started, once one is on file — see [com.example.mytracker.fitness.StrengthMaxWeightGoal]. */
    val startWeightKg: Double? = null,
    /** True when [targetText] is a multiple of body weight rather than a weight in kilos. */
    val isRelative: Boolean = false,
)

/**
 * One entry of the Zieländerungs-Historie, ready to read: which goal, when, and what it went from
 * and to. The "von" is the row before it for the same goal — the log stores states, and what anyone
 * wants to see is the step between two of them.
 */
data class GoalChangeRow(
    val id: String,
    val label: String,
    val dateText: String,
    val changeText: String,
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
    val fitnessGoalSections: List<FitnessGoalSection> = emptyList(),
    val maxWeightGoals: List<MaxWeightGoalRow> = emptyList(),
    /** The latest logged body weight — what a relative target is multiplied by. Null until one exists. */
    val bodyWeightKg: Double? = null,
    /** The Fitness-Zieländerungen, newest first — see [GoalChangeRow]. */
    val goalChanges: List<GoalChangeRow> = emptyList(),
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
            val exercises = strengthExerciseRepository.observeAll().first()
            val fitnessGoals = fitnessGoalRepository.observeAll().first()
            val maxWeightGoals = fitnessGoalRepository.observeMaxWeightGoals().first()
            val maxWeightByExercise = fitnessGoalRepository.observeMaxWeightPerExercise().first()
            val bodyWeightKg = fitnessGoalRepository.observeLatestBodyWeightKg().first()
            val goalChanges = fitnessGoalRepository.observeGoalChanges().first()
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
                fitnessGoalSections = fitnessGoalSections(muscleGroups, exercises, fitnessGoals),
                maxWeightGoals = exercises.map { exercise ->
                    val goal = maxWeightGoals.firstOrNull { it.exerciseId == exercise.id }
                    MaxWeightGoalRow(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        // A relative goal is typed as the multiple it is, not as the kilos it
                        // happens to work out to today — those move with the body weight.
                        targetText = (goal?.targetBodyweightMultiple ?: goal?.targetWeightKg)
                            ?.formatDecimal(2).orEmpty(),
                        targetEpochDay = goal?.targetEpochDay,
                        currentMaxKg = maxWeightByExercise[exercise.id],
                        startWeightKg = goal?.startWeightKg,
                        isRelative = goal?.targetBodyweightMultiple != null,
                    )
                },
                bodyWeightKg = bodyWeightKg,
                goalChanges = goalChangeRows(goalChanges),
                sleepDurationMinHours = prefs.sleepDurationGoalMinutes?.min
                    ?.let { it.toInt().minutesAsHours().formatGoalHours() }.orEmpty(),
                sleepDurationMaxHours = prefs.sleepDurationGoalMinutes?.max
                    ?.let { it.toInt().minutesAsHours().formatGoalHours() }.orEmpty(),
                bedtimeGoalMinuteOfDay = prefs.bedtimeGoalMinuteOfDay,
            )
        }
    }

    /**
     * Every goal the app can hold, in sections — not only the ones already set. A goal that has to
     * be conjured out of a dropdown before it can be typed is a goal most people never find; a list
     * of empty fields says what is on offer and takes the target in one gesture.
     */
    private fun fitnessGoalSections(
        muscleGroups: List<MuscleGroup>,
        exercises: List<StrengthExercise>,
        goals: List<FitnessGoal>,
    ): List<FitnessGoalSection> {
        fun stored(
            metric: FitnessGoalMetric,
            period: GoalPeriod,
            muscleGroupId: String? = null,
            movementDirection: MovementDirection? = null,
            exerciseId: String? = null,
        ): FitnessGoal? = goals.firstOrNull {
            it.metric == metric && it.period == period && it.muscleGroupId == muscleGroupId &&
                it.movementDirection == movementDirection && it.exerciseId == exerciseId
        }

        fun row(
            key: String,
            metric: FitnessGoalMetric,
            label: String,
            sectionTitle: String,
            muscleGroupId: String? = null,
            movementDirection: MovementDirection? = null,
            exerciseId: String? = null,
        ): FitnessGoalRow {
            val weekly = stored(metric, GoalPeriod.WEEKLY, muscleGroupId, movementDirection, exerciseId)
            val monthly = stored(metric, GoalPeriod.MONTHLY, muscleGroupId, movementDirection, exerciseId)
            return FitnessGoalRow(
                key = key,
                metric = metric,
                label = label,
                logLabel = "$sectionTitle · $label",
                unit = metric.unit(),
                muscleGroupId = muscleGroupId,
                movementDirection = movementDirection,
                exerciseId = exerciseId,
                weeklyText = weekly?.targetValue?.formatDecimal(2).orEmpty(),
                monthlyText = monthly?.targetValue?.formatDecimal(2).orEmpty(),
                // Both periods of one row share the mode: "5 % pro Woche, aber 2000 kg pro Monat"
                // is two different questions in one row, and the row has one pair of chips.
                isPercent = weekly?.isPercent ?: monthly?.isPercent ?: false,
            )
        }

        return buildList {
            add(
                FitnessGoalSection(
                    "Cardio",
                    listOf(
                        row("cardio-sessions", FitnessGoalMetric.CARDIO_SESSIONS, "Einheiten", "Cardio"),
                        row("cardio-duration", FitnessGoalMetric.CARDIO_DURATION_MINUTES, "Dauer", "Cardio"),
                    ),
                ),
            )
            add(
                FitnessGoalSection(
                    "Kraft gesamt",
                    listOf(
                        row(
                            "strength-sets",
                            FitnessGoalMetric.STRENGTH_SETS_TOTAL,
                            "Sätze gesamt",
                            "Kraft gesamt",
                        ),
                    ),
                ),
            )
            if (muscleGroups.isNotEmpty()) {
                add(
                    FitnessGoalSection(
                        "Muskelgruppen",
                        muscleGroups.flatMap { group ->
                            listOf(
                                row(
                                    key = "muscle-sets-${group.id}",
                                    metric = FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP,
                                    label = "${group.name} · Sätze",
                                    sectionTitle = "Muskelgruppen",
                                    muscleGroupId = group.id,
                                ),
                                // Volume and not a top set: the heaviest thing done for "Rücken" is
                                // whichever exercise uses the biggest numbers, which says nothing
                                // about the muscle group. Volume adds up across exercises.
                                row(
                                    key = "muscle-volume-${group.id}",
                                    metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP,
                                    label = "${group.name} · Volumen-Steigerung",
                                    sectionTitle = "Muskelgruppen",
                                    muscleGroupId = group.id,
                                ),
                            )
                        },
                    ),
                )
            }
            add(
                FitnessGoalSection(
                    "Bewegungsrichtungen",
                    MovementDirection.entries.flatMap { direction ->
                        listOf(
                            row(
                                key = "direction-sets-${direction.name}",
                                metric = FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION,
                                label = "${direction.label()} · Sätze",
                                sectionTitle = "Bewegungsrichtungen",
                                movementDirection = direction,
                            ),
                            row(
                                key = "direction-volume-${direction.name}",
                                metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MOVEMENT_DIRECTION,
                                label = "${direction.label()} · Volumen-Steigerung",
                                sectionTitle = "Bewegungsrichtungen",
                                movementDirection = direction,
                            ),
                        )
                    },
                ),
            )
            exercises.forEach { exercise ->
                add(
                    FitnessGoalSection(
                        exercise.name,
                        listOf(
                            row(
                                key = "maxweight-increase-${exercise.id}",
                                metric = FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE,
                                label = "Steigerung Maximalgewicht",
                                sectionTitle = exercise.name,
                                exerciseId = exercise.id,
                            ),
                            row(
                                key = "volume-increase-${exercise.id}",
                                metric = FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
                                label = "Steigerung Gesamtvolumen",
                                sectionTitle = exercise.name,
                                exerciseId = exercise.id,
                            ),
                        ),
                    ),
                )
            }
        }
    }

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

    fun onFitnessGoalTargetChange(rowKey: String, period: GoalPeriod, value: String) {
        _state.value = _state.value.copy(
            fitnessGoalSections = _state.value.fitnessGoalSections.map { section ->
                section.copy(
                    rows = section.rows.map { row ->
                        when {
                            row.key != rowKey -> row
                            period == GoalPeriod.MONTHLY -> row.copy(monthlyText = value)
                            else -> row.copy(weeklyText = value)
                        }
                    },
                )
            },
        )
    }

    /** kg or %, for both periods of the row at once — see [FitnessGoalRow.isPercent]. */
    fun onFitnessGoalPercentChange(rowKey: String, isPercent: Boolean) {
        _state.value = _state.value.copy(
            fitnessGoalSections = _state.value.fitnessGoalSections.map { section ->
                section.copy(
                    rows = section.rows.map { row ->
                        if (row.key == rowKey) row.copy(isPercent = isPercent) else row
                    },
                )
            },
        )
    }

    /** Switches the long-term target between kilos and a multiple of body weight. */
    fun onMaxWeightGoalRelativeChange(exerciseId: String, isRelative: Boolean) {
        updateMaxWeightGoal(exerciseId) { it.copy(isRelative = isRelative) }
    }

    fun onMaxWeightGoalTargetChange(exerciseId: String, value: String) {
        updateMaxWeightGoal(exerciseId) { it.copy(targetText = value) }
    }

    /** Null clears the date, which is how a long-term goal is taken back off an exercise. */
    fun onMaxWeightGoalDateChange(exerciseId: String, epochDay: Long?) {
        updateMaxWeightGoal(exerciseId) { it.copy(targetEpochDay = epochDay) }
    }

    private fun updateMaxWeightGoal(exerciseId: String, transform: (MaxWeightGoalRow) -> MaxWeightGoalRow) {
        _state.value = _state.value.copy(
            maxWeightGoals = _state.value.maxWeightGoals.map {
                if (it.exerciseId == exerciseId) transform(it) else it
            },
        )
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
            // Every row is written on every save, in both periods: with the whole list on screen, an
            // emptied field is an instruction to drop that goal, and only writing the filled ones
            // would make deleting one impossible.
            s.fitnessGoalSections.flatMap { it.rows }.forEach { row ->
                listOf(GoalPeriod.WEEKLY to row.weeklyText, GoalPeriod.MONTHLY to row.monthlyText)
                    .forEach { (period, text) ->
                        val target = text.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
                        val label = "${row.logLabel} · ${period.label()}"
                        if (target == null) {
                            fitnessGoalRepository.clearGoal(
                                metric = row.metric,
                                period = period,
                                muscleGroupId = row.muscleGroupId,
                                movementDirection = row.movementDirection,
                                exerciseId = row.exerciseId,
                                label = label,
                            )
                        } else {
                            fitnessGoalRepository.setGoal(
                                metric = row.metric,
                                period = period,
                                muscleGroupId = row.muscleGroupId,
                                movementDirection = row.movementDirection,
                                targetValue = target,
                                exerciseId = row.exerciseId,
                                isPercent = row.isIncrease && row.isPercent,
                                label = label,
                            )
                        }
                    }
            }
            // A long-term goal needs both halves: a weight without a date is not a plan, and a date
            // without a weight is not a goal. Either missing takes the goal off the exercise.
            s.maxWeightGoals.forEach { row ->
                val target = row.targetText.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
                val date = row.targetEpochDay
                val label = "${row.exerciseName} · Langfristiges Maximalgewicht"
                if (target != null && date != null) {
                    // A relative target still stores the kilos it currently means, so the goal reads
                    // as a number even before the next weigh-in — but the multiple is what governs.
                    val multiple = target.takeIf { row.isRelative }
                    fitnessGoalRepository.setMaxWeightGoal(
                        exerciseId = row.exerciseId,
                        targetWeightKg = if (multiple != null) target * (s.bodyWeightKg ?: 0.0) else target,
                        targetEpochDay = date,
                        targetBodyweightMultiple = multiple,
                        label = label,
                    )
                } else {
                    fitnessGoalRepository.clearMaxWeightGoal(row.exerciseId, label = label)
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
            // Re-read: the history is the one part of this screen that the save itself writes to.
            _state.value = _state.value.copy(
                goalChanges = goalChangeRows(fitnessGoalRepository.observeGoalChanges().first()),
            )
            _saved.emit(Unit)
        }
    }
}

/** Hours as typed to whole minutes; blank, unparseable or non-positive means "kein Ziel". */
private fun String.toGoalMinutes(): Double? =
    toLocaleDoubleOrNull()?.takeIf { it > 0.0 }?.let { (it * 60).toInt().toDouble() }

/** "7" / "7,5" — the goal read back into the field it was typed in. */
private fun Double.formatGoalHours(): String = formatDecimal(2)
