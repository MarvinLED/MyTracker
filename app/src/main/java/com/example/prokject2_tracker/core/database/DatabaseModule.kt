package com.example.prokject2_tracker.core.database

import android.content.Context
import androidx.room.Room
import com.example.prokject2_tracker.fitness.cardio.CardioDao
import com.example.prokject2_tracker.fluid.FluidDao
import com.example.prokject2_tracker.nutrition.diary.DiaryDao
import com.example.prokject2_tracker.nutrition.food.FoodDao
import com.example.prokject2_tracker.nutrition.recipe.RecipeDao
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
            // Pre-release: schema changes freely without bumping `version`. Destructively
            // recreate on any mismatch instead of crashing; switch to real migrations once shipped.
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()

    @Provides
    fun provideFoodDao(database: AppDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideRecipeDao(database: AppDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideDiaryDao(database: AppDatabase): DiaryDao = database.diaryDao()

    @Provides
    fun provideFluidDao(database: AppDatabase): FluidDao = database.fluidDao()

    @Provides
    fun provideCardioDao(database: AppDatabase): CardioDao = database.cardioDao()
}
