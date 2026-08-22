package com.example.prokject2_tracker.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.minutesBetweenTimesOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How many past nights the list under the form shows — enough to see a pattern, not a log. */
private const val HISTORY_LIMIT = 14

/** What a night that was never logged before starts on, when there is no earlier night to copy. */
private const val DEFAULT_BEDTIME_MINUTE = 23 * 60
private const val DEFAULT_WAKE_MINUTE = 7 * 60

/** Where the fitness slider starts before a single morning has ever been rated — the middle. */
private const val DEFAULT_MORNING_FITNESS = 5

/** Which form mode is active. */
enum class SleepFormMode { NIGHT, NAP }

/** One past night, with its tags resolved to names for the list. */
data class SleepHistoryRow(
    val entry: SleepEntry,
    val tagNames: List<String>,
)

/**
 * The form always edits *one* night: the one that ended on [epochDay]. Saving upserts that night, so
 * paging back to a night already logged shows its values and corrects them in place — there is no
 * separate "edit" mode to get into.
 */
data class SleepUiState(
    val epochDay: Long = DateUtils.todayEpochDay(),
    val mode: SleepFormMode = SleepFormMode.NIGHT,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val lastMealMinuteOfDay: Int? = null,
    val morningFitness: Int = DEFAULT_MORNING_FITNESS,
    val napRefreshmentFitness: Int = DEFAULT_MORNING_FITNESS,
    val didNotSleep: Boolean = false,
    val selectedTagIds: Set<String> = emptySet(),
    val allTags: List<SleepTag> = emptyList(),
    val tagInput: String = "",
    /** True once this night exists in the database — the button then says "Aktualisieren". */
    val isExistingNight: Boolean = false,
    /** True when the current form differs from the stored entry — "update" button only shows when true. */
    val hasChanges: Boolean = false,
    val history: List<SleepHistoryRow> = emptyList(),
    val durationGoalMinutes: NutrientGoal? = null,
    val bedtimeGoalMinuteOfDay: Int? = null,
) {
    /** Null until both ends are set — a night with only a bedtime has no length yet. */
    val durationMinutes: Int?
        get() {
            val start = startMinuteOfDay ?: return null
            val end = endMinuteOfDay ?: return null
            return minutesBetweenTimesOfDay(start, end)
        }

    val minutesBetweenLastMealAndSleep: Int?
        get() {
            val meal = lastMealMinuteOfDay ?: return null
            val start = startMinuteOfDay ?: return null
            return minutesBetweenTimesOfDay(meal, start)
        }

    val canSave: Boolean
        get() = didNotSleep || (startMinuteOfDay != null && endMinuteOfDay != null)

    /** How the night in the form stands against the goals — the same rows the Tagesziele show. */
    val goalStatuses: List<SleepGoalStatus>
        get() = sleepGoalStatuses(
            entry = draftEntry(),
            durationGoalMinutes = durationGoalMinutes,
            bedtimeGoalMinuteOfDay = bedtimeGoalMinuteOfDay,
        )

    /** The form as an entry, for anything that computes on a night. Null while it is incomplete. */
    private fun draftEntry(): SleepEntry? {
        val start = startMinuteOfDay ?: return null
        val end = endMinuteOfDay ?: return null
        return SleepEntry(
            id = "sleep-$epochDay",
            epochDay = epochDay,
            startMinuteOfDay = start,
            endMinuteOfDay = end,
            morningFitness = morningFitness,
            lastMealMinuteOfDay = lastMealMinuteOfDay,
            createdAt = java.time.Instant.EPOCH,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SleepViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _epochDay = MutableStateFlow(calculateInitialEpochDay())
    private val _mode = MutableStateFlow(SleepFormMode.NIGHT)

    /** The typed-in half of the form. Null fields mean "not touched", so loading a night can fill them. */
    private val _draft = MutableStateFlow(SleepDraft())
    private val _tagInput = MutableStateFlow("")

    val uiState: StateFlow<SleepUiState> = combine(
        _epochDay.flatMapLatest { day -> sleepRepository.observeForDay(day) },
        _draft,
        _mode,
        combine(
            sleepRepository.observeAll(),
            sleepRepository.observeTags(),
            sleepRepository.observeTagIdsByEntryId(),
        ) { entries, tags, tagIdsByEntry -> Triple(entries, tags, tagIdsByEntry) },
        combine(_epochDay, _tagInput, userPreferencesRepository.userPreferences) { day, input, prefs ->
            Triple(day, input, prefs)
        },
    ) { storedNight, draft, mode, (entries, tags, tagIdsByEntry), (day, tagInput, prefs) ->
        val tagNamesById = tags.associate { it.id to it.name }
        val hasChanges = storedNight != null && (
            draft.startMinuteOfDay != storedNight.startMinuteOfDay ||
            draft.endMinuteOfDay != storedNight.endMinuteOfDay ||
            draft.lastMealMinuteOfDay != storedNight.lastMealMinuteOfDay ||
            draft.morningFitness != storedNight.morningFitness ||
            draft.didNotSleep != storedNight.didNotSleep ||
            draft.tagIds != sleepRepository.getTagIdsForEntry(storedNight.id).toSet()
        )
        SleepUiState(
            epochDay = day,
            mode = mode,
            startMinuteOfDay = draft.startMinuteOfDay,
            endMinuteOfDay = draft.endMinuteOfDay,
            lastMealMinuteOfDay = draft.lastMealMinuteOfDay,
            morningFitness = draft.morningFitness,
            napRefreshmentFitness = draft.napRefreshmentFitness,
            didNotSleep = draft.didNotSleep,
            selectedTagIds = draft.tagIds,
            allTags = tags,
            tagInput = tagInput,
            isExistingNight = storedNight != null,
            hasChanges = hasChanges,
            history = entries.filter { it.epochDay != day }.take(HISTORY_LIMIT).map { entry ->
                SleepHistoryRow(
                    entry = entry,
                    tagNames = tagIdsByEntry[entry.id].orEmpty().mapNotNull { tagNamesById[it] },
                )
            },
            durationGoalMinutes = prefs.sleepDurationGoalMinutes,
            bedtimeGoalMinuteOfDay = prefs.bedtimeGoalMinuteOfDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepUiState())

    init {
        loadDraftFor(_epochDay.value)
    }

    /**
     * Fills the form for [epochDay]: that night if it is already logged, otherwise the previous
     * night's times as a starting point — bedtimes barely move, so the usual save is two taps.
     * A brand new user gets 23:00–7:00 rather than an empty form that says nothing about the shape
     * of the answer.
     */
    private fun loadDraftFor(epochDay: Long) {
        viewModelScope.launch {
            val stored = sleepRepository.getForDay(epochDay)
            if (stored != null) {
                _draft.value = SleepDraft(
                    startMinuteOfDay = stored.startMinuteOfDay,
                    endMinuteOfDay = stored.endMinuteOfDay,
                    lastMealMinuteOfDay = stored.lastMealMinuteOfDay,
                    morningFitness = resolveFitness(stored.morningFitness, epochDay),
                    didNotSleep = stored.didNotSleep,
                    tagIds = sleepRepository.getTagIdsForEntry(stored.id).toSet(),
                )
                return@launch
            }
            val previous = sleepRepository.getMostRecentBefore(epochDay)
            _draft.value = SleepDraft(
                startMinuteOfDay = previous?.startMinuteOfDay ?: DEFAULT_BEDTIME_MINUTE,
                endMinuteOfDay = previous?.endMinuteOfDay ?: DEFAULT_WAKE_MINUTE,
                morningFitness = resolveFitness(stored = null, epochDay = epochDay),
                // Deliberately not carried over: the tags and the meal time are about *this* night,
                // and repeating them would quietly log yesterday's evening again.
                lastMealMinuteOfDay = null,
                didNotSleep = false,
                tagIds = emptySet(),
            )
        }
    }

    /**
     * What the slider shows: the night's own rating, else the last rating given before it. Mornings
     * feel much like the one before, so starting there makes the usual night a no-op — and a night
     * that was never rated (an old entry, or one logged before the slider existed) gets a sensible
     * position rather than an arbitrary one.
     */
    private suspend fun resolveFitness(stored: Int?, epochDay: Long): Int =
        stored ?: sleepRepository.getMostRecentFitnessBefore(epochDay) ?: DEFAULT_MORNING_FITNESS

    fun goToPreviousDay() = selectDay(_epochDay.value - 1)

    fun goToNextDay() = selectDay(_epochDay.value + 1)

    fun selectDay(epochDay: Long) {
        _epochDay.value = epochDay
        loadDraftFor(epochDay)
    }

    fun onStartChange(minuteOfDay: Int) { _draft.value = _draft.value.copy(startMinuteOfDay = minuteOfDay) }

    fun onEndChange(minuteOfDay: Int) { _draft.value = _draft.value.copy(endMinuteOfDay = minuteOfDay) }

    fun onLastMealChange(minuteOfDay: Int) { _draft.value = _draft.value.copy(lastMealMinuteOfDay = minuteOfDay) }

    fun clearLastMeal() { _draft.value = _draft.value.copy(lastMealMinuteOfDay = null) }

    fun onFitnessChange(value: Int) { _draft.value = _draft.value.copy(morningFitness = value) }

    fun onTagToggle(tagId: String) {
        val current = _draft.value.tagIds
        _draft.value = _draft.value.copy(
            tagIds = if (tagId in current) current - tagId else current + tagId,
        )
    }

    fun onTagInputChange(value: String) { _tagInput.value = value }

    fun onModeChange(mode: SleepFormMode) { _mode.value = mode }

    fun onDidNotSleepChange(value: Boolean) {
        _draft.value = _draft.value.copy(didNotSleep = value)
    }

    fun onNapRefreshmentChange(value: Int) {
        _draft.value = _draft.value.copy(napRefreshmentFitness = value)
    }

    /** Creates the typed tag (or reuses one of the same name) and attaches it to the night at once. */
    fun addTagFromInput() {
        val name = _tagInput.value
        if (name.isBlank()) return
        viewModelScope.launch {
            sleepRepository.createTag(name)?.let { tag ->
                _draft.value = _draft.value.copy(tagIds = _draft.value.tagIds + tag.id)
                _tagInput.value = ""
            }
        }
    }

    fun save() {
        val draft = _draft.value
        val day = _epochDay.value
        viewModelScope.launch {
            if (draft.didNotSleep) {
                sleepRepository.logNight(
                    epochDay = day,
                    startMinuteOfDay = null,
                    endMinuteOfDay = null,
                    morningFitness = draft.morningFitness,
                    lastMealMinuteOfDay = null,
                    tagIds = draft.tagIds.toList(),
                    didNotSleep = true,
                )
            } else {
                val start = draft.startMinuteOfDay ?: return@launch
                val end = draft.endMinuteOfDay ?: return@launch
                sleepRepository.logNight(
                    epochDay = day,
                    startMinuteOfDay = start,
                    endMinuteOfDay = end,
                    morningFitness = draft.morningFitness,
                    lastMealMinuteOfDay = draft.lastMealMinuteOfDay,
                    tagIds = draft.tagIds.toList(),
                    didNotSleep = false,
                )
            }
        }
    }

    fun deleteNight(entry: SleepEntry) {
        viewModelScope.launch {
            sleepRepository.delete(entry)
            if (entry.epochDay == _epochDay.value) loadDraftFor(entry.epochDay)
        }
    }

    private data class SleepDraft(
        val startMinuteOfDay: Int? = null,
        val endMinuteOfDay: Int? = null,
        val lastMealMinuteOfDay: Int? = null,
        val morningFitness: Int = DEFAULT_MORNING_FITNESS,
        val napRefreshmentFitness: Int = DEFAULT_MORNING_FITNESS,
        val didNotSleep: Boolean = false,
        val tagIds: Set<String> = emptySet(),
    )
}

/**
 * Calculate which night should be displayed when the screen opens.
 * Morning (4 AM - 12:00 noon) → show last night (today)
 * Afternoon (12:00 noon onwards) → show next night (tomorrow)
 */
private fun calculateInitialEpochDay(): Long {
    val now = LocalTime.now()
    val noonTime = LocalTime.of(12, 0)
    val morningStart = LocalTime.of(4, 0)

    val today = DateUtils.todayEpochDay()
    return if (now >= morningStart && now < noonTime) {
        today
    } else {
        today + 1
    }
}
