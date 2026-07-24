package com.example.prokject2_tracker.fitness.cardio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CardioListViewModel @Inject constructor(
    private val cardioRepository: CardioRepository,
) : ViewModel() {
    val sessions: StateFlow<List<CardioSession>> = cardioRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { cardioRepository.ensureDefaultActivityTypesSeeded() }
    }

    fun delete(session: CardioSession) {
        viewModelScope.launch { cardioRepository.delete(session) }
    }
}
