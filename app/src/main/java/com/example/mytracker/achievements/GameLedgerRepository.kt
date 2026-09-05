package com.example.mytracker.achievements

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.datastore.UserPreferencesRepository
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalRepository
import com.example.mytracker.fitness.periodEndDay
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.goals.DayGoalRow
import com.example.mytracker.goals.NutrientGoalChange
import com.example.mytracker.goals.NutrientGoalChangeDao
import com.example.mytracker.goals.fluidGoalRows
import com.example.mytracker.goals.habitGoalRows
import com.example.mytracker.goals.nutrientGoalOn
import com.example.mytracker.goals.nutrientGoalRows
import com.example.mytracker.goals.sleepGoalRows
import com.example.mytracker.goals.taskRows
import com.example.mytracker.habit.Habit
import com.example.mytracker.habit.HabitCheckIn
import com.example.mytracker.habit.HabitCheckInDao
import com.example.mytracker.habit.HabitGoal
import com.example.mytracker.habit.HabitGoalDao
import com.example.mytracker.habit.HabitRepository
import com.example.mytracker.nutrition.NutritionTotals
import com.example.mytracker.nutrition.diary.DiaryDao
import com.example.mytracker.sleep.SleepEntry
import com.example.mytracker.sleep.SleepRepository
import com.example.mytracker.task.Task
import com.example.mytracker.task.TaskCompletion
import com.example.mytracker.task.TaskRepository
import com.example.mytracker.task.taskStatuses
import com.example.mytracker.weight.BodyWeightDao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * The furthest back a first run will reach. A year of history is plenty to arrive at a figure that
 * reflects the person, and it bounds the work of the one run that has everything to do.
 */
private const val MAX_BACKFILL_DAYS = 365L

/**
 * Settles days into the points ledger, and hands the ledger back out.
 *
 * The one rule that shapes everything here: **today is never booked**. A day still running is not a
 * day whose goals were missed, exactly as an unfinished week is left out of a streak. Booking it
 * would hand out points at breakfast and then have to take them back.
 */
