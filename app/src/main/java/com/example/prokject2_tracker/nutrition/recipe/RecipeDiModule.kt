package com.example.prokject2_tracker.nutrition.recipe

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
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
    fun bindRecipeLibraryExportProvider(impl: RecipeLibraryExportProvider): LibraryExportProvider
}
