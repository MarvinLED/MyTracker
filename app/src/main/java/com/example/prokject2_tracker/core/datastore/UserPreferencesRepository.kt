package com.example.prokject2_tracker.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

enum class WeightUnit { KG, LB }

data class UserPreferences(
    val dailyCalorieGoalKcal: Double,
    val dailyWaterGoalMl: Double,
    val weightUnit: WeightUnit,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val CALORIE_GOAL = doublePreferencesKey("daily_calorie_goal_kcal")
        val WATER_GOAL = doublePreferencesKey("daily_water_goal_ml")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    }

    val userPreferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            dailyCalorieGoalKcal = prefs[Keys.CALORIE_GOAL] ?: 2000.0,
            dailyWaterGoalMl = prefs[Keys.WATER_GOAL] ?: 2000.0,
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { WeightUnit.valueOf(it) } ?: WeightUnit.KG,
        )
    }

    suspend fun setDailyCalorieGoal(kcal: Double) {
        context.userPreferencesDataStore.edit { it[Keys.CALORIE_GOAL] = kcal }
    }

    suspend fun setDailyWaterGoal(ml: Double) {
        context.userPreferencesDataStore.edit { it[Keys.WATER_GOAL] = ml }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.userPreferencesDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }
}
