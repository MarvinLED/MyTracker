package com.example.mytracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mytracker.achievements.GameDayPoints
import com.example.mytracker.achievements.GameDayPointsDao
import com.example.mytracker.bloodpressure.BloodPressureDao
import com.example.mytracker.bloodpressure.BloodPressureEntry
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalDao
import com.example.mytracker.fitness.FitnessGoalChange
import com.example.mytracker.fitness.FitnessGoalChangeDao
import com.example.mytracker.fitness.StrengthMaxWeightGoal
import com.example.mytracker.fitness.StrengthMaxWeightGoalDao
import com.example.mytracker.fitness.cardio.CardioActivityType
import com.example.mytracker.fitness.cardio.CardioActivityTypeDao
import com.example.mytracker.fitness.cardio.CardioDao
import com.example.mytracker.fitness.cardio.CardioSession
import com.example.mytracker.fitness.strength.MuscleGroup
import com.example.mytracker.fitness.strength.MuscleGroupDao
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fitness.strength.StrengthExerciseDao
import com.example.mytracker.fitness.strength.StrengthExerciseMuscleGroup
import com.example.mytracker.fitness.strength.StrengthLogDao
import com.example.mytracker.fitness.strength.StrengthLogEntry
import com.example.mytracker.fitness.strength.StrengthSet
import com.example.mytracker.fitness.strength.StrengthSetDao
import com.example.mytracker.fluid.FluidDao
import com.example.mytracker.fluid.FluidEntry
import com.example.mytracker.fluid.FluidQuickAdd
import com.example.mytracker.fluid.FluidQuickAddDao
import com.example.mytracker.fluid.FluidType
import com.example.mytracker.fluid.FluidTypeDao
import com.example.mytracker.fluid.FluidUnit
import com.example.mytracker.fluid.FluidUnitDao
import com.example.mytracker.goals.NutrientGoalChange
import com.example.mytracker.goals.NutrientGoalChangeDao
import com.example.mytracker.habit.Habit
import com.example.mytracker.habit.HabitCheckIn
import com.example.mytracker.habit.HabitCheckInDao
import com.example.mytracker.habit.HabitDao
import com.example.mytracker.habit.HabitGoal
import com.example.mytracker.habit.HabitGoalDao
import com.example.mytracker.measurement.BodyMeasurement
import com.example.mytracker.measurement.BodyMeasurementDao
import com.example.mytracker.measurement.BodySite
import com.example.mytracker.measurement.BodySiteDao
import com.example.mytracker.nutrition.diary.DiaryDao
import com.example.mytracker.nutrition.diary.DiaryEntry
import com.example.mytracker.nutrition.diary.DiaryRecipeIngredient
import com.example.mytracker.nutrition.food.FoodDao
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.FoodItemTag
import com.example.mytracker.nutrition.food.FoodUnit
import com.example.mytracker.nutrition.food.FoodUnitDao
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagDao
import com.example.mytracker.nutrition.food.TagImplication
import com.example.mytracker.nutrition.recipe.Recipe
import com.example.mytracker.nutrition.recipe.RecipeDao
import com.example.mytracker.nutrition.recipe.RecipeIngredient
import com.example.mytracker.sleep.NapEntry
import com.example.mytracker.sleep.SleepDao
import com.example.mytracker.sleep.SleepEntry
import com.example.mytracker.sleep.SleepEntryTag
import com.example.mytracker.sleep.SleepTag
import com.example.mytracker.sleep.SleepTagDao
import com.example.mytracker.task.Task
import com.example.mytracker.task.TaskCompletion
import com.example.mytracker.task.TaskCompletionDao
import com.example.mytracker.task.TaskDao
import com.example.mytracker.weight.BodyWeightDao
import com.example.mytracker.weight.BodyWeightEntry

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
        TagImplication::class,
        BodyWeightEntry::class,
        FitnessGoal::class,
        StrengthMaxWeightGoal::class,
        FitnessGoalChange::class,
        BodySite::class,
        BodyMeasurement::class,
        BloodPressureEntry::class,
        SleepEntry::class,
        NapEntry::class,
        SleepTag::class,
        SleepEntryTag::class,
        Task::class,
        TaskCompletion::class,
        NutrientGoalChange::class,
        GameDayPoints::class,
    ],
    version = 29,
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
    abstract fun strengthMaxWeightGoalDao(): StrengthMaxWeightGoalDao
    abstract fun fitnessGoalChangeDao(): FitnessGoalChangeDao
    abstract fun bodySiteDao(): BodySiteDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun sleepDao(): SleepDao
    abstract fun sleepTagDao(): SleepTagDao
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun nutrientGoalChangeDao(): NutrientGoalChangeDao
    abstract fun gameDayPointsDao(): GameDayPointsDao
}
