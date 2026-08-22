package com.example.mytracker.fitness.cardio

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
data class CardioActivityTypeDto(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

private fun CardioActivityType.toDto() = CardioActivityTypeDto(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun CardioActivityTypeDto.toEntity() = CardioActivityType(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/** Exports/imports cardio activity-type definitions only — logged cardio sessions are tracked data and never included. */
class CardioActivityTypeLibraryExportProvider @Inject constructor(
    private val cardioActivityTypeDao: CardioActivityTypeDao,
) : BackupExportProvider {
    override val key = "cardioActivityTypes"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(cardioActivityTypeDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<CardioActivityTypeDto>>(json)
        dtos.forEach { dto ->
            val existing = cardioActivityTypeDao.getById(dto.id)
            if (existing == null) {
                cardioActivityTypeDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        cardioActivityTypeDao.deleteAll()
    }
}
