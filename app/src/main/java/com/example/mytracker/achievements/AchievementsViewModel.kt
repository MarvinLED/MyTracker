package com.example.mytracker.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    achievementRepository: AchievementRepository,
    gameLedgerRepository: GameLedgerRepository,
    private val settingsRepository: AchievementsSettingsRepository,
) : ViewModel() {
    // Fixed at construction like the Habits and Tagesziele screens: "heute" deciding what counts as
    // a running streak must not change under the user's hands at midnight.
    private val today = DateUtils.todayEpochDay()

    /**
     * What had already been seen when this screen was opened, read **once**.
     *
     * Held still on purpose. Observing the store instead would mean that marking everything seen —
     * which happens the moment the wall is drawn — immediately cleared every "Neu" badge again, and
     * the one thing the badges exist for is to still be there while the screen is being read.
     */
    private val seenAtOpen = MutableStateFlow<AchievementsSettings?>(null)

    init {
        viewModelScope.launch {
            seenAtOpen.value = settingsRepository.settings.first()
            // Settling the finished days is what fills the figure in the first place. It runs here
            // rather than at app start because this is the only screen that shows the result, and a
            // backfill of a year has no business delaying the Tagebuch.
            gameLedgerRepository.bookMissingDays(today)
        }
    }

    val uiState: StateFlow<AchievementsUiState> = combine(
        achievementRepository.observe(today),
        gameLedgerRepository.observePoints(),
        seenAtOpen.filterNotNull(),
    ) { sections, ledger, seen ->
        val byAttribute = ledger
            .groupBy { it.attribute }
            .mapValues { (_, rows) -> rows.associate { it.epochDay to it.points } }
        val attributes = attributeLevels(byAttribute, today, ledger.minOfOrNull { it.epochDay })
        val unlocked = unlockedItems(attributes)

        // Nothing is new on a first visit: everything would be, and a screen where every row shouts
        // is a screen where none of them do.
        fun isNew(key: String) = seen.hasBeenSeen && key !in seen.seenIds

        AchievementsUiState(
            attributes = attributes,
            // Yesterday, not today: today is deliberately never booked, so it has no points yet.
            lastBookedPoints = ledger.filter { it.epochDay == today - 1 }.sumOf { it.points },
            unlockedItems = unlocked,
            newItems = unlocked.filterTo(mutableSetOf()) { isNew(itemSeenKey(it)) },
            nextUnlock = nextUnlock(attributes),
            sections = sections.map { section ->
                section.copy(items = section.items.map { it.copy(isNew = isNew(it.seenKey)) })
            },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AchievementsUiState())

    /**
     * Remembers everything currently on the wall, so the next visit can tell what has changed.
     * Called by the screen once it has actually drawn — marking things seen that were never on
     * screen would swallow exactly the news this is for.
     */
    fun markSeen() {
        val state = uiState.value
        if (!state.loaded) return
        viewModelScope.launch {
            settingsRepository.markSeen(
                state.sections.flatMap { section -> section.items.map { it.seenKey } }.toSet() +
                    state.unlockedItems.map { itemSeenKey(it) },
            )
        }
    }
}

/** Equipment is remembered by name — unlike a record, it has no value that can move. */
private fun itemSeenKey(item: AvatarItem): String = "item:${item.name}"
