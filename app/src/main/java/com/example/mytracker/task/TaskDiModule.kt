package com.example.mytracker.task

import com.example.mytracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Two providers, one per [com.example.mytracker.core.backup.BackupScope]: an Aufgabe and its
 * rhythm are something the user built, when it was ticked off is tracked data.
 */
@Module
@InstallIn(SingletonComponent::class)
interface TaskDiModule {
    @Binds
    @IntoSet
    fun bindTaskLibraryExportProvider(impl: TaskLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindTaskCompletionExportProvider(impl: TaskCompletionExportProvider): BackupExportProvider
}
