package com.example.mytracker.achievements

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
data class GameDayPointsDto(
    val epochDay: Long,
    val attribute: String,
    val points: Double,
    val bookedAtEpochMillis: Long,
)

private fun GameDayPoints.toDto() = GameDayPointsDto(
    epochDay = epochDay,
    attribute = attribute.name,
    points = points,
    bookedAtEpochMillis = bookedAt.toEpochMilli(),
)

/**
 * The points ledger travels with the tracked data.
 *
 * It has to: the ledger is deliberately not reproducible from the entries alone, because Flüssigkeit,
 * Schlaf and Habits keep no goal history, so a day settled under last year's water goal could not be
 * settled the same way again. Leaving it out of the backup would quietly reshape the figure on every
 * restore.
 *
 * An unknown attribute name is skipped rather than failing the import — that is a ledger written by
 * a newer version, and losing one row of one day beats losing the restore.
 */
class GamePointsExportProvider @Inject constructor(
    private val gameDayPointsDao: GameDayPointsDao,
) : BackupExportProvider {
    override val key = "gameDayPoints"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(gameDayPointsDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<GameDayPointsDto>>(json)
        val existing = gameDayPointsDao.bookedDays().toSet()
        val rows = dtos.mapNotNull { dto ->
            val attribute = AvatarAttribute.entries.firstOrNull { it.name == dto.attribute }
                ?: return@mapNotNull null
            // A day already settled here keeps its own booking. Re-settling it from a backup would
            // overwrite a decision that was final when it was made.
            if (dto.epochDay in existing) return@mapNotNull null
            GameDayPoints(
                epochDay = dto.epochDay,
                attribute = attribute,
                points = dto.points,
                bookedAt = Instant.ofEpochMilli(dto.bookedAtEpochMillis),
            )
        }
        gameDayPointsDao.upsertAll(rows)
    }

    override suspend fun clear() {
        gameDayPointsDao.deleteAll()
    }
}
