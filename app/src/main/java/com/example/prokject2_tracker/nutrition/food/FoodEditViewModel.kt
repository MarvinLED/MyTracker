package com.example.prokject2_tracker.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Blank is allowed (defaults to 0 on save); a non-blank value must parse as a number. */
private fun String.isBlankOrValidNumber(): Boolean = isBlank() || toLocaleDoubleOrNull() != null

/** Blank defaults to 0.0; a non-blank, already-validated value is parsed as-is. */
private fun String.toNutrientValue(): Double = toLocaleDoubleOrNull() ?: 0.0

data class FoodEditState(
    val id: String? = null,
    val name: String = "",
    val brand: String = "",
    val kcalPer100: String = "",
    val proteinPer100: String = "",
    val carbsPer100: String = "",
    val fatPer100: String = "",
    val saturatedFatPer100: String = "",
    val sugarPer100: String = "",
    val fiberPer100: String = "",
    val saltPer100: String = "",
    val servingName: String = "",
    val servingAmount: String = "",
    val tags: List<Tag> = emptyList(),
    val tagInput: String = "",
    val isSaved: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
            kcalPer100.isNotBlank() && kcalPer100.toLocaleDoubleOrNull() != null &&
            proteinPer100.isBlankOrValidNumber() &&
            carbsPer100.isBlankOrValidNumber() &&
            fatPer100.isBlankOrValidNumber() &&
            saturatedFatPer100.isBlankOrValidNumber() &&
            sugarPer100.isBlankOrValidNumber() &&
            fiberPer100.isBlankOrValidNumber() &&
            saltPer100.isBlankOrValidNumber()
}

@HiltViewModel
class FoodEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val route: FoodEditRoute = savedStateHandle.toRoute()
    private var existing: FoodItem? = null

    private val _state = MutableStateFlow(FoodEditState(id = route.foodId))
    val state: StateFlow<FoodEditState> = _state.asStateFlow()

    val allTags: StateFlow<List<Tag>> = tagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBrands: StateFlow<List<String>> = foodRepository.observeAllBrands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val foodId = route.foodId
        if (foodId != null) {
            viewModelScope.launch {
                foodRepository.getById(foodId)?.let { food ->
                    existing = food
                    _state.value = FoodEditState(
                        id = food.id,
                        name = food.name,
                        brand = food.brand.orEmpty(),
                        kcalPer100 = food.kcalPer100.toString(),
                        proteinPer100 = food.proteinPer100.toString(),
                        carbsPer100 = food.carbsPer100.toString(),
                        fatPer100 = food.fatPer100.toString(),
                        saturatedFatPer100 = food.saturatedFatPer100.toString(),
                        sugarPer100 = food.sugarPer100.toString(),
                        fiberPer100 = food.fiberPer100.toString(),
                        saltPer100 = food.saltPer100.toString(),
                        servingName = food.servingName.orEmpty(),
                        servingAmount = food.servingAmount?.toString().orEmpty(),
                        tags = tagRepository.getTagsForFoodOnce(food.id),
                    )
                }
            }
        }
    }

    fun onTagInputChange(value: String) { _state.value = _state.value.copy(tagInput = value) }

    fun addTag(tag: Tag) {
        val current = _state.value
        if (current.tags.none { it.id == tag.id }) {
            _state.value = current.copy(tags = current.tags + tag)
        }
    }

    /**
     * Attaches the typed name as a tag, reusing an existing tag (case-insensitive match) if one
     * exists. A brand-new name gets a throwaway local id — [save] persists tags by name, so this
     * id is never written; it only needs to be stable enough for the chip list in this session.
     */
    fun addTagFromInput() {
        val name = _state.value.tagInput.trim()
        if (name.isBlank()) return
        val current = _state.value
        if (current.tags.any { it.name.equals(name, ignoreCase = true) }) {
            _state.value = current.copy(tagInput = "")
            return
        }
        val tag = allTags.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: Tag(id = IdGenerator.newId(), name = name, createdAt = Instant.now())
        _state.value = current.copy(tags = current.tags + tag, tagInput = "")
    }

    fun removeTag(tag: Tag) {
        _state.value = _state.value.copy(tags = _state.value.tags.filterNot { it.id == tag.id })
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onBrandChange(value: String) { _state.value = _state.value.copy(brand = value) }
    fun onKcalChange(value: String) { _state.value = _state.value.copy(kcalPer100 = value) }
    fun onProteinChange(value: String) { _state.value = _state.value.copy(proteinPer100 = value) }
    fun onCarbsChange(value: String) { _state.value = _state.value.copy(carbsPer100 = value) }
    fun onFatChange(value: String) { _state.value = _state.value.copy(fatPer100 = value) }
    fun onSaturatedFatChange(value: String) { _state.value = _state.value.copy(saturatedFatPer100 = value) }
    fun onSugarChange(value: String) { _state.value = _state.value.copy(sugarPer100 = value) }
    fun onFiberChange(value: String) { _state.value = _state.value.copy(fiberPer100 = value) }
    fun onSaltChange(value: String) { _state.value = _state.value.copy(saltPer100 = value) }
    fun onServingNameChange(value: String) { _state.value = _state.value.copy(servingName = value) }
    fun onServingAmountChange(value: String) { _state.value = _state.value.copy(servingAmount = value) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            val brand = s.brand.ifBlank { null }
            val servingName = s.servingName.ifBlank { null }
            val servingAmount = s.servingAmount.toLocaleDoubleOrNull()
            val current = existing
            val savedFoodId: String
            if (current == null) {
                val created = foodRepository.create(
                    name = s.name,
                    brand = brand,
                    baseUnit = BaseUnit.G,
                    kcalPer100 = s.kcalPer100.toNutrientValue(),
                    proteinPer100 = s.proteinPer100.toNutrientValue(),
                    carbsPer100 = s.carbsPer100.toNutrientValue(),
                    fatPer100 = s.fatPer100.toNutrientValue(),
                    saturatedFatPer100 = s.saturatedFatPer100.toNutrientValue(),
                    sugarPer100 = s.sugarPer100.toNutrientValue(),
                    fiberPer100 = s.fiberPer100.toNutrientValue(),
                    saltPer100 = s.saltPer100.toNutrientValue(),
                    servingName = servingName,
                    servingAmount = servingAmount,
                )
                savedFoodId = created.id
            } else {
                savedFoodId = current.id
                foodRepository.update(
                    current,
                    current.copy(
                        name = s.name,
                        brand = brand,
                        baseUnit = BaseUnit.G,
                        kcalPer100 = s.kcalPer100.toNutrientValue(),
                        proteinPer100 = s.proteinPer100.toNutrientValue(),
                        carbsPer100 = s.carbsPer100.toNutrientValue(),
                        fatPer100 = s.fatPer100.toNutrientValue(),
                        saturatedFatPer100 = s.saturatedFatPer100.toNutrientValue(),
                        sugarPer100 = s.sugarPer100.toNutrientValue(),
                        fiberPer100 = s.fiberPer100.toNutrientValue(),
                        saltPer100 = s.saltPer100.toNutrientValue(),
                        servingName = servingName,
                        servingAmount = servingAmount,
                    ),
                )
            }
            tagRepository.setFoodTagsByName(savedFoodId, s.tags.map { it.name })
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
