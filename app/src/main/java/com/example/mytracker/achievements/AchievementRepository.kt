package com.example.mytracker.achievements

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.DayStreak
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.dayStreak
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalRepository
import com.example.mytracker.fitness.cardio.CardioRepository
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fitness.strength.StrengthExerciseRepository
import com.example.mytracker.fitness.strength.StrengthLogRepository
import com.example.mytracker.fitness.strength.StrengthSet
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.goals.dayGoalLabel
import com.example.mytracker.habit.Habit
import com.example.mytracker.habit.HabitRepository
import com.example.mytracker.nutrition.diary.DiaryRepository
import com.example.mytracker.sleep.SleepRepository
import com.example.mytracker.weight.BodyWeightEntry
import com.example.mytracker.weight.BodyWeightRepository
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * How far back a goal's best run is looked for. Half a year of weeks — far enough for the mark to
 * mean something, and bounded because each period costs the same queries as evaluating the current
 * one. Every text built from it says how far it looked, so the number is never passed off as
 * all-time.
 */
private const val GOAL_RECORD_PERIODS = 26

/** Every day the app has ever seen. Wide enough to be "all time" without a first-day lookup. */
private const val FIRST_POSSIBLE_DAY = 0L

private val dayFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)

/**
 * The wall of records and milestones, assembled from what is already logged.
 *
 * Nothing here is stored. Every mark is recomputed from the entries themselves, which has three
 * consequences worth the extra work: the wall is already full the first time it is opened, a
 * restored backup brings every mark back with it, and no record can drift out of step with the data
 * it claims to describe.
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val strengthLogRepository: StrengthLogRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
    private val cardioRepository: CardioRepository,
    private val bodyWeightRepository: BodyWeightRepository,
    private val diaryRepository: DiaryRepository,
    private val fluidRepository: FluidRepository,
    private val habitRepository: HabitRepository,
    private val sleepRepository: SleepRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
) {
    /**
     * The days anything at all was logged, across every area that records one. Union rather than any
     * single source: a day spent logging only sleep is a day the app was used, and an Erfassungsserie
     * that ignored it would break on it.
     */
    private fun loggedDays(today: Long): Flow<Set<Long>> = combine(
        // A list rather than four arguments: every source has already been reduced to the same
        // "which days does this area know about", and the fold treats them all alike.
        listOf(
            diaryRepository.observeDailyKcalTotals(FIRST_POSSIBLE_DAY, today).map { rows -> rows.map { it.epochDay } },
            fluidRepository.observeDailyMlTotals(FIRST_POSSIBLE_DAY, today).map { rows -> rows.map { it.epochDay } },
            habitRepository.observeDailyCompletedCounts(FIRST_POSSIBLE_DAY, today)
                .map { rows -> rows.map { it.epochDay } },
            sleepRepository.observeDailyDurationMinutes(FIRST_POSSIBLE_DAY, today)
                .map { rows -> rows.map { it.epochDay } },
        ),
    ) { sources -> sources.flatMap { it }.toSet() }

    fun observe(today: Long = DateUtils.todayEpochDay()): Flow<List<AchievementSection>> = combine(
        combine(
            strengthLogRepository.observeAllSets(),
            strengthExerciseRepository.observeAll(),
        ) { sets, exercises -> sets to exercises },
        combine(
            cardioRepository.observeAll().map { sessions -> sessions.map { it.epochDay } },
            bodyWeightRepository.observeAll(),
        ) { cardioDays, weights -> cardioDays to weights },
        loggedDays(today),
        combine(
            habitRepository.observeActive(),
            fitnessGoalRepository.observeAll(),
            strengthExerciseRepository.observeMuscleGroups(),
        ) { habits, goals, muscleGroups -> Triple(habits, goals, muscleGroups) },
    ) { (sets, exercises), (cardioDays, weights), otherDays, (habits, goals, muscleGroups) ->
        val allLoggedDays = otherDays +
            sets.map { it.epochDay } +
            cardioDays +
            weights.map { it.epochDay }
        val streak = dayStreak(allLoggedDays, today)

        listOfNotNull(
            bestMarksSection(sets, exercises, weights, today),
            runsSection(streak, habits, goals, muscleGroups.associate { it.id to it.name }, exercises, today),
            milestonesSection(sets, cardioDays.size, allLoggedDays.size, streak),
        )
    }
        // Walking every set for its records, and every goal back over 26 periods, is far too much to
        // do on the thread that is drawing the screen.
        .flowOn(Dispatchers.Default)

    // ---- Bestmarken ------------------------------------------------------------------------

    private fun bestMarksSection(
        sets: List<StrengthSet>,
        exercises: List<StrengthExercise>,
        weights: List<BodyWeightEntry>,
        today: Long,
    ): AchievementSection? {
        val names = exercises.associate { it.id to it.name }
        val topSets = topSetRecords(sets)
            // Heaviest first: the wall should open with the mark worth being proud of, not with
            // whatever exercise happens to sort first alphabetically.
            .entries.sortedByDescending { it.value.value }
            .mapNotNull { (exerciseId, mark) ->
                val name = names[exerciseId] ?: return@mapNotNull null
                Achievement(
                    id = "topset-$exerciseId",
                    title = name,
                    value = "${mark.value.formatDecimal(2)} kg",
                    detail = mark.detailText { "${it.formatDecimal(2)} kg" },
                )
            }

        // Bodyweight-only weeks carry no tonnage at all; letting a 0 t week into the record walk
        // would make the very first week the standing mark and every later one "davor 0,0 t".
        val bestWeek = recordMark(weeklyVolumePoints(sets).filter { it.value > 0.0 })?.let { mark ->
            Achievement(
                id = "best-week-volume",
                title = "Stärkste Woche",
                value = "${(mark.value / 1000.0).formatDecimal(1)} t",
                detail = mark.detailText(dayLabel = "Woche ab ") { "${(it / 1000.0).formatDecimal(1)} t" },
            )
        }

        // A year, not all time: "leichtester Wert überhaupt" would reach back to a body that is no
        // longer the one being trained, and it can never be beaten again after a deliberate bulk.
        val yearAgo = today - 365
        val lowestWeight = weights.filter { it.epochDay in yearAgo..today }
            .minByOrNull { it.weightKg }
            ?.let { entry ->
                Achievement(
                    id = "lowest-weight-year",
                    title = "Leichtester Tag",
                    value = "${entry.weightKg.formatDecimal(1)} kg",
                    detail = "am ${dayText(entry.epochDay)} · letzte 12 Monate",
                )
            }

        val items = topSets + listOfNotNull(bestWeek, lowestWeight)
        return items.asSection("Bestmarken")
    }

    // ---- Serien ----------------------------------------------------------------------------

    private suspend fun runsSection(
        loggingStreak: DayStreak,
        habits: List<Habit>,
        goals: List<FitnessGoal>,
        muscleGroupNames: Map<String, String>,
        exercises: List<StrengthExercise>,
        today: Long,
    ): AchievementSection? {
        val logging = Achievement(
            id = "logging-streak",
            title = "Erfassungsserie",
            value = "${loggingStreak.best} Tage",
            detail = runDetail(loggingStreak),
        ).takeIf { loggingStreak.best >= 2 }

        val habitRuns = habits
            .map { habit -> habit to habitRepository.getStreak(habit, today) }
            .filter { (_, streak) -> streak.best >= 2 }
            .sortedByDescending { (_, streak) -> streak.best }
            .map { (habit, streak) ->
                Achievement(
                    id = "habit-streak-${habit.id}",
                    title = habit.name,
                    value = "${streak.best} Tage",
                    detail = runDetail(streak),
                )
            }

        val exerciseNames = exercises.associate { it.id to it.name }
        val goalRuns = goals
            .map { goal -> goal to fitnessGoalRepository.getStreak(goal, today, GOAL_RECORD_PERIODS) }
            .filter { (_, streak) -> streak.bestRun >= 2 }
            .sortedByDescending { (_, streak) -> streak.bestRun }
            .map { (goal, streak) ->
                val unit = when (goal.period) {
                    GoalPeriod.WEEKLY -> "Wochen"
                    GoalPeriod.MONTHLY -> "Monate"
                    GoalPeriod.DAILY -> "Tage"
                }
                Achievement(
                    id = "goal-streak-${goal.id}",
                    title = goal.dayGoalLabel(muscleGroupNames, exerciseNames),
                    value = "${streak.bestRun} $unit",
                    // Said outright, because the number is not an all-time one: it is the best run
                    // inside the window that was searched, and nothing beyond it was looked at.
                    detail = "beste Serie der letzten $GOAL_RECORD_PERIODS $unit" +
                        if (streak.currentRun >= 1) " · aktuell ${streak.currentRun}" else "",
                )
            }

        return (listOfNotNull(logging) + habitRuns + goalRuns).asSection("Serien")
    }

    private fun runDetail(streak: DayStreak): String = when {
        streak.current >= streak.best -> "läuft gerade"
        streak.current > 0 -> "aktuell ${streak.current}"
        else -> "aktuell keine"
    }

    // ---- Meilensteine ----------------------------------------------------------------------

    private fun milestonesSection(
        sets: List<StrengthSet>,
        cardioSessions: Int,
        loggedDayCount: Int,
        loggingStreak: DayStreak,
    ): AchievementSection? {
        val sessions = strengthSessionCount(sets) + cardioSessions
        val items = listOf(
            milestone(
                id = "milestone-days",
                title = "Erfasste Tage",
                value = loggedDayCount.toDouble(),
                steps = LoggedDayTiers,
                unit = "Tage",
            ),
            milestone(
                id = "milestone-volume",
                title = "Bewegtes Gewicht",
                value = totalVolume(sets),
                steps = TotalVolumeTiers,
                unit = "t",
                format = { "${(it / 1000.0).formatDecimal(1)} t" },
            ),
            milestone(
                id = "milestone-sessions",
                title = "Einheiten",
                value = sessions.toDouble(),
                steps = SessionTiers,
                unit = "Einheiten",
            ),
            milestone(
                id = "milestone-streak",
                title = "Längste Erfassungsserie",
                value = loggingStreak.best.toDouble(),
                steps = LoggingStreakTiers,
                unit = "Tage",
            ),
        )
        return items.asSection("Meilensteine")
    }

    private fun milestone(
        id: String,
        title: String,
        value: Double,
        steps: List<Double>,
        unit: String,
        format: (Double) -> String = { "${it.formatCompact()} $unit" },
    ): Achievement {
        val tier = tierFor(value, steps)
        return Achievement(
            id = id,
            title = title,
            value = format(value),
            detail = tier.reached?.let { "Meilenstein ${format(it)} erreicht" } ?: "noch kein Meilenstein",
            fraction = tier.fraction,
            nextLabel = tier.next?.let { "noch ${format(it - value)} bis ${format(it)}" },
        )
    }

    // ---- Formatting ------------------------------------------------------------------------

    private fun RecordMark.detailText(dayLabel: String = "am ", format: (Double) -> String): String =
        buildString {
            append(dayLabel)
            append(dayText(epochDay))
            previous?.let { append(" · davor ${format(it)}") }
        }

    private fun dayText(epochDay: Long): String =
        DateUtils.localDateOfEpochDay(epochDay).format(dayFormatter)

    private fun List<Achievement>.asSection(title: String): AchievementSection? =
        if (isEmpty()) null else AchievementSection(title, this)
}
