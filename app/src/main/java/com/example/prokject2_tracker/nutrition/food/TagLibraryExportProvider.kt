package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
)

private fun Tag.toDto() = TagDto(id = id, name = name, createdAtEpochMillis = createdAt.toEpochMilli())

private fun TagDto.toEntity() = Tag(id = id, name = name, createdAt = Instant.ofEpochMilli(createdAtEpochMillis))

/**
 * Key `"tags"`, imported before `"foods"` (see [com.example.prokject2_tracker.nutrition.food.FoodLibraryExportProvider])
 * since a food's tag ids are foreign keys into this data.
 */
class TagLibraryExportProvider @Inject constructor(
    private val tagDao: TagDao,
) : LibraryExportProvider {
    override val key = "tags"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(tagDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<TagDto>>(json)
        dtos.forEach { dto ->
            if (tagDao.getById(dto.id) == null) {
                tagDao.upsert(dto.toEntity())
            }
        }
    }
}
