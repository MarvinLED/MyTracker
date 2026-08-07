package com.example.prokject2_tracker.fitness.strength

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projections matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailySetsTotal(val epochDay: Long, val value: Double)
data class DailyVolumeTotal(val epochDay: Long, val value: Double)


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

    @Query("SELECT * FROM strength_sets ORDER BY epochDay, setIndex")
    suspend fun getAllOnce(): List<StrengthSet>
}
