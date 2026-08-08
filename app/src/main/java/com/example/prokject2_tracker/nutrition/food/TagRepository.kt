package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
) {
    fun observeAllTags(): Flow<List<Tag>> = tagDao.observeAll()

    /** The "implies" graph — see [tagFilterClosure] for what a filter does with it. */
    fun observeAllImplications(): Flow<List<TagImplication>> = tagDao.observeAllImplications()

    /** Every food's currently attached tags, keyed by foodItemId — for the Bibliothek list. */
    fun observeTagsByFoodId(): Flow<Map<String, List<Tag>>> =
        combine(tagDao.observeAll(), tagDao.observeAllFoodItemTags()) { tags, crossRefs ->
            val tagById = tags.associateBy { it.id }
            crossRefs
                .groupBy({ it.foodItemId }) { tagById[it.tagId] }
                .mapValues { (_, tagList) -> tagList.filterNotNull() }
        }

    suspend fun getTagsForFoodOnce(foodItemId: String): List<Tag> {
        val ids = tagDao.getCrossRefsForFood(foodItemId).map { it.tagId }.toSet()
        return tagDao.getAllOnce().filter { it.id in ids }
    }

    /** Union of tags across several foods — used to derive a Rezept's tags from its ingredients. */
    suspend fun getTagsForFoodIds(foodItemIds: List<String>): List<Tag> {
        if (foodItemIds.isEmpty()) return emptyList()
        val ids = tagDao.getCrossRefsForFoods(foodItemIds).map { it.tagId }.toSet()
        return tagDao.getAllOnce().filter { it.id in ids }
    }

    /** Finds an existing tag by name (case-insensitive) or creates it; blank names are skipped. */
    private suspend fun findOrCreateTagIds(names: List<String>): List<String> =
        names.map { it.trim() }.filter { it.isNotBlank() }.distinct().map { name ->
            tagDao.getByName(name)?.id ?: IdGenerator.newId().also { newId ->
                tagDao.upsert(Tag(id = newId, name = name, createdAt = Instant.now()))
            }
        }

    suspend fun setFoodTagsByName(foodItemId: String, names: List<String>) {
        tagDao.replaceFoodTags(foodItemId, findOrCreateTagIds(names))
    }

    /**
     * Creates the tag, or returns null when a tag of that name already exists (case-insensitive) —
     * `tags` carries a unique index on `name`, so an unchecked insert would throw rather than
     * quietly merge. The Tags tab turns the null into a message instead.
     */
    suspend fun createTag(name: String, colorArgb: Int? = null): Tag? {
        val trimmed = name.trim()
        if (trimmed.isBlank() || tagDao.getByName(trimmed) != null) return null
        val tag = Tag(
            id = IdGenerator.newId(),
            name = trimmed,
            createdAt = Instant.now(),
            colorArgb = colorArgb,
        )
        tagDao.upsert(tag)
        return tag
    }

    /**
     * Renames and/or recolours an existing tag. Returns false when the new name is blank or already
     * belongs to a *different* tag; keeping its own name is always allowed, so a pure colour change
     * never trips the check.
     */
    suspend fun updateTag(tag: Tag, name: String, colorArgb: Int?): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val clash = tagDao.getByName(trimmed)
        if (clash != null && clash.id != tag.id) return false
        tagDao.upsert(tag.copy(name = trimmed, colorArgb = colorArgb))
        return true
    }

    /** How many Lebensmittel would lose this tag — the Tags tab says so before deleting. */
    suspend fun tagUsageCount(tagId: String): Int = tagDao.usageCount(tagId)

    /** The `food_item_tags` and `tag_implications` rows cascade with the tag. */
    suspend fun deleteTag(tag: Tag) {
        tagDao.delete(tag)
    }

    /**
     * Declares "[childTagId] implies [parentTagId]". Returns false when that would close a loop,
     * which would otherwise make [tagFilterClosure] answer with the whole cycle for every member.
     */
    suspend fun addImplication(childTagId: String, parentTagId: String): Boolean {
        if (wouldCreateCycle(childTagId, parentTagId, tagDao.getAllImplicationsOnce())) return false
        tagDao.upsertImplication(TagImplication(childTagId = childTagId, parentTagId = parentTagId))
        return true
    }

    suspend fun removeImplication(childTagId: String, parentTagId: String) {
        tagDao.deleteImplication(childTagId, parentTagId)
    }
}
