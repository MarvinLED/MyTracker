package com.example.prokject2_tracker.fitness.strength

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StrengthLogListViewModel @Inject constructor(
    private val strengthLogRepository: StrengthLogRepository,
) : ViewModel() {
    val entries: StateFlow<List<StrengthLogEntry>> = strengthLogRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(entry: StrengthLogEntry) {
        viewModelScope.launch { strengthLogRepository.delete(entry) }
    }
}
