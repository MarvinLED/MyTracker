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
    val dailyProteinGoalG: Double? = null,
    val dailyCarbsGoalG: Double? = null,
    val dailyFatGoalG: Double? = null,
    val dailySaturatedFatGoalG: Double? = null,
    val dailySugarGoalG: Double? = null,
    val dailyFiberGoalG: Double? = null,
    val dailySaltGoalG: Double? = null,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val CALORIE_GOAL = doublePreferencesKey("daily_calorie_goal_kcal")
        val WATER_GOAL = doublePreferencesKey("daily_water_goal_ml")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val PROTEIN_GOAL = doublePreferencesKey("daily_protein_goal_g")
        val CARBS_GOAL = doublePreferencesKey("daily_carbs_goal_g")
        val FAT_GOAL = doublePreferencesKey("daily_fat_goal_g")
        val SATURATED_FAT_GOAL = doublePreferencesKey("daily_saturated_fat_goal_g")
        val SUGAR_GOAL = doublePreferencesKey("daily_sugar_goal_g")
        val FIBER_GOAL = doublePreferencesKey("daily_fiber_goal_g")
        val SALT_GOAL = doublePreferencesKey("daily_salt_goal_g")
    }

    val userPreferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            dailyCalorieGoalKcal = prefs[Keys.CALORIE_GOAL] ?: 2000.0,
            dailyWaterGoalMl = prefs[Keys.WATER_GOAL] ?: 2000.0,
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { WeightUnit.valueOf(it) } ?: WeightUnit.KG,
            dailyProteinGoalG = prefs[Keys.PROTEIN_GOAL],
            dailyCarbsGoalG = prefs[Keys.CARBS_GOAL],
            dailyFatGoalG = prefs[Keys.FAT_GOAL],
            dailySaturatedFatGoalG = prefs[Keys.SATURATED_FAT_GOAL],
            dailySugarGoalG = prefs[Keys.SUGAR_GOAL],
            dailyFiberGoalG = prefs[Keys.FIBER_GOAL],
            dailySaltGoalG = prefs[Keys.SALT_GOAL],
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

    suspend fun setDailyProteinGoal(g: Double?) = setOrRemove(Keys.PROTEIN_GOAL, g)
    suspend fun setDailyCarbsGoal(g: Double?) = setOrRemove(Keys.CARBS_GOAL, g)
    suspend fun setDailyFatGoal(g: Double?) = setOrRemove(Keys.FAT_GOAL, g)
    suspend fun setDailySaturatedFatGoal(g: Double?) = setOrRemove(Keys.SATURATED_FAT_GOAL, g)
    suspend fun setDailySugarGoal(g: Double?) = setOrRemove(Keys.SUGAR_GOAL, g)
    suspend fun setDailyFiberGoal(g: Double?) = setOrRemove(Keys.FIBER_GOAL, g)
    suspend fun setDailySaltGoal(g: Double?) = setOrRemove(Keys.SALT_GOAL, g)

    private suspend fun setOrRemove(key: Preferences.Key<Double>, value: Double?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (value == null) prefs.remove(key) else prefs[key] = value
        }
    }
}
