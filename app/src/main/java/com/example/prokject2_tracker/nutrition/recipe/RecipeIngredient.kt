package com.example.prokject2_tracker.nutrition.recipe

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.prokject2_tracker.nutrition.food.FoodItem

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
    /** Amount in the referenced food's [com.example.prokject2_tracker.nutrition.food.BaseUnit]. */
    val amountBaseUnits: Double,
    val sortOrder: Int = 0,
)
