package com.example.mytracker.fluid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FluidQuickAddManageViewModel @Inject constructor(
    private val fluidRepository: FluidRepository,
) : ViewModel() {
    val quickAdds: StateFlow<List<FluidQuickAdd>> = fluidRepository.observeQuickAdds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val types: StateFlow<List<FluidType>> = fluidRepository.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(fluidTypeId: String, symbol: FluidQuickAddSymbol, amountMl: Double) {
        viewModelScope.launch { fluidRepository.createQuickAdd(fluidTypeId, symbol, amountMl) }
    }

    fun update(existing: FluidQuickAdd, fluidTypeId: String, symbol: FluidQuickAddSymbol, amountMl: Double) {
        viewModelScope.launch { fluidRepository.updateQuickAdd(existing, fluidTypeId, symbol, amountMl) }
    }

    fun delete(quickAdd: FluidQuickAdd) {
        viewModelScope.launch { fluidRepository.deleteQuickAdd(quickAdd) }
    }
}
