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
}
