package com.example.mytracker.bloodpressure

import com.example.mytracker.core.backup.BackupExportProvider
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