@Singleton
class GameLedgerRepository @Inject constructor(
    private val gameDayPointsDao: GameDayPointsDao,
    private val diaryDao: DiaryDao,
    private val bodyWeightDao: BodyWeightDao,
    private val nutrientGoalChangeDao: NutrientGoalChangeDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val fluidRepository: FluidRepository,
    private val habitRepository: HabitRepository,
    private val habitCheckInDao: HabitCheckInDao,
    private val habitGoalDao: HabitGoalDao,
    private val sleepRepository: SleepRepository,
    private val taskRepository: TaskRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
) {
    fun observePoints(): Flow<List<GameDayPoints>> = gameDayPointsDao.observeAll()

    /**
     * Works out every day that has finished but never been settled, and settles it.
     *
     * Days are walked oldest first, and a window of days *before* the first unsettled one is
     * evaluated without being written: a goal's difficulty is its hit rate over the preceding
     * [DIFFICULTY_WINDOW_DAYS], so the run needs to have seen them even when they are long since
     * booked. That evaluation is all in memory — every source is read once for the whole span.
     */
    suspend fun bookMissingDays(today: Long = DateUtils.todayEpochDay()) = withContext(Dispatchers.Default) {
        // Off the main thread deliberately: the queries would move themselves, but judging a year of
        // days against every goal is plain computation, and it is called from a ViewModel scope.
        val lastBookable = today - 1
        val booked = gameDayPointsDao.bookedDays().toSet()
        val earliest = earliestInterestingDay(today)
        val firstToBook = (earliest..lastBookable).firstOrNull { it !in booked } ?: return@withContext

        val evaluateFrom = firstToBook - DIFFICULTY_WINDOW_DAYS
        val source = loadSource(evaluateFrom, lastBookable)
        val history = GoalHistoryWindow()
        val bookedAt = Instant.now()

        for (day in evaluateFrom..lastBookable) {
            val goals = scoredGoals(day, source)
            if (day >= firstToBook && day !in booked) {
                val score = dayScore(day, goals, history.before(day))
                gameDayPointsDao.upsertAll(
                    AvatarAttribute.entries.map { attribute ->
                        GameDayPoints(
                            epochDay = day,
                            attribute = attribute,
                            // Every attribute, zero included: the days present in the table are
                            // exactly the days that are settled.
                            points = score.points[attribute] ?: 0.0,
                            bookedAt = bookedAt,
                        )
                    },
                )
            }
            history.record(day, goals)
        }
    }

    /**
     * Where a run starts: the first day anything was logged, never more than [MAX_BACKFILL_DAYS]
     * back.
     *
     * Only the diary and the scale are asked, because they are the two that can answer it in one
     * query. An install that has never used either still gets the last month settled, so somebody
     * who only logs training starts earning from their Fitness-Ziele rather than from nothing.
     */
    private suspend fun earliestInterestingDay(today: Long): Long {
        val firstLogged = listOfNotNull(
            diaryDao.observeFirstLoggedDay().first(),
            bodyWeightDao.observeFirstLoggedDay().first(),
        ).minOrNull() ?: (today - FORM_WINDOW_DAYS)
        return maxOf(firstLogged, today - MAX_BACKFILL_DAYS)
    }

    // ---- Reading every source once ---------------------------------------------------------

    private suspend fun loadSource(from: Long, to: Long): DaySource {
        val prefs = userPreferencesRepository.userPreferences.first()
        val habits = habitRepository.observeActive().first()
        return DaySource(
            nutritionByDay = diaryDao.observeDailyNutritionTotals(from, to).first()
                .associate { it.epochDay to it.totals },
            nutrientChanges = nutrientGoalChangeDao.observeAll().first().groupBy { it.nutrient },
            currentNutrientGoals = prefs.nutrientGoals,
            fluidMlByDay = fluidRepository.observeDailyMlTotals(from, to).first()
                .associate { it.epochDay to it.value },
            dailyWaterGoalMl = prefs.dailyWaterGoalMl,
            habits = habits,
            checkInsByDay = habits
                .flatMap { habitCheckInDao.getAllForHabit(it.id) }
                .groupBy { it.epochDay },
            habitDailyGoals = habitGoalDao.observeAll().first()
                .filter { it.period == GoalPeriod.DAILY }
                .associateBy { it.habitId },
            sleepByDay = sleepRepository.observeAll().first().associateBy { it.epochDay },
            sleepDurationGoalMinutes = prefs.sleepDurationGoalMinutes,
            bedtimeGoalMinuteOfDay = prefs.bedtimeGoalMinuteOfDay,
            tasks = taskRepository.observeActive().first(),
            taskCompletions = taskRepository.observeCompletions().first(),
            fitnessGoals = fitnessGoalRepository.observeAll().first(),
        )
    }

    /**
     * Every goal that had an answer on [day], each already routed to the attribute it feeds.
     *
     * The rows come from the very builders the Tagesziele screen uses — they are pure functions of
     * one day's data, so feeding them a past day yields exactly the verdict that day would have
     * shown. Nothing about "erreicht" is re-implemented here.
     */
    private suspend fun scoredGoals(day: Long, source: DaySource): List<ScoredGoal> {
        val rows = mutableListOf<DayGoalRow>()

        val goalsForDay = Nutrient.entries.mapNotNull { nutrient ->
            nutrientGoalOn(
                day = day,
                changes = source.nutrientChanges[nutrient].orEmpty().sortedBy { it.effectiveFromEpochDay },
                currentGoal = source.currentNutrientGoals[nutrient],
            )?.let { nutrient to it }
        }.toMap()
        rows += nutrientGoalRows(
            goals = goalsForDay,
            consumed = (source.nutritionByDay[day] ?: NutritionTotals.ZERO).byNutrient(),
        )

        // The overall drinking goal only. Per-drink goals would need a per-type total for every past
        // day, which no query offers — and inventing one from today's types would judge a past day
        // against a goal that did not exist then.
        rows += fluidGoalRows(
            dailyGoalMl = source.dailyWaterGoalMl,
            totalMl = source.fluidMlByDay[day] ?: 0.0,
            types = emptyList(),
            totalsByTypeId = emptyMap(),
        )

        val checkIns = source.checkInsByDay[day].orEmpty()
        rows += habitGoalRows(
            habits = source.habits,
            dailyGoalsByHabitId = source.habitDailyGoals,
            checkedInHabitIds = checkIns.map { it.habitId }.toSet(),
            valuesByHabitId = checkIns.mapNotNull { c -> c.value?.let { c.habitId to it } }.toMap(),
        )

        rows += sleepGoalRows(
            entry = source.sleepByDay[day],
            durationGoalMinutes = source.sleepDurationGoalMinutes,
            bedtimeGoalMinuteOfDay = source.bedtimeGoalMinuteOfDay,
        )

        rows += taskRows(taskStatuses(source.tasks, source.taskCompletions, day))

        val fromRows = rows.mapNotNull { row ->
            attributeForGoalRow(row.id)?.let { ScoredGoal(row.id, it, row.isMet) }
        }
        return fromRows + fitnessGoalsEndingOn(day, source.fitnessGoals)
    }

    /**
     * The Fitness-Ziele whose period ends on [day], evaluated as of that last day.
     *
     * A period goal cannot pay per day — a weekly set count would otherwise be earned seven times
     * over, or be judged unfinished six days out of seven. It pays once, when its period is over and
     * the answer is final.
     */
    private suspend fun fitnessGoalsEndingOn(day: Long, goals: List<FitnessGoal>): List<ScoredGoal> =
        goals.filter { periodEndDay(it.period, day) == day }
            .map { goal ->
                val progress = fitnessGoalRepository.getProgress(goal, day)
                ScoredGoal(
                    id = "fitness-${goal.id}",
                    attribute = attributeForFitnessMetric(goal.metric),
                    isMet = progress.isMet,
                )
            }
}

