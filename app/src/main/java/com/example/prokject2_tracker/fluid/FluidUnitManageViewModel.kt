package com.example.prokject2_tracker.fluid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FluidUnitManageViewModel @Inject constructor(
    private val fluidRepository: FluidRepository,
) : ViewModel() {
    val units: StateFlow<List<FluidUnit>> = fluidRepository.observeUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String, amountMl: Double) {
        viewModelScope.launch { fluidRepository.createUnit(name, amountMl) }
    }

    fun update(unit: FluidUnit, name: String, amountMl: Double) {
        viewModelScope.launch { fluidRepository.updateUnit(unit, name, amountMl) }
    }

    fun deleteIfUnused(unit: FluidUnit, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (fluidRepository.canDeleteUnit(unit.id)) {
                fluidRepository.deleteUnit(unit)
            } else {
                onBlocked()
            }
        }
    }
}
