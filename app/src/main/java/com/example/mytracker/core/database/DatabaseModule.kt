package com.example.mytracker.core.database

import android.content.Context
import androidx.room.Room
import com.example.mytracker.achievements.GameDayPointsDao
import com.example.mytracker.bloodpressure.BloodPressureDao
import com.example.mytracker.fitness.FitnessGoalDao
import com.example.mytracker.fitness.FitnessGoalChangeDao
import com.example.mytracker.fitness.StrengthMaxWeightGoalDao
import com.example.mytracker.fitness.cardio.CardioActivityTypeDao
import com.example.mytracker.fitness.cardio.CardioDao
import com.example.mytracker.fitness.strength.MuscleGroupDao
import com.example.mytracker.fitness.strength.StrengthExerciseDao
import com.example.mytracker.fitness.strength.StrengthLogDao
import com.example.mytracker.fitness.strength.StrengthSetDao
import com.example.mytracker.fluid.FluidDao
import com.example.mytracker.fluid.FluidTypeDao
import com.example.mytracker.fluid.FluidQuickAddDao
import com.example.mytracker.fluid.FluidUnitDao
import com.example.mytracker.goals.NutrientGoalChangeDao
import com.example.mytracker.habit.HabitCheckInDao
import com.example.mytracker.habit.HabitDao
import com.example.mytracker.habit.HabitGoalDao
import com.example.mytracker.measurement.BodyMeasurementDao
import com.example.mytracker.measurement.BodySiteDao
import com.example.mytracker.nutrition.diary.DiaryDao
import com.example.mytracker.nutrition.food.FoodDao
import com.example.mytracker.nutrition.food.FoodUnitDao
import com.example.mytracker.nutrition.food.TagDao
import com.example.mytracker.nutrition.recipe.RecipeDao
import com.example.mytracker.sleep.SleepDao
import com.example.mytracker.sleep.SleepTagDao
import com.example.mytracker.task.TaskCompletionDao
import com.example.mytracker.task.TaskDao
import com.example.mytracker.weight.BodyWeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "prokject2_tracker.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22,
                MIGRATION_22_23,
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28,
            )
            // Upgrades now always go through a real, data-preserving Migration above — a missing
            // migration crashes loudly in development instead of silently wiping a real user's
            // data again. Downgrades only happen when sideloading an older debug APK over a newer
            // one (dev-only, data loss already expected there), so that fallback is kept.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
            .build()

    @Provides
    fun provideFoodDao(database: AppDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideFoodUnitDao(database: AppDatabase): FoodUnitDao = database.foodUnitDao()

    @Provides
    fun provideRecipeDao(database: AppDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideDiaryDao(database: AppDatabase): DiaryDao = database.diaryDao()

    @Provides
    fun provideFluidDao(database: AppDatabase): FluidDao = database.fluidDao()

    @Provides
    fun provideFluidTypeDao(database: AppDatabase): FluidTypeDao = database.fluidTypeDao()

    @Provides
    fun provideFluidUnitDao(database: AppDatabase): FluidUnitDao = database.fluidUnitDao()

    @Provides
    fun provideFluidQuickAddDao(database: AppDatabase): FluidQuickAddDao = database.fluidQuickAddDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideCardioDao(database: AppDatabase): CardioDao = database.cardioDao()

    @Provides
    fun provideCardioActivityTypeDao(database: AppDatabase): CardioActivityTypeDao = database.cardioActivityTypeDao()

    @Provides
    fun provideStrengthExerciseDao(database: AppDatabase): StrengthExerciseDao = database.strengthExerciseDao()

    @Provides
    fun provideStrengthLogDao(database: AppDatabase): StrengthLogDao = database.strengthLogDao()

    @Provides
    fun provideStrengthSetDao(database: AppDatabase): StrengthSetDao = database.strengthSetDao()

    @Provides
    fun provideMuscleGroupDao(database: AppDatabase): MuscleGroupDao = database.muscleGroupDao()

    @Provides
    fun provideHabitDao(database: AppDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideHabitCheckInDao(database: AppDatabase): HabitCheckInDao = database.habitCheckInDao()

    @Provides
    fun provideHabitGoalDao(database: AppDatabase): HabitGoalDao = database.habitGoalDao()

    @Provides
    fun provideBodyWeightDao(database: AppDatabase): BodyWeightDao = database.bodyWeightDao()

    @Provides
    fun provideFitnessGoalDao(database: AppDatabase): FitnessGoalDao = database.fitnessGoalDao()

    @Provides
    fun provideStrengthMaxWeightGoalDao(database: AppDatabase): StrengthMaxWeightGoalDao =
        database.strengthMaxWeightGoalDao()

    @Provides
    fun provideFitnessGoalChangeDao(database: AppDatabase): FitnessGoalChangeDao = database.fitnessGoalChangeDao()

    @Provides
    fun provideBodySiteDao(database: AppDatabase): BodySiteDao = database.bodySiteDao()

    @Provides
    fun provideBodyMeasurementDao(database: AppDatabase): BodyMeasurementDao = database.bodyMeasurementDao()

    @Provides
    fun provideBloodPressureDao(database: AppDatabase): BloodPressureDao = database.bloodPressureDao()

    @Provides
    fun provideSleepDao(database: AppDatabase): SleepDao = database.sleepDao()

    @Provides
    fun provideSleepTagDao(database: AppDatabase): SleepTagDao = database.sleepTagDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskCompletionDao(database: AppDatabase): TaskCompletionDao = database.taskCompletionDao()

    @Provides
    fun provideNutrientGoalChangeDao(database: AppDatabase): NutrientGoalChangeDao =
        database.nutrientGoalChangeDao()

    @Provides
    fun provideGameDayPointsDao(database: AppDatabase): GameDayPointsDao = database.gameDayPointsDao()
}
