package com.example.mytracker.nutrition.diary

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.mytracker.nutrition.FoodAmount
import com.example.mytracker.nutrition.food.FoodItem

/**
 * One ingredient of a single Tagebuch entry's *own* copy of a recipe — "the Rezept the way I actually
 * made it that day" (a spoon more rice, the cream left out).
 *
 * Rows exist only for recipe entries the user modified for that day, and then they replace the
 * library recipe's ingredient list wholesale for that one entry: nutrition and mirrored fluid come
 * from these rows instead. Without any rows the entry keeps following the library recipe as before,
 * so a modification never leaks back into the recipe or into other days.
 */
@Entity(
    tableName = "diary_recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["diaryEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("diaryEntryId"), Index("foodId")],
)
data class DiaryRecipeIngredient(
    @PrimaryKey val id: String,
    val diaryEntryId: String,
    val foodId: String,
    /** Amount in the referenced food's [com.example.mytracker.nutrition.food.BaseUnit]. */
    val amountBaseUnits: Double,
    /** How the amount was entered, if by a named unit — see [DiaryEntry.unitName]. */
    val unitName: String? = null,
    val unitCount: Double? = null,
    val sortOrder: Int = 0,
)

data class DiaryRecipeIngredientWithFood(
    @Embedded val ingredient: DiaryRecipeIngredient,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: FoodItem,
)

fun List<DiaryRecipeIngredientWithFood>.foodAmounts(): List<FoodAmount> =
    map {
        FoodAmount(
            food = it.food,
            amountBaseUnits = it.ingredient.amountBaseUnits,
            unitName = it.ingredient.unitName,
            unitCount = it.ingredient.unitCount,
        )
    }
