package com.example.prokject2_tracker.task

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant

/**
 * How a task repeats. The interval kinds all share the same shape — "every N of something, counted
 * from [Task.startEpochDay]" — which is what keeps [nextDueOnOrAfter] to one small function per kind.
 */
enum class TaskRecurrence {
    /** A single date. Once it is done, the task is finished for good. */
    ONCE,

    /** Every [Task.intervalCount] days from the start day: "alle 3 Tage". */
    EVERY_N_DAYS,

    /** Every [Task.intervalCount] weeks, on the start day's weekday: "alle 3 Wochen". */
    EVERY_N_WEEKS,

    /** On the weekdays in [Task.weekdayMask], every week: "Mo/Mi/Fr". */
    WEEKDAYS,

    /** On [Task.dayOfMonth] every [Task.intervalCount] months: "immer am Monatsersten". */
    DAY_OF_MONTH,
}

/** Monday is bit 0 — [DayOfWeek.getValue] is 1-based, so the shift is by `value - 1`. */
fun DayOfWeek.bit(): Int = 1 shl (value - 1)

fun Int.hasWeekday(dayOfWeek: DayOfWeek): Boolean = this and dayOfWeek.bit() != 0

/**
 * A thing to do, either once or on a rhythm. The task holds only the *rule*; which of its due dates
 * are done lives in [TaskCompletion], so a recurring task needs no rewriting as it is worked through.
 *
 * [startEpochDay] anchors every rhythm: it is the due date for [TaskRecurrence.ONCE], and the first
 * day the others can fire on. Fields not used by the chosen [recurrence] are ignored rather than
 * cleared — switching the rhythm back and forth in the dialog then keeps what was typed.
 */
@Entity(tableName = "tasks", indices = [Index("name")])
data class Task(
    @PrimaryKey val id: String,
    val name: String,
    val recurrence: TaskRecurrence,
    /** The N in "alle N Tage/Wochen/Monate". At least 1; ignored by the other rhythms. */
    val intervalCount: Int = 1,
    /** Bitmask of [DayOfWeek.bit] values, for [TaskRecurrence.WEEKDAYS]. */
    val weekdayMask: Int = 0,
    /** 1–31 for [TaskRecurrence.DAY_OF_MONTH]; clamped to the month's length when it is short. */
    val dayOfMonth: Int = 1,
    val startEpochDay: Long,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * One due date ticked off. Keyed by the *due* day rather than the day of the tap, so working
 * through a backlog credits the occurrence it was meant for; [completedEpochDay] separately records
 * when it actually happened, which is what lets the Tagesziele still show it as done today.
 */
@Entity(
    tableName = "task_completions",
    indices = [
        Index(value = ["taskId", "dueEpochDay"], unique = true),
        Index("completedEpochDay"),
    ],
    // A deleted task takes its history with it — the rows mean nothing without the rule that
    // produced the due dates they point at.
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TaskCompletion(
    @PrimaryKey val id: String,
    val taskId: String,
    val dueEpochDay: Long,
    val completedEpochDay: Long,
    val createdAt: Instant,
)
