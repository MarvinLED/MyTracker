package com.example.mytracker.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.datastore.UserPreferencesRepository
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.FitnessGoalRepository
import com.example.mytracker.fitness.strength.StrengthExerciseRepository
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.habit.HabitRepository
import com.example.mytracker.nutrition.diary.DiaryRepository
import com.example.mytracker.sleep.SleepRepository
import com.example.mytracker.task.TaskRepository
import com.example.mytracker.task.taskStatuses
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
    sleepRepository: SleepRepository,
    taskRepository: TaskRepository,
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

    // The night that ended *this morning* — that is the sleep today is running on.
    private val sleepSection = combine(
        userPreferencesRepository.userPreferences,
        sleepRepository.observeForDay(today),
    ) { prefs, night ->
        sleepGoalRows(
            entry = night,
            durationGoalMinutes = prefs.sleepDurationGoalMinutes,
            bedtimeGoalMinuteOfDay = prefs.bedtimeGoalMinuteOfDay,
        ).asSection("Schlaf")
    }

    // Unlike the rest of this screen, a task is not a target that the day's logging is measured
    // against — it is something to go and do. That is why it leads: it is the only section anyone
    // can act on directly.
    private val taskSection = combine(
        taskRepository.observeActive(),
        taskRepository.observeCompletions(),
    ) { tasks, completions ->
        taskRows(taskStatuses(tasks, completions, today)).asSection("Aufgaben")
    }

    // Past the five-flow `combine` overloads, so this is the vararg one: every section is the same
    // nullable type, and the nulls are the areas with nothing set.
    val uiState: StateFlow<DayGoalsUiState> = combine(
        taskSection,
        nutritionSection,
        fluidSection,
        habitSection,
        fitnessSection,
        sleepSection,
    ) { sections -> DayGoalsUiState(sections = sections.filterNotNull()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayGoalsUiState())
}

/** An area with no goal set contributes no heading either. */
private fun List<DayGoalRow>.asSection(title: String): DayGoalSection? =
    if (isEmpty()) null else DayGoalSection(title, this)
