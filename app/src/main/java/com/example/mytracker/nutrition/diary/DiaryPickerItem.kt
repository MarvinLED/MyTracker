package com.example.mytracker.nutrition.diary

import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagImplication
import com.example.mytracker.nutrition.food.tagFilterClosure
import com.example.mytracker.nutrition.recipe.RecipeWithNutrition

sealed class DiaryPickerItem {
    abstract val id: String
    abstract val sourceType: DiarySourceType
    abstract val name: String
    abstract val tags: List<Tag>

    data class Food(val food: FoodItem, override val tags: List<Tag>) : DiaryPickerItem() {
        override val id get() = food.id
        override val sourceType get() = DiarySourceType.FOOD
        override val name get() = food.name
    }

    data class Recipe(val recipe: RecipeWithNutrition) : DiaryPickerItem() {
        override val id get() = recipe.recipe.id
        override val sourceType get() = DiarySourceType.RECIPE
        override val name get() = recipe.recipe.name
        override val tags get() = recipe.tags
    }
}

/**
 * [implications] widens the tag filter downwards: picking "vegetarisch" also keeps items that only
 * carry "vegan", because vegan implies vegetarisch. Only the filter widens — an item's own
 * [DiaryPickerItem.tags] stay exactly what the user assigned, so nothing grows a label it was never
 * given.
 */
fun List<DiaryPickerItem>.filteredForPicker(
    mode: DiaryPickerMode,
    tagId: String?,
    implications: List<TagImplication> = emptyList(),
): List<DiaryPickerItem> {
    var result = this

    when (mode) {
        DiaryPickerMode.FOOD -> result = result.filterIsInstance<DiaryPickerItem.Food>()
        DiaryPickerMode.RECIPE -> result = result.filterIsInstance<DiaryPickerItem.Recipe>()
        DiaryPickerMode.ALL, DiaryPickerMode.QUICK -> Unit
    }

    if (tagId != null) {
        val matching = tagFilterClosure(tagId, implications)
        result = result.filter { item ->
            item.tags.any { it.id in matching }
        }
    }

    return result
}

fun List<DiaryPickerItem>.sortedForPicker(
    sort: DiaryPickerSort,
    currentMealType: MealType,
    lastLogged: Map<Pair<DiarySourceType, String>, LastLoggedSource>,
): List<DiaryPickerItem> {
    return when (sort) {
        DiaryPickerSort.NAME -> sortedBy { it.name }
        DiaryPickerSort.LAST_EATEN -> {
            val grouped = groupBy { item ->
                val lastLog = lastLogged[item.sourceType to item.id]
                when {
                    lastLog == null -> 2
                    lastLog.mealType == currentMealType -> 0
                    else -> 1
                }
            }

            val tier0 = (grouped[0] ?: emptyList()).sortedWith(
                compareBy<DiaryPickerItem> { item ->
                    -((lastLogged[item.sourceType to item.id]?.epochDay) ?: 0L)
                }.thenBy { item ->
                    -((lastLogged[item.sourceType to item.id]?.createdAt?.toEpochMilli()) ?: 0L)
                }
            )

            val tier1 = (grouped[1] ?: emptyList()).sortedWith(
                compareBy<DiaryPickerItem> { item ->
                    -((lastLogged[item.sourceType to item.id]?.epochDay) ?: 0L)
                }.thenBy { item ->
                    -((lastLogged[item.sourceType to item.id]?.createdAt?.toEpochMilli()) ?: 0L)
                }
            )

            val tier2 = (grouped[2] ?: emptyList()).sortedBy { it.name }

            tier0 + tier1 + tier2
        }
    }
}
