package com.example.prokject2_tracker.goals

import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * The day a seed row claims its bounds started applying: 1970-01-01, i.e. "for as long as this log
 * knows". It is not a real measurement — it exists so a nutrient's first-ever recorded change still
 * leaves the *previous* value written down somewhere, which nothing else in the app does.
 */
private const val SEED_EPOCH_DAY = 0L

/**
 * Writes nutrient goals *and* logs that they moved.
 *
 * Every path where the *user* changes a goal must come through here.
 * [UserPreferencesRepository.setNutrientGoal] overwrites in place and keeps no history, so calling
 * it directly silently loses the change and leaves a hole in the Verlauf's Soll line.
 *
 * The one deliberate exception is
 * [GoalsExportProvider][com.example.prokject2_tracker.goals.GoalsExportProvider], which restores the
 * goals and their log side by side out of a backup — logging those as fresh changes would date them
 * to the day of the restore.
 */
@Singleton
class NutrientGoalHistoryRepository @Inject constructor(
    private val nutrientGoalChangeDao: NutrientGoalChangeDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    fun observeChanges(): Flow<List<NutrientGoalChange>> = nutrientGoalChangeDao.observeAll()

    /**
     * Stores [newGoal] for [nutrient] and records the move away from [oldGoal].
     *
     * Nothing is written when the goal did not actually change — the Ziele screen saves all eight
     * nutrients on every tap of "Speichern", and logging those would bury the real changes in
     * duplicates. An empty goal and no goal are the same thing here, since neither can be aimed at.
     */
    suspend fun setGoal(
        nutrient: Nutrient,
        oldGoal: NutrientGoal?,
        newGoal: NutrientGoal?,
        today: Long = DateUtils.todayEpochDay(),
    ) {
        val previous = oldGoal?.takeUnless { it.isEmpty }
        val next = newGoal?.takeUnless { it.isEmpty }
        if (previous == next) return

        val now = Instant.now()
        // The first change for a nutrient also has to write down what it changed *from*, or the
        // old value — which until now existed only as the DataStore entry being overwritten right
        // here — would be gone before the chart ever saw it.
        if (previous != null && nutrientGoalChangeDao.countForNutrient(nutrient) == 0) {
            nutrientGoalChangeDao.insert(
                NutrientGoalChange(
                    id = IdGenerator.newId(),
                    nutrient = nutrient,
                    effectiveFromEpochDay = SEED_EPOCH_DAY,
                    minValue = previous.min,
                    maxValue = previous.max,
                    changedAt = now,
                ),
            )
        }

        nutrientGoalChangeDao.insert(
            NutrientGoalChange(
                id = IdGenerator.newId(),
                nutrient = nutrient,
                effectiveFromEpochDay = today,
                minValue = next?.min,
                maxValue = next?.max,
                changedAt = now,
            ),
        )

        userPreferencesRepository.setNutrientGoal(nutrient, next)
    }
}
