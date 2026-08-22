package com.example.mytracker.nutrition.food

import com.example.mytracker.core.backup.BackupExportProvider
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
    fun bindFoodLibraryExportProvider(impl: FoodLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindTagLibraryExportProvider(impl: TagLibraryExportProvider): BackupExportProvider
}
