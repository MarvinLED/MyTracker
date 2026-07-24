package com.example.prokject2_tracker.habit

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class HabitGoalDto(
    val period: GoalPeriod,
    val targetValue: Double,
)

@Serializable
data class HabitDto(
    val id: String,
    val name: String,
    val archived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val type: HabitType = HabitType.YES_NO,
    val goals: List<HabitGoalDto> = emptyList(),
)

private fun Habit.toDto(goals: List<HabitGoal>) = HabitDto(
    id = id,
    name = name,
    archived = archived,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    type = type,
    goals = goals.map { HabitGoalDto(period = it.period, targetValue = it.targetValue) },
)

private fun HabitDto.toEntity() = Habit(
    id = id,
    name = name,
    archived = archived,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    type = type,
)

/** Exports/imports habit *definitions* (including goals) only — check-ins are tracked data and never included. */
class HabitLibraryExportProvider @Inject constructor(
    private val habitDao: HabitDao,
    private val habitGoalDao: HabitGoalDao,
) : LibraryExportProvider {
    override val key = "habits"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val habits = habitDao.getAllOnce()
        val goalsByHabitId = habitGoalDao.getAllOnce().groupBy { it.habitId }
        return json.encodeToJsonElement(
            habits.map { habit -> habit.toDto(goalsByHabitId[habit.id] ?: emptyList()) },
        )
    }

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<HabitDto>>(json)
        dtos.forEach { dto ->
            val existing = habitDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                habitDao.upsert(dto.toEntity())
            }
            habitGoalDao.replaceGoalsForHabit(
                dto.id,
                dto.goals.map { goalDto ->
                    HabitGoal(
                        id = "${dto.id}-${goalDto.period.name}",
                        habitId = dto.id,
                        period = goalDto.period,
                        targetValue = goalDto.targetValue,
                        createdAt = Instant.now(),
                    )
                },
            )
        }
    }
}
