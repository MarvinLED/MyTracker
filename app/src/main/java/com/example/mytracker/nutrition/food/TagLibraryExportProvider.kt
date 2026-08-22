package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import com.example.prokject2_tracker.core.backup.BackupScope
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
    /** Both default so a backup written before Tags had colours or dependencies still reads. */
    val colorArgb: Int? = null,
    /** The tags this one implies — "vegan" carries "vegetarisch" here. */
    val impliesTagIds: List<String> = emptyList(),
)

private fun Tag.toDto(impliesTagIds: List<String>) = TagDto(
    id = id,
    name = name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    colorArgb = colorArgb,
    impliesTagIds = impliesTagIds,
)

private fun TagDto.toEntity() = Tag(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    colorArgb = colorArgb,
)

/**
 * Key `"tags"`, imported before `"foods"` (see [com.example.prokject2_tracker.nutrition.food.FoodLibraryExportProvider])
 * since a food's tag ids are foreign keys into this data.
 */
class TagLibraryExportProvider @Inject constructor(
    private val tagDao: TagDao,
) : BackupExportProvider {
    override val key = "tags"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val impliedByChild = tagDao.getAllImplicationsOnce().groupBy({ it.childTagId }) { it.parentTagId }
        return json.encodeToJsonElement(
            tagDao.getAllOnce().map { it.toDto(impliedByChild[it.id].orEmpty()) },
        )
    }

    /**
     * Two passes: every tag has to exist before any implication can point at one, since a tag may
     * well imply another that comes later in the list and the foreign key would reject it.
     */
    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<TagDto>>(json)
        dtos.forEach { dto ->
            if (tagDao.getById(dto.id) == null) {
                tagDao.upsert(dto.toEntity())
            }
        }
        dtos.forEach { dto ->
            dto.impliesTagIds.forEach { parentId ->
                if (tagDao.getById(parentId) != null) {
                    tagDao.upsertImplication(TagImplication(childTagId = dto.id, parentTagId = parentId))
                }
            }
        }
    }

    /** The links to Lebensmittel and between Tags cascade with the Tags. */
    override suspend fun clear() {
        tagDao.deleteAllImplications()
        tagDao.deleteAll()
    }
}
