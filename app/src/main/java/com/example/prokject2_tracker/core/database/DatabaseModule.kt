package com.example.prokject2_tracker.core.database

import android.content.Context
import androidx.room.Room
import com.example.prokject2_tracker.bloodpressure.BloodPressureDao
import com.example.prokject2_tracker.fitness.FitnessGoalDao
import com.example.prokject2_tracker.fitness.cardio.CardioActivityTypeDao
import com.example.prokject2_tracker.fitness.cardio.CardioDao
import com.example.prokject2_tracker.fitness.strength.MuscleGroupDao
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseDao
import com.example.prokject2_tracker.fitness.strength.StrengthLogDao
import com.example.prokject2_tracker.fitness.strength.StrengthSetDao
import com.example.prokject2_tracker.fluid.FluidDao
import com.example.prokject2_tracker.fluid.FluidTypeDao
import com.example.prokject2_tracker.fluid.FluidQuickAddDao
import com.example.prokject2_tracker.fluid.FluidUnitDao
import com.example.prokject2_tracker.habit.HabitCheckInDao
import com.example.prokject2_tracker.habit.HabitDao
import com.example.prokject2_tracker.habit.HabitGoalDao
import com.example.prokject2_tracker.measurement.BodyMeasurementDao
import com.example.prokject2_tracker.measurement.BodySiteDao
import com.example.prokject2_tracker.nutrition.diary.DiaryDao
import com.example.prokject2_tracker.nutrition.food.FoodDao
import com.example.prokject2_tracker.nutrition.food.FoodUnitDao
import com.example.prokject2_tracker.nutrition.food.TagDao
import com.example.prokject2_tracker.nutrition.recipe.RecipeDao
import com.example.prokject2_tracker.sleep.SleepDao
import com.example.prokject2_tracker.sleep.SleepTagDao
import com.example.prokject2_tracker.task.TaskCompletionDao
import com.example.prokject2_tracker.task.TaskDao
import com.example.prokject2_tracker.weight.BodyWeightDao
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
}
