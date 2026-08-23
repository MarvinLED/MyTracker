package com.example.mytracker.goals

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupScope
import com.example.mytracker.core.datastore.DEFAULT_WATER_GOAL_ML
import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.datastore.UserPreferencesRepository
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalDao
import com.example.mytracker.fitness.FitnessGoalMetric
import com.example.mytracker.fitness.StrengthMaxWeightGoal
import com.example.mytracker.fitness.StrengthMaxWeightGoalDao
import com.example.mytracker.fitness.strength.MovementDirection
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class NutrientGoalDto(
    val nutrient: Nutrient,
    val min: Double? = null,
    val max: Double? = null,
)

@Serializable
data class FitnessGoalDto(
    val id: String,
    val metric: FitnessGoalMetric,
    val period: GoalPeriod,
    val muscleGroupId: String? = null,
    val movementDirection: MovementDirection? = null,
    /** Defaulted, so a backup written before the per-exercise goals reads back as what it was. */
    val exerciseId: String? = null,
    val targetValue: Double,
    val createdAtEpochMillis: Long,
)

/**
 * A long-term max-weight goal. The starting point travels with it: without it the plan would restart
 * from the restoring device's current weight, which is exactly the progress the backup was meant to
 * bring back.
 */
@Serializable
data class StrengthMaxWeightGoalDto(
    val id: String,
    val exerciseId: String,
    val targetWeightKg: Double,
    val targetEpochDay: Long,
    val startWeightKg: Double,
    val startEpochDay: Long,
    val createdAtEpochMillis: Long,
)

@Serializable
data class NutrientGoalChangeDto(
    val id: String,
    val nutrient: Nutrient,
    val effectiveFromEpochDay: Long,
    val min: Double? = null,
    val max: Double? = null,
    val changedAtEpochMillis: Long,
)

@Serializable
data class GoalsDto(
    val dailyWaterGoalMl: Double = DEFAULT_WATER_GOAL_ML,
    val nutrientGoals: List<NutrientGoalDto> = emptyList(),
    /** Both in minutes, matching `UserPreferences.sleepDurationGoalMinutes`. */
    val sleepDurationMinMinutes: Double? = null,
    val sleepDurationMaxMinutes: Double? = null,
    val bedtimeGoalMinuteOfDay: Int? = null,
    val fitnessGoals: List<FitnessGoalDto> = emptyList(),
    val strengthMaxWeightGoals: List<StrengthMaxWeightGoalDto> = emptyList(),
    /**
     * When the nutrient goals were moved. Irreplaceable in a way the goals themselves are not: a
     * lost target can be typed in again, a lost record of when it changed cannot be reconstructed
     * from anything.
     */
    val nutrientGoalChanges: List<NutrientGoalChangeDto> = emptyList(),
)

private fun NutrientGoalChange.toDto() = NutrientGoalChangeDto(
    id = id,
    nutrient = nutrient,
    effectiveFromEpochDay = effectiveFromEpochDay,
    min = minValue,
    max = maxValue,
    changedAtEpochMillis = changedAt.toEpochMilli(),
)

private fun NutrientGoalChangeDto.toEntity() = NutrientGoalChange(
    id = id,
    nutrient = nutrient,
    effectiveFromEpochDay = effectiveFromEpochDay,
    minValue = min,
    maxValue = max,
    changedAt = Instant.ofEpochMilli(changedAtEpochMillis),
)

