package com.example.mytracker.achievements

import com.example.mytracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface AchievementsDiModule {
    @Binds
    @IntoSet
    fun bindGamePointsExportProvider(impl: GamePointsExportProvider): BackupExportProvider
}
