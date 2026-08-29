package com.example.mytracker.fitness.strength

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projections matching [com.example.mytracker.core.metrics.MetricPoint]'s field names. */
data class DailySetsTotal(val epochDay: Long, val value: Double)
data class DailyVolumeTotal(val epochDay: Long, val value: Double)

/** One exercise's heaviest set ever. [value] is null for an exercise only ever done at bodyweight. */
data class ExerciseMaxWeight(val exerciseId: String, val value: Double?)


@Dao
interface StrengthSetDao {
    @Query("SELECT * FROM strength_sets WHERE logEntryId = :logEntryId ORDER BY setIndex")
    suspend fun getForLogEntry(logEntryId: String): List<StrengthSet>

    @Query("SELECT * FROM strength_sets ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<StrengthSet>>

    @Upsert
    suspend fun upsertAll(sets: List<StrengthSet>)

    @Query("DELETE FROM strength_sets WHERE logEntryId = :logEntryId")
    suspend fun deleteForLogEntry(logEntryId: String)

    @Transaction
    suspend fun replaceSetsForLogEntry(logEntryId: String, sets: List<StrengthSet>) {
        deleteForLogEntry(logEntryId)
        if (sets.isNotEmpty()) upsertAll(sets)
    }

    @Query("SELECT * FROM strength_sets WHERE exerciseId = :exerciseId ORDER BY epochDay DESC, setIndex DESC LIMIT 1")
    suspend fun getMostRecentForExercise(exerciseId: String): StrengthSet?

    /**
     * One exercise's whole history, newest day first and in logging order within a day. The join is
     * what makes that order right when a day holds more than one log entry — `setIndex` restarts at
     * 0 per entry, so it can't order a day on its own. A single exercise is a few hundred rows even
     * after years, so the detail page derives its stats from these rows instead of a query per figure.
     */
    @Query(
        "SELECT ss.* FROM strength_sets ss " +
            "JOIN strength_log_entries sle ON sle.id = ss.logEntryId " +
            "WHERE ss.exerciseId = :exerciseId " +
            "ORDER BY ss.epochDay DESC, sle.createdAt ASC, ss.setIndex ASC",
    )
    fun observeAllForExercise(exerciseId: String): Flow<List<StrengthSet>>

    /**
     * The sets of every exercise's most recent training day — what the exercise list needs to show
     * both "zuletzt: vor 3 Tagen" and the top set of that session. Bounded by (exercises × sets per
     * session), so it stays small no matter how long the log gets; the day itself rides along on the
     * rows, which is why no separate `MAX(epochDay)` query is needed.
     */
    @Query(
        "SELECT ss.* FROM strength_sets ss " +
            "JOIN strength_log_entries sle ON sle.id = ss.logEntryId " +
            "JOIN (SELECT exerciseId, MAX(epochDay) AS lastDay FROM strength_sets GROUP BY exerciseId) latest " +
            "ON latest.exerciseId = ss.exerciseId AND latest.lastDay = ss.epochDay " +
            "ORDER BY ss.exerciseId, sle.createdAt ASC, ss.setIndex ASC",
    )
    fun observeLastSessionSetsPerExercise(): Flow<List<StrengthSet>>

    @Query(
        "SELECT epochDay, COUNT(*) AS value FROM strength_sets " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailySetsTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>>

    @Query(
        "SELECT epochDay, SUM(reps * COALESCE(weightKg,0)) AS value FROM strength_sets " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyVolumeTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>>

    @Query("SELECT COUNT(*) FROM strength_sets WHERE epochDay BETWEEN :startInclusive AND :endInclusive")
    suspend fun countBetween(startInclusive: Long, endInclusive: Long): Int

    @Query(
        "SELECT COUNT(*) FROM strength_sets ss " +
            "JOIN strength_exercise_muscle_groups segm ON segm.exerciseId = ss.exerciseId " +
            "WHERE segm.muscleGroupId = :muscleGroupId AND ss.epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun countBetweenForMuscleGroup(muscleGroupId: String, startInclusive: Long, endInclusive: Long): Int

    /** [movementDirection] is a [MovementDirection] name; untagged exercises match nothing. */
    @Query(
        "SELECT COUNT(*) FROM strength_sets ss " +
            "JOIN strength_exercises se ON se.id = ss.exerciseId " +
            "WHERE se.movementDirection = :movementDirection " +
            "AND ss.epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun countBetweenForMovementDirection(
        movementDirection: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Int

    @Query(
        "SELECT epochDay, SUM(reps * COALESCE(weightKg,0)) AS value FROM strength_sets " +
            "WHERE exerciseId = :exerciseId AND epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyVolumeTotalsForExercise(
        exerciseId: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailyVolumeTotal>>

    @Query(
        "SELECT epochDay, COUNT(*) AS value FROM strength_sets " +
            "WHERE exerciseId = :exerciseId AND epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailySetsTotalsForExercise(
        exerciseId: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailySetsTotal>>

    @Query(
        "SELECT ss.epochDay, SUM(ss.reps * COALESCE(ss.weightKg,0)) AS value FROM strength_sets ss " +
            "JOIN strength_exercise_muscle_groups segm ON segm.exerciseId = ss.exerciseId " +
            "WHERE segm.muscleGroupId = :muscleGroupId AND ss.epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY ss.epochDay ORDER BY ss.epochDay",
    )
    fun observeDailyVolumeTotalsForMuscleGroup(
        muscleGroupId: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailyVolumeTotal>>

    @Query(
        "SELECT ss.epochDay, COUNT(*) AS value FROM strength_sets ss " +
            "JOIN strength_exercise_muscle_groups segm ON segm.exerciseId = ss.exerciseId " +
            "WHERE segm.muscleGroupId = :muscleGroupId AND ss.epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY ss.epochDay ORDER BY ss.epochDay",
    )
    fun observeDailySetsTotalsForMuscleGroup(
        muscleGroupId: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailySetsTotal>>

    @Query(
        "SELECT ss.epochDay, SUM(ss.reps * COALESCE(ss.weightKg,0)) AS value FROM strength_sets ss " +
            "JOIN strength_exercises se ON se.id = ss.exerciseId " +
            "WHERE se.movementDirection = :movementDirection " +
            "AND ss.epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY ss.epochDay ORDER BY ss.epochDay",
    )
    fun observeDailyVolumeTotalsForMovementDirection(
        movementDirection: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailyVolumeTotal>>

    @Query(
        "SELECT ss.epochDay, COUNT(*) AS value FROM strength_sets ss " +
            "JOIN strength_exercises se ON se.id = ss.exerciseId " +
            "WHERE se.movementDirection = :movementDirection " +
            "AND ss.epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY ss.epochDay ORDER BY ss.epochDay",
    )
    fun observeDailySetsTotalsForMovementDirection(
        movementDirection: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailySetsTotal>>

    /**
     * The heaviest set of one exercise in a window. Null when the exercise was not trained in it —
     * which is not the same as 0 kg, and the difference decides whether a Steigerung is even defined.
     */
    @Query(
        "SELECT MAX(weightKg) FROM strength_sets " +
            "WHERE exerciseId = :exerciseId AND epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun maxWeightBetweenForExercise(exerciseId: String, startInclusive: Long, endInclusive: Long): Double?

    /**
     * The best this exercise had ever been before [beforeDay] — what a Steigerung of the top set is
     * measured against. Deliberately not "the period before": a week off would reset the bar to
     * nothing and turn simply repeating an old top set into a record gain.
     */
    @Query("SELECT MAX(weightKg) FROM strength_sets WHERE exerciseId = :exerciseId AND epochDay < :beforeDay")
    suspend fun maxWeightBeforeForExercise(exerciseId: String, beforeDay: Long): Double?

    @Query("SELECT MAX(weightKg) FROM strength_sets WHERE exerciseId = :exerciseId")
    suspend fun maxWeightForExercise(exerciseId: String): Double?

    /** Every exercise's all-time top set at once — for a screen that lists them all. */
    @Query("SELECT exerciseId, MAX(weightKg) AS value FROM strength_sets GROUP BY exerciseId")
    fun observeMaxWeightPerExercise(): Flow<List<ExerciseMaxWeight>>

    /** Volume is Wiederholungen × Gewicht; a bodyweight set contributes nothing to it. */
    @Query(
        "SELECT COALESCE(SUM(reps * COALESCE(weightKg, 0)), 0) FROM strength_sets " +
            "WHERE exerciseId = :exerciseId AND epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun volumeBetweenForExercise(exerciseId: String, startInclusive: Long, endInclusive: Long): Double

    /**
     * How many sets this exercise had in a window. Whether a period counts as trained is read from
     * this and not from the volume: a bodyweight session carries no volume at all, and counting it
     * as "nicht trainiert" would put a Klimmzug-Woche on the same footing as a week on the sofa.
     */
    @Query(
        "SELECT COUNT(*) FROM strength_sets " +
            "WHERE exerciseId = :exerciseId AND epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun countBetweenForExercise(exerciseId: String, startInclusive: Long, endInclusive: Long): Int

    @Query(
        "SELECT COALESCE(SUM(ss.reps * COALESCE(ss.weightKg, 0)), 0) FROM strength_sets ss " +
            "JOIN strength_exercise_muscle_groups segm ON segm.exerciseId = ss.exerciseId " +
            "WHERE segm.muscleGroupId = :muscleGroupId AND ss.epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun volumeBetweenForMuscleGroup(muscleGroupId: String, startInclusive: Long, endInclusive: Long): Double

    /** [movementDirection] is a [MovementDirection] name; untagged exercises match nothing. */
    @Query(
        "SELECT COALESCE(SUM(ss.reps * COALESCE(ss.weightKg, 0)), 0) FROM strength_sets ss " +
            "JOIN strength_exercises se ON se.id = ss.exerciseId " +
            "WHERE se.movementDirection = :movementDirection " +
            "AND ss.epochDay BETWEEN :startInclusive AND :endInclusive",
    )
    suspend fun volumeBetweenForMovementDirection(
        movementDirection: String,
        startInclusive: Long,
        endInclusive: Long,
    ): Double

    @Query("SELECT * FROM strength_sets ORDER BY epochDay, setIndex")
    suspend fun getAllOnce(): List<StrengthSet>
}
