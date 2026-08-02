package com.example.prokject2_tracker.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.GoalPeriod
import com.example.prokject2_tracker.fitness.FitnessGoalRepository
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseRepository
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.habit.HabitRepository
import com.example.prokject2_tracker.nutrition.diary.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Today's goals across all areas, each with how far along it is. Read-only: the targets themselves
 * are set on the Ziele screen, and this one answers "where do I stand on them right now".
 *
 * Only goals whose period *is* the day appear — a weekly set count is not a thing today can be
 * measured against, so it is left to the screens that own it.
 */
@HiltViewModel
class DayGoalsViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    diaryRepository: DiaryRepository,
    fluidRepository: FluidRepository,
    habitRepository: HabitRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
    strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    // Fixed at construction like the Habits screen: this is "today", and a screen that silently
    // rolled over at midnight would change what it says under the user's hands.
    private val today = DateUtils.todayEpochDay()

    private val nutritionSection = combine(
        userPreferencesRepository.userPreferences,
        diaryRepository.observeDayNutritionTotals(today),
    ) { prefs, totals ->
        nutrientGoalRows(prefs.nutrientGoals, totals.byNutrient()).asSection("Ernährung")
    }

    private val fluidSection = combine(
        userPreferencesRepository.userPreferences,
        fluidRepository.observeForDay(today),
        fluidRepository.observeTypes(),
    ) { prefs, entries, types ->
        fluidGoalRows(
            dailyGoalMl = prefs.dailyWaterGoalMl,
            totalMl = entries.sumOf { it.amountMl },
            types = types,
            totalsByTypeId = entries.groupBy { it.fluidTypeId }
                .mapValues { (_, sameType) -> sameType.sumOf { it.amountMl } },
        ).asSection("Flüssigkeit")
    }

    private val habitSection = combine(
        habitRepository.observeActive(),
        habitRepository.observeCheckInsForDay(today),
        habitRepository.observeGoalsByHabitId(),
    ) { habits, checkIns, goalsByHabitId ->
        habitGoalRows(
            habits = habits,
            dailyGoalsByHabitId = goalsByHabitId.mapNotNull { (habitId, goals) ->
                goals.firstOrNull { it.period == GoalPeriod.DAILY }?.let { habitId to it }
            }.toMap(),
            checkedInHabitIds = checkIns.map { it.habitId }.toSet(),
            valuesByHabitId = checkIns.mapNotNull { checkIn ->
                checkIn.value?.let { checkIn.habitId to it }
            }.toMap(),
        ).asSection("Habits")
    }

    private val fitnessSection = combine(
        fitnessGoalRepository.observeAll(),
        strengthExerciseRepository.observeMuscleGroups(),
    ) { goals, muscleGroups ->
        val daily = goals.filter { it.period == GoalPeriod.DAILY }
        fitnessGoalRows(
            goals = daily,
            progressByGoalId = daily.associate { it.id to fitnessGoalRepository.getPeriodProgress(it, today) },
            muscleGroupNames = muscleGroups.associate { it.id to it.name },
        ).asSection("Fitness")
    }

    val uiState: StateFlow<DayGoalsUiState> = combine(
        nutritionSection,
        fluidSection,
        habitSection,
        fitnessSection,
    ) { nutrition, fluid, habits, fitness ->
        DayGoalsUiState(sections = listOfNotNull(nutrition, fluid, habits, fitness))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayGoalsUiState())
}

/** An area with no goal set contributes no heading either. */
private fun List<DayGoalRow>.asSection(title: String): DayGoalSection? =
    if (isEmpty()) null else DayGoalSection(title, this)
