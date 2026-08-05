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

    /** Get all parent tags of a tag, recursively traversing the hierarchy. */
    private suspend fun getAllParentTagIds(childTagId: String): Set<String> {
        val parents = mutableSetOf<String>()
        val toVisit = mutableListOf(childTagId)
        val visited = mutableSetOf<String>()

        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeAt(0)
            if (current in visited) continue
            visited.add(current)

            val hierarchy = tagDao.getHierarchyForTag(current)
            hierarchy.forEach { h ->
                if (h.childTagId == current && h.parentTagId !in visited) {
                    parents.add(h.parentTagId)
                    toVisit.add(h.parentTagId)
                }
            }
        }

        return parents
    }

    /** Get all tags that should match in search (includes all parent tags). */
    suspend fun expandTagsWithParents(tagIds: Set<String>): Set<String> {
        val expanded = tagIds.toMutableSet()
        for (tagId in tagIds) {
            expanded.addAll(getAllParentTagIds(tagId))
        }
        return expanded
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

    suspend fun updateTagName(tagId: String, newName: String) {
        tagDao.updateTagName(tagId, newName)
    }

    suspend fun addTagHierarchy(parentTagId: String, childTagId: String) {
        tagDao.insertHierarchy(TagHierarchy(parentTagId, childTagId))
    }

    suspend fun removeTagHierarchy(parentTagId: String, childTagId: String) {
        tagDao.deleteHierarchy(parentTagId, childTagId)
    }

    /** Filter tags to show only the most specific ones. If a tag is a parent of another in the list, exclude it. */
    private fun filterToMostSpecificTags(tags: List<Tag>, hierarchy: List<TagHierarchy>): List<Tag> {
        if (tags.isEmpty()) return emptyList()

        val tagIds = tags.map { it.id }.toSet()
        val tagsToExclude = mutableSetOf<String>()

        hierarchy.forEach { relation ->
            if (relation.parentTagId in tagIds && relation.childTagId in tagIds) {
                tagsToExclude.add(relation.parentTagId)
            }
        }

        return tags.filter { it.id !in tagsToExclude }
    }
}
