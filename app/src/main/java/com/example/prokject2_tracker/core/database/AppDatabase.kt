package com.example.prokject2_tracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.prokject2_tracker.bloodpressure.BloodPressureDao
import com.example.prokject2_tracker.bloodpressure.BloodPressureEntry
import com.example.prokject2_tracker.fitness.FitnessGoal
import com.example.prokject2_tracker.fitness.FitnessGoalDao
import com.example.prokject2_tracker.fitness.cardio.CardioActivityType
import com.example.prokject2_tracker.fitness.cardio.CardioActivityTypeDao
import com.example.prokject2_tracker.fitness.cardio.CardioDao
import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.MuscleGroup
import com.example.prokject2_tracker.fitness.strength.MuscleGroupDao
import com.example.prokject2_tracker.fitness.strength.StrengthExercise
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseDao
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseMuscleGroup
import com.example.prokject2_tracker.fitness.strength.StrengthLogDao
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fitness.strength.StrengthSet
import com.example.prokject2_tracker.fitness.strength.StrengthSetDao
import com.example.prokject2_tracker.fluid.FluidDao
import com.example.prokject2_tracker.fluid.FluidEntry
import com.example.prokject2_tracker.fluid.FluidQuickAdd
import com.example.prokject2_tracker.fluid.FluidQuickAddDao
import com.example.prokject2_tracker.fluid.FluidType
import com.example.prokject2_tracker.fluid.FluidTypeDao
import com.example.prokject2_tracker.fluid.FluidUnit
import com.example.prokject2_tracker.fluid.FluidUnitDao
import com.example.prokject2_tracker.habit.Habit
import com.example.prokject2_tracker.habit.HabitCheckIn
import com.example.prokject2_tracker.habit.HabitCheckInDao
import com.example.prokject2_tracker.habit.HabitDao
import com.example.prokject2_tracker.habit.HabitGoal
import com.example.prokject2_tracker.habit.HabitGoalDao
import com.example.prokject2_tracker.measurement.BodyMeasurement
import com.example.prokject2_tracker.measurement.BodyMeasurementDao
import com.example.prokject2_tracker.measurement.BodySite
import com.example.prokject2_tracker.measurement.BodySiteDao
import com.example.prokject2_tracker.nutrition.diary.DiaryDao
import com.example.prokject2_tracker.nutrition.diary.DiaryEntry
import com.example.prokject2_tracker.nutrition.diary.DiaryRecipeIngredient
import com.example.prokject2_tracker.nutrition.food.FoodDao
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.FoodItemTag
import com.example.prokject2_tracker.nutrition.food.FoodUnit
import com.example.prokject2_tracker.nutrition.food.FoodUnitDao
import com.example.prokject2_tracker.nutrition.food.Tag
import com.example.prokject2_tracker.nutrition.food.TagDao
import com.example.prokject2_tracker.nutrition.recipe.Recipe
import com.example.prokject2_tracker.nutrition.recipe.RecipeDao
import com.example.prokject2_tracker.nutrition.recipe.RecipeIngredient
import com.example.prokject2_tracker.sleep.NapEntry
import com.example.prokject2_tracker.sleep.SleepDao
import com.example.prokject2_tracker.sleep.SleepEntry
import com.example.prokject2_tracker.sleep.SleepEntryTag
import com.example.prokject2_tracker.sleep.SleepTag
import com.example.prokject2_tracker.sleep.SleepTagDao
import com.example.prokject2_tracker.task.Task
import com.example.prokject2_tracker.task.TaskCompletion
import com.example.prokject2_tracker.task.TaskCompletionDao
import com.example.prokject2_tracker.task.TaskDao
import com.example.prokject2_tracker.weight.BodyWeightDao
import com.example.prokject2_tracker.weight.BodyWeightEntry

@Database(
    entities = [
        FoodItem::class,
        FoodUnit::class,
        Recipe::class,
        RecipeIngredient::class,
        DiaryEntry::class,
        DiaryRecipeIngredient::class,
        FluidEntry::class,
        FluidType::class,
        FluidUnit::class,
        FluidQuickAdd::class,
        CardioSession::class,
        CardioActivityType::class,
        StrengthExercise::class,
        StrengthLogEntry::class,
        StrengthSet::class,
        MuscleGroup::class,
        StrengthExerciseMuscleGroup::class,
        Habit::class,
        HabitCheckIn::class,
        HabitGoal::class,
        Tag::class,
        FoodItemTag::class,
        BodyWeightEntry::class,
        FitnessGoal::class,
        BodySite::class,
        BodyMeasurement::class,
        BloodPressureEntry::class,
        SleepEntry::class,
        NapEntry::class,
        SleepTag::class,
        SleepEntryTag::class,
        Task::class,
        TaskCompletion::class,
    ],
    version = 22,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun foodUnitDao(): FoodUnitDao
    abstract fun recipeDao(): RecipeDao
    abstract fun diaryDao(): DiaryDao
    abstract fun fluidDao(): FluidDao
    abstract fun fluidTypeDao(): FluidTypeDao
    abstract fun fluidUnitDao(): FluidUnitDao
    abstract fun fluidQuickAddDao(): FluidQuickAddDao
    abstract fun cardioDao(): CardioDao
    abstract fun cardioActivityTypeDao(): CardioActivityTypeDao
    abstract fun strengthExerciseDao(): StrengthExerciseDao
    abstract fun strengthLogDao(): StrengthLogDao
    abstract fun strengthSetDao(): StrengthSetDao
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCheckInDao(): HabitCheckInDao
    abstract fun habitGoalDao(): HabitGoalDao
    abstract fun tagDao(): TagDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun fitnessGoalDao(): FitnessGoalDao
    abstract fun bodySiteDao(): BodySiteDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun sleepDao(): SleepDao
    abstract fun sleepTagDao(): SleepTagDao
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
}
