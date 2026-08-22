package com.example.mytracker.nutrition.recipe

import kotlinx.serialization.Serializable

@Serializable
data class RecipeEditRoute(val recipeId: String? = null)
