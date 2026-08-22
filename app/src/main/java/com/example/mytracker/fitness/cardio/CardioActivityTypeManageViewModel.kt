package com.example.mytracker.fitness.cardio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CardioActivityTypeManageViewModel @Inject constructor(
    private val cardioRepository: CardioRepository,
) : ViewModel() {
    val types: StateFlow<List<CardioActivityType>> = cardioRepository.observeActivityTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { cardioRepository.createActivityType(name) }
    }

    fun update(type: CardioActivityType, name: String) {
        viewModelScope.launch { cardioRepository.updateActivityType(type, name) }
    }

    fun deleteIfUnused(type: CardioActivityType, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (cardioRepository.canDeleteActivityType(type.id)) {
                cardioRepository.deleteActivityType(type)
            } else {
                onBlocked()
            }
        }
    }
}
