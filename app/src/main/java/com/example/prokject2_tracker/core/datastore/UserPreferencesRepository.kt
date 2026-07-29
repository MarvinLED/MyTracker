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

/** Falls back to 2000 kcal when the user has never set a calorie goal. */
private const val DEFAULT_CALORIE_GOAL_KCAL = 2000.0

data class UserPreferences(
    val dailyWaterGoalMl: Double,
    val weightUnit: WeightUnit,
    /** Only the nutrients the user actually set a goal for; the rest are simply absent. */
    val nutrientGoals: Map<Nutrient, NutrientGoal> = emptyMap(),
) {
    /**
     * The calorie goal as a plain number, for the many places that only need "what am I aiming at".
     * Unlike the other nutrients this one always has a value, so those call sites don't each have to
     * repeat the fallback.
     */
    val dailyCalorieGoalKcal: Double
        get() = nutrientGoals[Nutrient.KCAL]?.value ?: DEFAULT_CALORIE_GOAL_KCAL

    /** Goals that are set and not yet met, in [Nutrient] order — everything still open today. */
    fun unmetGoals(consumed: Map<Nutrient, Double>): List<Triple<Nutrient, NutrientGoal, Double>> =
        Nutrient.entries.mapNotNull { nutrient ->
            val goal = nutrientGoals[nutrient] ?: return@mapNotNull null
            val value = consumed[nutrient] ?: 0.0
            if (goal.isMetBy(value)) null else Triple(nutrient, goal, value)
        }
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val WATER_GOAL = doublePreferencesKey("daily_water_goal_ml")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")

        /**
         * The historical per-nutrient value keys, kept verbatim so goals set before goal *types*
         * existed are still read back. The matching `_type` key is new and absent for those, which
         * reads as [NutrientGoalType.EXACT] — the behaviour those goals already had.
         */
        private val VALUE_KEY_NAMES = mapOf(
            Nutrient.KCAL to "daily_calorie_goal_kcal",
            Nutrient.PROTEIN to "daily_protein_goal_g",
            Nutrient.CARBS to "daily_carbs_goal_g",
            Nutrient.FAT to "daily_fat_goal_g",
            Nutrient.SATURATED_FAT to "daily_saturated_fat_goal_g",
            Nutrient.SUGAR to "daily_sugar_goal_g",
            Nutrient.FIBER to "daily_fiber_goal_g",
            Nutrient.SALT to "daily_salt_goal_g",
        )

        fun value(nutrient: Nutrient) = doublePreferencesKey(VALUE_KEY_NAMES.getValue(nutrient))
        fun type(nutrient: Nutrient) = stringPreferencesKey("${VALUE_KEY_NAMES.getValue(nutrient)}_type")
    }

    val userPreferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            dailyWaterGoalMl = prefs[Keys.WATER_GOAL] ?: 2000.0,
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { WeightUnit.valueOf(it) } ?: WeightUnit.KG,
            nutrientGoals = Nutrient.entries.mapNotNull { nutrient ->
                val value = prefs[Keys.value(nutrient)] ?: return@mapNotNull null
                val type = prefs[Keys.type(nutrient)]
                    ?.let { runCatching { NutrientGoalType.valueOf(it) }.getOrNull() }
                    ?: NutrientGoalType.EXACT
                nutrient to NutrientGoal(value = value, type = type)
            }.toMap(),
        )
    }

    suspend fun setDailyWaterGoal(ml: Double) {
        context.userPreferencesDataStore.edit { it[Keys.WATER_GOAL] = ml }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.userPreferencesDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    /** A null [goal] clears the nutrient's goal entirely, value and type together. */
    suspend fun setNutrientGoal(nutrient: Nutrient, goal: NutrientGoal?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (goal == null) {
                prefs.remove(Keys.value(nutrient))
                prefs.remove(Keys.type(nutrient))
            } else {
                prefs[Keys.value(nutrient)] = goal.value
                prefs[Keys.type(nutrient)] = goal.type.name
            }
        }
    }
}
