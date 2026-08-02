package com.example.prokject2_tracker.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.fluid.FluidType
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

/** One editable row of the "Einheiten" section; [amount] is free text until save. */
data class UnitRow(val name: String = "", val amount: String = "")

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
    val units: List<UnitRow> = emptyList(),
    /** Blank = no price recorded; see [FoodItem.price]. */
    val price: String = "",
    /** null = the price is for 100 g, otherwise the name of one of [units]; see [FoodItem.priceUnitName]. */
    val priceUnitName: String? = null,
    /** null = this food isn't (partly) a drink; see [FoodItem.fluidTypeId]. */
    val fluidTypeId: String? = null,
    /** Blank means "consists entirely of it" and is saved as 100 ml per 100 g. */
    val fluidMlPer100: String = "",
    val tags: List<Tag> = emptyList(),
    val tagInput: String = "",
    val isSaved: Boolean = false,
) {
    /** The units a price can currently be entered for — half-filled rows aren't offered. */
    val priceUnitOptions: List<UnitRow> get() = units.filter { it.isComplete }

    /**
     * "≈ 0,50 € / 100 g" while a price is being typed for a named unit, so the comparison value is
     * visible before saving. Null when there is nothing (yet) to convert.
     */
    val pricePer100Hint: String?
        get() {
            val basis = priceUnitOptions.firstOrNull { it.name == priceUnitName } ?: return null
            val per100 = pricePer100(price.toLocaleDoubleOrNull(), basis.amount.toLocaleDoubleOrNull())
                ?: return null
            return "≈ ${formatEuro(per100)} / 100 g"
        }

    val isValid: Boolean
        get() = name.isNotBlank() &&
            kcalPer100.isNotBlank() && kcalPer100.toLocaleDoubleOrNull() != null &&
            proteinPer100.isBlankOrValidNumber() &&
            carbsPer100.isBlankOrValidNumber() &&
            fatPer100.isBlankOrValidNumber() &&
            saturatedFatPer100.isBlankOrValidNumber() &&
            sugarPer100.isBlankOrValidNumber() &&
            fiberPer100.isBlankOrValidNumber() &&
            saltPer100.isBlankOrValidNumber() &&
            units.all { it.isBlank || it.isComplete } &&
            price.isBlankOrValidNumber() &&
            (fluidTypeId == null || fluidMlPer100.isBlankOrValidNumber())
}

/** An untouched row is simply dropped on save; a half-filled one blocks it. */
private val UnitRow.isBlank: Boolean get() = name.isBlank() && amount.isBlank()

private val UnitRow.isComplete: Boolean
    get() = name.isNotBlank() && amount.toLocaleDoubleOrNull()?.let { it > 0.0 } == true

@HiltViewModel
class FoodEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val tagRepository: TagRepository,
    fluidRepository: FluidRepository,
) : ViewModel() {
    private val route: FoodEditRoute = savedStateHandle.toRoute()
    private var existing: FoodItem? = null

    private val _state = MutableStateFlow(FoodEditState(id = route.foodId))
    val state: StateFlow<FoodEditState> = _state.asStateFlow()

    val allTags: StateFlow<List<Tag>> = tagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBrands: StateFlow<List<String>> = foodRepository.observeAllBrands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fluidTypes: StateFlow<List<FluidType>> = fluidRepository.observeTypes()
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
                        units = foodRepository.getUnits(food.id).map {
                            UnitRow(name = it.name, amount = it.amountBaseUnits.formatDecimal(3))
                        },
                        price = food.price?.formatDecimal(2).orEmpty(),
                        priceUnitName = food.priceUnitName,
                        fluidTypeId = food.fluidTypeId,
                        fluidMlPer100 = food.fluidMlPer100?.toString().orEmpty(),
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
    fun onFluidMlPer100Change(value: String) { _state.value = _state.value.copy(fluidMlPer100 = value) }

    fun onPriceChange(value: String) { _state.value = _state.value.copy(price = value) }

    /** null selects "pro 100 g"; any other value must be one of the unit rows' names. */
    fun onPriceUnitChange(unitName: String?) { _state.value = _state.value.copy(priceUnitName = unitName) }

    fun addUnitRow() {
        _state.value = _state.value.copy(units = _state.value.units + UnitRow())
    }

    /** Renaming the unit a price refers to carries the price along instead of silently dropping it. */
    fun onUnitNameChange(index: Int, value: String) {
        val renamed = _state.value.units.getOrNull(index)?.name
        updateUnitRow(index) { it.copy(name = value) }
        if (renamed != null && renamed == _state.value.priceUnitName) {
            _state.value = _state.value.copy(priceUnitName = value.ifBlank { null })
        }
    }

    fun onUnitAmountChange(index: Int, value: String) = updateUnitRow(index) { it.copy(amount = value) }

    fun removeUnitRow(index: Int) {
        val removed = _state.value.units.getOrNull(index)?.name
        val current = _state.value
        _state.value = current.copy(
            units = current.units.filterIndexed { i, _ -> i != index },
            // The basis is gone, so the price falls back to "pro 100 g" rather than to a unit that
            // no longer exists.
            priceUnitName = if (removed == current.priceUnitName) null else current.priceUnitName,
        )
    }

    private fun updateUnitRow(index: Int, transform: (UnitRow) -> UnitRow) {
        _state.value = _state.value.copy(
            units = _state.value.units.mapIndexed { i, row -> if (i == index) transform(row) else row },
        )
    }

    /** Picking a type pre-fills 100 ml/100 g ("besteht ganz daraus"); clearing it drops the amount too. */
    fun onFluidTypeChange(typeId: String?) {
        val current = _state.value
        _state.value = when {
            typeId == null -> current.copy(fluidTypeId = null, fluidMlPer100 = "")
            current.fluidMlPer100.isBlank() -> current.copy(fluidTypeId = typeId, fluidMlPer100 = "100")
            else -> current.copy(fluidTypeId = typeId)
        }
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            val brand = s.brand.ifBlank { null }
            val unitDrafts = s.units.filter { it.isComplete }.map {
                FoodUnitDraft(name = it.name, amountBaseUnits = it.amount.toLocaleDoubleOrNull() ?: 0.0)
            }
            // A picked type with a blank amount means "besteht ganz aus dieser Flüssigkeit" = 100 ml/100 g.
            val fluidMlPer100 = s.fluidTypeId?.let { s.fluidMlPer100.toLocaleDoubleOrNull() ?: 100.0 }
            // A blank or non-positive price is "kein Preis erfasst", not "kostet 0 €". The basis is
            // only kept if it survived the unit edits above — otherwise the price is per 100 g.
            val price = s.price.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
            val priceUnitName = s.priceUnitName?.trim()
                ?.takeIf { name -> unitDrafts.any { it.name.trim() == name } }
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
                    fluidTypeId = s.fluidTypeId,
                    fluidMlPer100 = fluidMlPer100,
                    price = price,
                    priceUnitName = priceUnitName,
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
                        fluidTypeId = s.fluidTypeId,
                        fluidMlPer100 = fluidMlPer100,
                        price = price,
                        priceUnitName = priceUnitName,
                    ),
                )
            }
            tagRepository.setFoodTagsByName(savedFoodId, s.tags.map { it.name })
            foodRepository.setUnits(savedFoodId, unitDrafts)
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
