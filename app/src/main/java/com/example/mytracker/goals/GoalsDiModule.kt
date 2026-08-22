package com.example.mytracker.goals

import com.example.mytracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface GoalsDiModule {
    @Binds
    @IntoSet
    fun bindGoalsExportProvider(impl: GoalsExportProvider): BackupExportProvider
}
