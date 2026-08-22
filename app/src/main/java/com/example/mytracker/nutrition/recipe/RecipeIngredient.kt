package com.example.mytracker.nutrition.recipe

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mytracker.nutrition.food.FoodItem

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("recipeId"), Index("foodId")],
)
data class RecipeIngredient(
    @PrimaryKey val id: String,
    val recipeId: String,
    val foodId: String,
    /** Amount in the referenced food's [com.example.mytracker.nutrition.food.BaseUnit]. */
    val amountBaseUnits: Double,
    /**
     * How the amount was entered, if by a named
     * [FoodUnit][com.example.mytracker.nutrition.food.FoodUnit] instead of by weight —
     * "2 × Scheibe" for an [amountBaseUnits] of 50. Null means grams were typed directly.
     */
    val unitName: String? = null,
    val unitCount: Double? = null,
    val sortOrder: Int = 0,
)
