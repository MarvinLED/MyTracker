package com.example.mytracker.nutrition.diary

import com.example.mytracker.nutrition.food.BaseUnit
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagImplication
import com.example.mytracker.nutrition.recipe.Recipe
import com.example.mytracker.nutrition.recipe.RecipeWithNutrition
import com.example.mytracker.nutrition.NutritionTotals
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryPickerItemTest {
    private val now = Instant.now()
    private val tag1 = Tag(id = "tag-1", name = "Breakfast", createdAt = now)
    private val tag2 = Tag(id = "tag-2", name = "Protein", createdAt = now)

    private val food1 = FoodItem(
        id = "food-1",
        name = "Banana",
        baseUnit = BaseUnit.G,
        kcalPer100 = 89.0,
        proteinPer100 = 1.1,
        carbsPer100 = 23.0,
        fatPer100 = 0.3,
        createdAt = now,
        updatedAt = now,
    )

    private val food2 = FoodItem(
        id = "food-2",
        name = "Apple",
        baseUnit = BaseUnit.G,
        kcalPer100 = 52.0,
        proteinPer100 = 0.3,
        carbsPer100 = 14.0,
        fatPer100 = 0.2,
        createdAt = now,
        updatedAt = now,
    )

    private val recipe1 = RecipeWithNutrition(
        recipe = Recipe(id = "recipe-1", name = "Oatmeal", servings = 2.0, createdAt = now, updatedAt = now),
        ingredients = emptyList(),
        total = NutritionTotals(kcal = 200.0, protein = 10.0, carbs = 30.0, fat = 5.0),
        perServing = NutritionTotals(kcal = 100.0, protein = 5.0, carbs = 15.0, fat = 2.5),
        tags = listOf(tag1),
    )

    @Test
    fun filteredForPicker_modeFood_excludesRecipes() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Food(food2, listOf(tag2)),
            DiaryPickerItem.Recipe(recipe1),
        )

        val result = items.filteredForPicker(DiaryPickerMode.FOOD, null)

        assertEquals(2, result.size)
        assertTrue(result.all { it.sourceType == DiarySourceType.FOOD })
    }

    @Test
    fun filteredForPicker_modeRecipe_excludesFoods() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Recipe(recipe1),
        )

        val result = items.filteredForPicker(DiaryPickerMode.RECIPE, null)

        assertEquals(1, result.size)
        assertTrue(result.first().sourceType == DiarySourceType.RECIPE)
    }

    @Test
    fun filteredForPicker_modeAll_includesAll() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Recipe(recipe1),
        )

        val result = items.filteredForPicker(DiaryPickerMode.ALL, null)

        assertEquals(2, result.size)
    }

    @Test
    fun filteredForPicker_byTag_excludesUntaggedItems() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Food(food2, listOf(tag2)),
            DiaryPickerItem.Recipe(recipe1),  // has tag1
        )

        val result = items.filteredForPicker(DiaryPickerMode.ALL, tag1.id)

        assertEquals(2, result.size)
        assertTrue(result.all { it.tags.any { t -> t.id == tag1.id } })
    }

    @Test
    fun filteredForPicker_byTagThatNoItemHas() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Recipe(recipe1),
        )

        val result = items.filteredForPicker(DiaryPickerMode.ALL, "tag-nonexistent")

        assertEquals(0, result.size)
    }

    @Test
    fun filteredForPicker_byTag_widensToTagsThatImplyIt() {
        // vegan ⇒ vegetarisch: asking for vegetarisch has to turn up the vegan-only food too.
        val vegetarisch = Tag(id = "vegetarisch", name = "vegetarisch", createdAt = now)
        val vegan = Tag(id = "vegan", name = "vegan", createdAt = now)
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(vegan)),
            DiaryPickerItem.Food(food2, listOf(vegetarisch)),
            DiaryPickerItem.Recipe(recipe1),  // has tag1 only
        )
        val implications = listOf(TagImplication(childTagId = vegan.id, parentTagId = vegetarisch.id))

        val result = items.filteredForPicker(DiaryPickerMode.ALL, vegetarisch.id, implications)

        assertEquals(setOf("Banana", "Apple"), result.map { it.name }.toSet())
    }

    @Test
    fun filteredForPicker_byTag_doesNotWidenTheOtherWay() {
        // Not every vegetarian food is vegan — filtering the narrower tag must stay narrow.
        val vegetarisch = Tag(id = "vegetarisch", name = "vegetarisch", createdAt = now)
        val vegan = Tag(id = "vegan", name = "vegan", createdAt = now)
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(vegan)),
            DiaryPickerItem.Food(food2, listOf(vegetarisch)),
        )
        val implications = listOf(TagImplication(childTagId = vegan.id, parentTagId = vegetarisch.id))

        val result = items.filteredForPicker(DiaryPickerMode.ALL, vegan.id, implications)

        assertEquals(listOf("Banana"), result.map { it.name })
    }

    @Test
    fun filteredForPicker_byTag_ignoresImplicationsOfOtherTags() {
        // An unrelated implication in the graph must not widen this filter at all.
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Food(food2, listOf(tag2)),
        )
        val implications = listOf(TagImplication(childTagId = "vegan", parentTagId = "vegetarisch"))

        val result = items.filteredForPicker(DiaryPickerMode.ALL, tag1.id, implications)

        assertEquals(listOf("Banana"), result.map { it.name })
    }

    @Test
    fun sortedForPicker_nameSort_sortsAlphabetically() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),  // Banana
            DiaryPickerItem.Food(food2, listOf(tag2)),  // Apple
            DiaryPickerItem.Recipe(recipe1),  // Oatmeal
        )

        val result = items.sortedForPicker(DiaryPickerSort.NAME, MealType.BREAKFAST, emptyMap())

        assertEquals("Apple", result[0].name)
        assertEquals("Banana", result[1].name)
        assertEquals("Oatmeal", result[2].name)
    }

    @Test
    fun sortedForPicker_lastEatenSort_tier0SameMethodType() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Food(food2, listOf(tag2)),
        )
        val lastLogged = mapOf(
            DiarySourceType.FOOD to "food-1" to LastLoggedSource(
                sourceType = DiarySourceType.FOOD,
                sourceId = "food-1",
                mealType = MealType.BREAKFAST,
                epochDay = 100L,
                createdAt = now,
            ),
            DiarySourceType.FOOD to "food-2" to LastLoggedSource(
                sourceType = DiarySourceType.FOOD,
                sourceId = "food-2",
                mealType = MealType.LUNCH,  // Different meal type
                epochDay = 100L,
                createdAt = now,
            ),
        )

        val result = items.sortedForPicker(DiaryPickerSort.LAST_EATEN, MealType.BREAKFAST, lastLogged)

        // food-1 (BREAKFAST) should come before food-2 (LUNCH)
        assertEquals("Banana", result[0].name)
        assertEquals("Apple", result[1].name)
    }

    @Test
    fun sortedForPicker_lastEatenSort_neverLoggedLast() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),  // never logged
            DiaryPickerItem.Food(food2, listOf(tag2)),  // logged before
        )
        val lastLogged = mapOf(
            DiarySourceType.FOOD to "food-2" to LastLoggedSource(
                sourceType = DiarySourceType.FOOD,
                sourceId = "food-2",
                mealType = MealType.BREAKFAST,
                epochDay = 100L,
                createdAt = now,
            ),
        )

        val result = items.sortedForPicker(DiaryPickerSort.LAST_EATEN, MealType.BREAKFAST, lastLogged)

        // food-2 (logged) should come before food-1 (never logged)
        assertEquals("Apple", result[0].name)
        assertEquals("Banana", result[1].name)
    }

    @Test
    fun sortedForPicker_lastEatenSort_recencyOrder() {
        val items = listOf(
            DiaryPickerItem.Food(food1, listOf(tag1)),
            DiaryPickerItem.Food(food2, listOf(tag2)),
        )
        val now1 = Instant.now()
        val now2 = now1.minusSeconds(3600)  // 1 hour earlier
        val lastLogged = mapOf(
            DiarySourceType.FOOD to "food-1" to LastLoggedSource(
                sourceType = DiarySourceType.FOOD,
                sourceId = "food-1",
                mealType = MealType.BREAKFAST,
                epochDay = 100L,
                createdAt = now1,  // More recent
            ),
            DiarySourceType.FOOD to "food-2" to LastLoggedSource(
                sourceType = DiarySourceType.FOOD,
                sourceId = "food-2",
                mealType = MealType.BREAKFAST,
                epochDay = 100L,
                createdAt = now2,  // Less recent
            ),
        )

        val result = items.sortedForPicker(DiaryPickerSort.LAST_EATEN, MealType.BREAKFAST, lastLogged)

        // food-1 (more recent) should come before food-2
        assertEquals("Banana", result[0].name)
        assertEquals("Apple", result[1].name)
    }
}
