package com.example.mytracker.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.fitness.cardio.CardioRepository
import com.example.mytracker.fitness.cardio.CardioSession
import com.example.mytracker.fitness.strength.StrengthLogEntry
import com.example.mytracker.fitness.strength.StrengthLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The chronological log of everything trained, day by day. This used to be the Fitness screen's
 * body; it moved here when the exercise list took that place, so browsing exercises and reviewing
 * what was actually done stay separate tasks.
 */
@HiltViewModel
class TrainingHistoryViewModel @Inject constructor(
    private val cardioRepository: CardioRepository,
    private val strengthLogRepository: StrengthLogRepository,
) : ViewModel() {
    val rows: StateFlow<List<TrainingListRow>> = combine(
        cardioRepository.observeAll(),
        strengthLogRepository.observeAll(),
        strengthLogRepository.observeAllSets(),
    ) { cardioSessions, strengthEntries, allSets ->
        val setsByEntryId = allSets.groupBy { it.logEntryId }
        val rows: List<TrainingListRow> = cardioSessions.map { TrainingListRow.Cardio(it) } +
            strengthEntries.map { entry -> TrainingListRow.Strength(entry, setsByEntryId[entry.id].orEmpty()) }
        rows.sortedWith(compareByDescending<TrainingListRow> { it.epochDay }.thenByDescending { it.createdAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCardio(session: CardioSession) {
        viewModelScope.launch { cardioRepository.delete(session) }
    }

    fun deleteStrength(entry: StrengthLogEntry) {
        viewModelScope.launch { strengthLogRepository.delete(entry) }
    }
}
