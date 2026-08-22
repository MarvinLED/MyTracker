package com.example.mytracker.fitness.strength

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupScope
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class StrengthSetDto(
    val id: String,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val isBodyweight: Boolean = false,
)

@Serializable
data class StrengthLogEntryDto(
    val id: String,
    val epochDay: Long,
    val createdAtEpochMillis: Long,
    val exerciseId: String,
    val exerciseName: String,
    val note: String? = null,
    val sets: List<StrengthSetDto> = emptyList(),
)

private fun StrengthSet.toDto() = StrengthSetDto(
    id = id,
    setIndex = setIndex,
    reps = reps,
    weightKg = weightKg,
    isBodyweight = isBodyweight,
)

/**
 * A set's `epochDay` and `exerciseId` are denormalised copies of its entry's — the charts group by
 * them without a join — so they are rebuilt from the entry rather than stored twice in the file.
 */
private fun StrengthSetDto.toEntity(entry: StrengthLogEntryDto) = StrengthSet(
    id = id,
    logEntryId = entry.id,
    epochDay = entry.epochDay,
    exerciseId = entry.exerciseId,
    setIndex = setIndex,
    reps = reps,
    weightKg = weightKg,
    isBodyweight = isBodyweight,
)

private fun StrengthLogEntry.toDto(sets: List<StrengthSet>) = StrengthLogEntryDto(
    id = id,
    epochDay = epochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    note = note,
    sets = sets.sortedBy { it.setIndex }.map { it.toDto() },
)

private fun StrengthLogEntryDto.toEntity() = StrengthLogEntry(
    id = id,
    epochDay = epochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    note = note,
)

/**
 * The Krafttraining log: each session with its Sätze nested inside, so the two can never arrive
 * apart. The entry carries the exercise's name as well as its id, so a session stays readable when
 * the Übung stayed behind — but the sets do not, since `strength_sets.exerciseId` is what every
 * chart and every "letztes Training" comparison groups by. An entry whose Übung is missing is
 * therefore skipped whole rather than imported half-useful.
 */
class StrengthLogExportProvider @Inject constructor(
    private val strengthLogDao: StrengthLogDao,
    private val strengthSetDao: StrengthSetDao,
    private val strengthExerciseDao: StrengthExerciseDao,
) : BackupExportProvider {
    override val key = "strengthLog"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val setsByEntry = strengthSetDao.getAllOnce().groupBy { it.logEntryId }
        val dtos = strengthLogDao.getAllOnce().map { entry ->
            entry.toDto(setsByEntry[entry.id].orEmpty())
        }
        return json.encodeToJsonElement(dtos)
    }

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<StrengthLogEntryDto>>(json)
        dtos.forEach { dto ->
            if (strengthLogDao.getById(dto.id) != null) return@forEach
            if (strengthExerciseDao.getById(dto.exerciseId) == null) return@forEach
            strengthLogDao.upsert(dto.toEntity())
            strengthSetDao.replaceSetsForLogEntry(dto.id, dto.sets.map { it.toEntity(dto) })
        }
    }

    /** The Sätze cascade with the entries that own them. */
    override suspend fun clear() {
        strengthLogDao.deleteAll()
    }
}