private fun FitnessGoal.toDto() = FitnessGoalDto(
    id = id,
    metric = metric,
    period = period,
    muscleGroupId = muscleGroupId,
    movementDirection = movementDirection,
    exerciseId = exerciseId,
    targetValue = targetValue,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun FitnessGoalDto.toEntity() = FitnessGoal(
    id = id,
    metric = metric,
    period = period,
    muscleGroupId = muscleGroupId,
    movementDirection = movementDirection,
    exerciseId = exerciseId,
    targetValue = targetValue,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

private fun StrengthMaxWeightGoal.toDto() = StrengthMaxWeightGoalDto(
    id = id,
    exerciseId = exerciseId,
    targetWeightKg = targetWeightKg,
    targetEpochDay = targetEpochDay,
    startWeightKg = startWeightKg,
    startEpochDay = startEpochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun StrengthMaxWeightGoalDto.toEntity() = StrengthMaxWeightGoal(
    id = id,
    exerciseId = exerciseId,
    targetWeightKg = targetWeightKg,
    targetEpochDay = targetEpochDay,
    startWeightKg = startWeightKg,
    startEpochDay = startEpochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * Every Ziel the user set: the nutrient bounds, the water, sleep and bedtime goals out of the
 * preferences DataStore, and the Fitness-Ziele out of Room. They ride with the Bibliothek rather
 * than with the Einstellungen because that is what they are — something built up over time, not a
 * switch. The Habit- and Getränkeziele are not here only because they already travel inside
 * `habits` and `fluidTypes`, which own the rows they hang off.
 *
 * Imported after `muscleGroups` (priority 0): [FitnessGoalDto.muscleGroupId] points into them.
 *
 * A merging import **fills gaps only** — a goal already set on the device is left exactly as it is,
 * including a water goal that has been moved off its default. Restoring a backup should bring back
 * what was lost without quietly undoing what was changed since. Use Ersetzen to get the file's
 * version verbatim.
 */
class GoalsExportProvider @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val fitnessGoalDao: FitnessGoalDao,
    private val maxWeightGoalDao: StrengthMaxWeightGoalDao,
    private val nutrientGoalChangeDao: NutrientGoalChangeDao,
) : BackupExportProvider {
    override val key = "goals"
    override val scope = BackupScope.LIBRARY
    override val importPriority = 6

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val prefs = userPreferencesRepository.userPreferences.first()
        return json.encodeToJsonElement(
            GoalsDto(
                dailyWaterGoalMl = prefs.dailyWaterGoalMl,
                nutrientGoals = prefs.nutrientGoals.map { (nutrient, goal) ->
                    NutrientGoalDto(nutrient = nutrient, min = goal.min, max = goal.max)
                },
                sleepDurationMinMinutes = prefs.sleepDurationGoalMinutes?.min,
                sleepDurationMaxMinutes = prefs.sleepDurationGoalMinutes?.max,
                bedtimeGoalMinuteOfDay = prefs.bedtimeGoalMinuteOfDay,
                fitnessGoals = fitnessGoalDao.getAllOnce().map { it.toDto() },
                strengthMaxWeightGoals = maxWeightGoalDao.getAllOnce().map { it.toDto() },
                nutrientGoalChanges = nutrientGoalChangeDao.getAllOnce().map { it.toDto() },
            ),
        )
    }

    override suspend fun import(json: JsonElement) {
        val dto = this.json.decodeFromJsonElement<GoalsDto>(json)
        val existing = userPreferencesRepository.userPreferences.first()

        if (existing.dailyWaterGoalMl == DEFAULT_WATER_GOAL_ML) {
            userPreferencesRepository.setDailyWaterGoal(dto.dailyWaterGoalMl)
        }
        dto.nutrientGoals.forEach { goalDto ->
            if (existing.nutrientGoals[goalDto.nutrient] == null) {
                userPreferencesRepository.setNutrientGoal(
                    goalDto.nutrient,
                    NutrientGoal(min = goalDto.min, max = goalDto.max),
                )
            }
        }
        if (existing.sleepDurationGoalMinutes == null) {
            userPreferencesRepository.setSleepDurationGoal(
                NutrientGoal(min = dto.sleepDurationMinMinutes, max = dto.sleepDurationMaxMinutes),
            )
        }
        if (existing.bedtimeGoalMinuteOfDay == null) {
            userPreferencesRepository.setBedtimeGoal(dto.bedtimeGoalMinuteOfDay)
        }
        dto.fitnessGoals.forEach { goalDto ->
            if (fitnessGoalDao.getById(goalDto.id) == null) {
                fitnessGoalDao.upsert(goalDto.toEntity())
            }
        }
        // Matched on the exercise, not the id: one long-term goal per exercise is the rule the table
        // enforces, so a device that already has one for this lift keeps its own.
        dto.strengthMaxWeightGoals.forEach { goalDto ->
            if (maxWeightGoalDao.getForExercise(goalDto.exerciseId) == null) {
                maxWeightGoalDao.upsert(goalDto.toEntity())
            }
        }
        // Append-only log, so a merge is "add the rows this device does not have". Matching on the
        // id is enough: a change row is never edited after the fact, so same id means same event.
        val knownIds = nutrientGoalChangeDao.getAllOnce().mapTo(mutableSetOf()) { it.id }
        nutrientGoalChangeDao.insertAll(
            dto.nutrientGoalChanges.filterNot { it.id in knownIds }.map { it.toEntity() },
        )
    }

    override suspend fun clear() {
        userPreferencesRepository.setDailyWaterGoal(DEFAULT_WATER_GOAL_ML)
        Nutrient.entries.forEach { userPreferencesRepository.setNutrientGoal(it, null) }
        userPreferencesRepository.setSleepDurationGoal(null)
        userPreferencesRepository.setBedtimeGoal(null)
        fitnessGoalDao.deleteAll()
        maxWeightGoalDao.deleteAll()
        // Goes with the goals: a replacing import that kept the old log would date this device's
        // history of changes to targets that are no longer here.
        nutrientGoalChangeDao.deleteAll()
    }
}
