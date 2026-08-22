package com.example.mytracker.nutrition.recipe

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String,
    val name: String,
    /** Double, not Int, to allow fractional yields (e.g. a half-batch). */
    val servings: Double,
    val instructions: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
