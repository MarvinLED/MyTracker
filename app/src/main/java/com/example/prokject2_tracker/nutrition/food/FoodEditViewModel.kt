package com.example.prokject2_tracker.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FoodEditState(
    val id: String? = null,
    val name: String = "",
    val baseUnit: BaseUnit = BaseUnit.G,
    val kcalPer100: String = "",
    val proteinPer100: String = "",
    val carbsPer100: String = "",
    val fatPer100: String = "",
    val servingName: String = "",
    val servingAmount: String = "",
    val isSaved: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
            kcalPer100.toDoubleOrNull() != null &&
            proteinPer100.toDoubleOrNull() != null &&
            carbsPer100.toDoubleOrNull() != null &&
            fatPer100.toDoubleOrNull() != null
}

@HiltViewModel
class FoodEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val route: FoodEditRoute = savedStateHandle.toRoute()
    private var existing: FoodItem? = null

    private val _state = MutableStateFlow(FoodEditState(id = route.foodId))
    val state: StateFlow<FoodEditState> = _state.asStateFlow()

    init {
        val foodId = route.foodId
        if (foodId != null) {
            viewModelScope.launch {
                foodRepository.getById(foodId)?.let { food ->
                    existing = food
                    _state.value = FoodEditState(
                        id = food.id,
                        name = food.name,
                        baseUnit = food.baseUnit,
                        kcalPer100 = food.kcalPer100.toString(),
                        proteinPer100 = food.proteinPer100.toString(),
                        carbsPer100 = food.carbsPer100.toString(),
                        fatPer100 = food.fatPer100.toString(),
                        servingName = food.servingName.orEmpty(),
                        servingAmount = food.servingAmount?.toString().orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onBaseUnitChange(value: BaseUnit) { _state.value = _state.value.copy(baseUnit = value) }
    fun onKcalChange(value: String) { _state.value = _state.value.copy(kcalPer100 = value) }
    fun onProteinChange(value: String) { _state.value = _state.value.copy(proteinPer100 = value) }
    fun onCarbsChange(value: String) { _state.value = _state.value.copy(carbsPer100 = value) }
    fun onFatChange(value: String) { _state.value = _state.value.copy(fatPer100 = value) }
    fun onServingNameChange(value: String) { _state.value = _state.value.copy(servingName = value) }
    fun onServingAmountChange(value: String) { _state.value = _state.value.copy(servingAmount = value) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            val servingName = s.servingName.ifBlank { null }
            val servingAmount = s.servingAmount.toDoubleOrNull()
            val current = existing
            if (current == null) {
                foodRepository.create(
                    name = s.name,
                    baseUnit = s.baseUnit,
                    kcalPer100 = s.kcalPer100.toDouble(),
                    proteinPer100 = s.proteinPer100.toDouble(),
                    carbsPer100 = s.carbsPer100.toDouble(),
                    fatPer100 = s.fatPer100.toDouble(),
                    servingName = servingName,
                    servingAmount = servingAmount,
                )
            } else {
                foodRepository.update(
                    current,
                    current.copy(
                        name = s.name,
                        baseUnit = s.baseUnit,
                        kcalPer100 = s.kcalPer100.toDouble(),
                        proteinPer100 = s.proteinPer100.toDouble(),
                        carbsPer100 = s.carbsPer100.toDouble(),
                        fatPer100 = s.fatPer100.toDouble(),
                        servingName = servingName,
                        servingAmount = servingAmount,
                    ),
                )
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
