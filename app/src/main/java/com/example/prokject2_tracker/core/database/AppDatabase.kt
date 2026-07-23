package com.example.prokject2_tracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.prokject2_tracker.fitness.cardio.CardioDao
import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.StrengthExercise
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseDao
import com.example.prokject2_tracker.fitness.strength.StrengthLogDao
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fluid.FluidDao
import com.example.prokject2_tracker.fluid.FluidEntry
import com.example.prokject2_tracker.fluid.FluidType
import com.example.prokject2_tracker.fluid.FluidTypeDao
import com.example.prokject2_tracker.habit.Habit
import com.example.prokject2_tracker.habit.HabitCheckIn
import com.example.prokject2_tracker.habit.HabitCheckInDao
import com.example.prokject2_tracker.habit.HabitDao
import com.example.prokject2_tracker.nutrition.diary.DiaryDao
import com.example.prokject2_tracker.nutrition.diary.DiaryEntry
import com.example.prokject2_tracker.nutrition.food.FoodDao
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.recipe.Recipe
import com.example.prokject2_tracker.nutrition.recipe.RecipeDao
import com.example.prokject2_tracker.nutrition.recipe.RecipeIngredient

@Database(
    entities = [
        FoodItem::class,
        Recipe::class,
        RecipeIngredient::class,
        DiaryEntry::class,
        FluidEntry::class,
        FluidType::class,
        CardioSession::class,
        StrengthExercise::class,
        StrengthLogEntry::class,
        Habit::class,
        HabitCheckIn::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao
    abstract fun diaryDao(): DiaryDao
    abstract fun fluidDao(): FluidDao
    abstract fun fluidTypeDao(): FluidTypeDao
    abstract fun cardioDao(): CardioDao
    abstract fun strengthExerciseDao(): StrengthExerciseDao
    abstract fun strengthLogDao(): StrengthLogDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCheckInDao(): HabitCheckInDao
}
