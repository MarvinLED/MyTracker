package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface FoodDiModule {
    @Binds
    @IntoSet
    fun bindFoodLibraryExportProvider(impl: FoodLibraryExportProvider): LibraryExportProvider
}
