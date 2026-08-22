package com.example.mytracker.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

/**
 * What the water goal is before anyone sets one. Public because the backup has to be able to tell
 * "never configured" from "deliberately set to 2000" — see
 * `com.example.mytracker.goals.GoalsExportProvider`.
 */
const val DEFAULT_WATER_GOAL_ML = 2000.0

data class UserPreferences(
    val dailyWaterGoalMl: Double,
    val weightUnit: WeightUnit,
    /** Only the nutrients the user actually set a goal for; the rest are simply absent. */
    val nutrientGoals: Map<Nutrient, NutrientGoal> = emptyMap(),
    /**
     * How long a night should be, in **minutes** — [NutrientGoal] is reused because a sleep goal has
     * exactly the same shape as a nutrient one: a lower bound ("mind. 7 h"), an upper bound, or both.
     * Null, or a goal with neither bound, means sleep duration isn't tracked as a goal.
     */
    val sleepDurationGoalMinutes: NutrientGoal? = null,
    /**
     * The latest one wants to be asleep, as minutes since midnight. Compared on an evening timeline,
     * see `com.example.mytracker.sleep.isBedtimeMet`. Null means no bedtime goal.
     */
    val bedtimeGoalMinuteOfDay: Int? = null,
) {
    /**
     * The calorie goal as a plain number, for the many places that only need "what am I aiming at".
     * Unlike the other nutrients this one always has a value, so those call sites don't each have to
     * repeat the fallback.
     */
    val dailyCalorieGoalKcal: Double
        get() = nutrientGoals[Nutrient.KCAL]?.barTarget ?: DEFAULT_CALORIE_GOAL_KCAL

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
         * The per-nutrient key stems. The stem itself is the *legacy* single-value key from before
         * goals had two bounds; the bounds live under `_min`/`_max` beside it. Both are still read,
         * because a goal saved by an older build only ever wrote the legacy pair.
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

        fun min(nutrient: Nutrient) = doublePreferencesKey("${VALUE_KEY_NAMES.getValue(nutrient)}_min")
        fun max(nutrient: Nutrient) = doublePreferencesKey("${VALUE_KEY_NAMES.getValue(nutrient)}_max")

        fun legacyValue(nutrient: Nutrient) = doublePreferencesKey(VALUE_KEY_NAMES.getValue(nutrient))
        fun legacyType(nutrient: Nutrient) = stringPreferencesKey("${VALUE_KEY_NAMES.getValue(nutrient)}_type")

        val SLEEP_DURATION_MIN = doublePreferencesKey("sleep_duration_goal_minutes_min")
        val SLEEP_DURATION_MAX = doublePreferencesKey("sleep_duration_goal_minutes_max")
        val BEDTIME_GOAL = intPreferencesKey("bedtime_goal_minute_of_day")
    }

    /**
     * A goal written before bounds existed: one value plus a direction. "mindestens"/"höchstens"
     * become the matching single bound; everything else was "genau", which becomes both bounds on
     * the same number.
     */
    private fun legacyGoal(value: Double, type: String?): NutrientGoal = when (type) {
        "MIN" -> NutrientGoal(min = value)
        "MAX" -> NutrientGoal(max = value)
        else -> NutrientGoal(min = value, max = value)
    }

    val userPreferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            dailyWaterGoalMl = prefs[Keys.WATER_GOAL] ?: DEFAULT_WATER_GOAL_ML,
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let { WeightUnit.valueOf(it) } ?: WeightUnit.KG,
            nutrientGoals = Nutrient.entries.mapNotNull { nutrient ->
                val min = prefs[Keys.min(nutrient)]
                val max = prefs[Keys.max(nutrient)]
                val goal = if (min != null || max != null) {
                    NutrientGoal(min = min, max = max)
                } else {
                    // Nothing under the new keys, so fall back to what an older build wrote.
                    val legacy = prefs[Keys.legacyValue(nutrient)] ?: return@mapNotNull null
                    legacyGoal(legacy, prefs[Keys.legacyType(nutrient)])
                }
                nutrient to goal
            }.toMap(),
            sleepDurationGoalMinutes = NutrientGoal(
                min = prefs[Keys.SLEEP_DURATION_MIN],
                max = prefs[Keys.SLEEP_DURATION_MAX],
            ).takeUnless { it.isEmpty },
            bedtimeGoalMinuteOfDay = prefs[Keys.BEDTIME_GOAL],
        )
    }

    suspend fun setDailyWaterGoal(ml: Double) {
        context.userPreferencesDataStore.edit { it[Keys.WATER_GOAL] = ml }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.userPreferencesDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    /** A null or empty [goal] clears the sleep-duration goal; bounds are in minutes. */
    suspend fun setSleepDurationGoal(goal: NutrientGoal?) {
        context.userPreferencesDataStore.edit { prefs ->
            val min = goal?.takeUnless { it.isEmpty }?.min
            val max = goal?.takeUnless { it.isEmpty }?.max
            if (min != null) prefs[Keys.SLEEP_DURATION_MIN] = min else prefs.remove(Keys.SLEEP_DURATION_MIN)
            if (max != null) prefs[Keys.SLEEP_DURATION_MAX] = max else prefs.remove(Keys.SLEEP_DURATION_MAX)
        }
    }

    /** Null clears the bedtime goal. */
    suspend fun setBedtimeGoal(minuteOfDay: Int?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (minuteOfDay != null) prefs[Keys.BEDTIME_GOAL] = minuteOfDay else prefs.remove(Keys.BEDTIME_GOAL)
        }
    }

    /** A null or empty [goal] clears the nutrient's goal entirely, both bounds together. */
    suspend fun setNutrientGoal(nutrient: Nutrient, goal: NutrientGoal?) {
        context.userPreferencesDataStore.edit { prefs ->
            // The legacy pair goes either way: leaving it behind would let a long-replaced value
            // reappear as soon as both new bounds are cleared again.
            prefs.remove(Keys.legacyValue(nutrient))
            prefs.remove(Keys.legacyType(nutrient))

            val min = goal?.min
            val max = goal?.max
            if (min != null) prefs[Keys.min(nutrient)] = min else prefs.remove(Keys.min(nutrient))
            if (max != null) prefs[Keys.max(nutrient)] = max else prefs.remove(Keys.max(nutrient))
        }
    }
}
