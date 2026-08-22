package com.example.prokject2_tracker.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): Task?

    @Upsert
    suspend fun upsert(task: Task)

    @Delete
    suspend fun delete(task: Task)

    /** Every Aufgabe including the archived ones — a backup keeps what the list has stopped showing. */
    @Query("SELECT * FROM tasks ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<Task>

    /** Wipes the Aufgaben for a replacing import; their completions cascade with them. */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface TaskCompletionDao {
    /**
     * Every completion there is. The due-day walk in [oldestOpenOccurrence] needs to ask about
     * arbitrary past days, so it is fed the whole set rather than a window — one small table for a
     * personal task list, and no query per task per day.
     */
    @Query("SELECT * FROM task_completions")
    fun observeAll(): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId")
    suspend fun getForTask(taskId: String): List<TaskCompletion>

    @Upsert
    suspend fun upsert(completion: TaskCompletion)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND dueEpochDay = :dueEpochDay")
    suspend fun deleteForTaskAndDueDay(taskId: String, dueEpochDay: Long)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("SELECT * FROM task_completions ORDER BY dueEpochDay")
    suspend fun getAllOnce(): List<TaskCompletion>

    /** The (Aufgabe, Fälligkeitstag) slot — unique, so an import matches on it and not on the id. */
    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND dueEpochDay = :dueEpochDay")
    suspend fun getForTaskAndDueDay(taskId: String, dueEpochDay: Long): TaskCompletion?

    /** Wipes the erledigten Aufgaben for a replacing import; the Aufgaben themselves stay. */
    @Query("DELETE FROM task_completions")
    suspend fun deleteAll()
}
