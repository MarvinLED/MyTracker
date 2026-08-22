package com.example.mytracker.nutrition.recipe

import com.example.mytracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface RecipeDiModule {
    @Binds
    @IntoSet
    fun bindRecipeLibraryExportProvider(impl: RecipeLibraryExportProvider): BackupExportProvider
}
