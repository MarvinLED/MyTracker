package com.example.prokject2_tracker.task

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Two providers, one per [com.example.prokject2_tracker.core.backup.BackupScope]: an Aufgabe and its
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
