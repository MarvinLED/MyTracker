package com.example.mytracker.core.datastore

import com.example.mytracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface DatastoreDiModule {
    @Binds
    @IntoSet
    fun bindSettingsExportProvider(impl: SettingsExportProvider): BackupExportProvider

    @Binds
    fun bindUserPreferencesSource(impl: UserPreferencesRepository): UserPreferencesSource
}