/** Everything the whole span needs, read once so a day's verdict costs no queries. */
private data class DaySource(
    val nutritionByDay: Map<Long, NutritionTotals>,
    val nutrientChanges: Map<Nutrient, List<NutrientGoalChange>>,
    val currentNutrientGoals: Map<Nutrient, NutrientGoal>,
    val fluidMlByDay: Map<Long, Double>,
    val dailyWaterGoalMl: Double,
    val habits: List<Habit>,
    val checkInsByDay: Map<Long, List<HabitCheckIn>>,
    val habitDailyGoals: Map<String, HabitGoal>,
    val sleepByDay: Map<Long, SleepEntry>,
    val sleepDurationGoalMinutes: NutrientGoal?,
    val bedtimeGoalMinuteOfDay: Int?,
    val tasks: List<Task>,
    val taskCompletions: List<TaskCompletion>,
    val fitnessGoals: List<FitnessGoal>,
)

/**
 * The rolling record of how each goal has been going, as the run walks forward.
 *
 * Only days that are already behind the one being scored are in it — a goal's difficulty must not
 * be influenced by the very day it is being paid out for, or reaching a goal would make that same
 * day's reward smaller.
 */
private class GoalHistoryWindow {
    private val outcomes = mutableMapOf<String, MutableList<Pair<Long, Boolean>>>()

    fun record(day: Long, goals: List<ScoredGoal>) {
        goals.forEach { goal ->
            outcomes.getOrPut(goal.id) { mutableListOf() }.add(day to goal.isMet)
        }
    }

    /** How each goal went over the [DIFFICULTY_WINDOW_DAYS] days ending the day before [day]. */
    fun before(day: Long): Map<String, GoalHistory> {
        val window = (day - DIFFICULTY_WINDOW_DAYS) until day
        return outcomes.mapValues { (_, entries) ->
            val inWindow = entries.filter { (recorded, _) -> recorded in window }
            GoalHistory(met = inWindow.count { it.second }, judged = inWindow.size)
        }
    }
}
