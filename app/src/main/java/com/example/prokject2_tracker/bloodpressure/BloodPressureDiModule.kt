package com.example.prokject2_tracker.bloodpressure

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface BloodPressureDiModule {
    @Binds
    @IntoSet
    fun bindBloodPressureExportProvider(impl: BloodPressureExportProvider): BackupExportProvider
}
