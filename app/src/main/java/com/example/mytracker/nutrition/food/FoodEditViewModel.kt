package com.example.mytracker.nutrition.food

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mytracker.core.util.IdGenerator
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.fluid.FluidType
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
    /**
     * Which unit row the values above were entered for; null means the usual "pro 100 g".
     *
     * Two quite different things come out of one checkbox, and which one depends solely on whether
     * that row has a weight. **With** a weight the values are simply converted — "1 Riegel = 45 g,
     * 230 kcal" becomes 511 kcal/100 g and the food is an ordinary one afterwards. **Without** one
     * the food has no weight at all, and grams disappear from it for good; see
     * [FoodItem.portionUnitName].
     */
    val referenceUnitIndex: Int? = null,
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
    /** The row the entered values belong to, or null while they are plain per-100-g values. */
    val referenceUnit: UnitRow? get() = referenceUnitIndex?.let { units.getOrNull(it) }

    /**
     * True once the values belong to a portion whose weight is unknown. From here on the food has no
     * grams: nothing can be converted, so nothing else may be offered either.
     */
    val isPortionOnly: Boolean get() = referenceUnit?.let { it.name.isNotBlank() && it.amount.isBlank() } == true

    /** What the value fields are labelled with — "100 g", or the portion the numbers are for. */
    val valueBasisLabel: String
        get() = referenceUnit?.name?.takeIf { it.isNotBlank() } ?: "100 g"

    /** Every row except the reference one — the rows a weightless portion leaves no room for. */
    val otherUnits: List<UnitRow>
        get() = units.filterIndexed { index, _ -> index != referenceUnitIndex }

    /**
     * What the typed values are multiplied by to become the per-100 figures that get stored.
     *
     * A weighed portion is converted once, here, and then forgotten: the food is an ordinary one
     * afterwards, comparable with every other. A weightless one is stored exactly as typed, because
     * its single portion *is* 100 base units — see [FoodItem.portionUnitName].
     */
    val storageFactor: Double
        get() = when {
            isPortionOnly -> 1.0
            else -> referenceGrams?.let { 100.0 / it } ?: 1.0
        }

    /** The name this food will be stored under, or null when it keeps a weight and needs none. */
    val savedPortionUnitName: String?
        get() = referenceUnit?.name?.trim()?.takeIf { isPortionOnly && it.isNotEmpty() }

    /** The reference portion's weight, or null when it has none — the blank that decides everything. */
    private val referenceGrams: Double?
        get() = referenceUnit?.amount?.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }

    /**
     * "≈ 511 kcal / 100 g" while the values are being entered for a weighed portion, so the figure
     * the food will actually be stored with is visible before saving rather than after.
     */
    val kcalPer100Hint: String?
        get() {
            referenceGrams ?: return null
            val kcal = kcalPer100.toLocaleDoubleOrNull() ?: return null
            return "≈ ${(kcal * storageFactor).formatDecimal(1)} kcal / 100 g"
        }

    /** The units a price can currently be entered for — half-filled rows aren't offered. */
    val priceUnitOptions: List<UnitRow> get() = units.filter { it.isComplete || it == referenceUnit }

    /**
     * "≈ 0,50 € / 100 g" while a price is being typed for a named unit, so the comparison value is
     * visible before saving. Null when there is nothing (yet) to convert.
     */
    val pricePer100Hint: String?
        get() {
            // A food without a weight has no 100 g to be a price of. Left to `pricePer100`, the
            // blank amount would read as "already per 100" and the price would be restated as a
            // comparison value that cannot exist.
            if (isPortionOnly) return null
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
            otherUnits.all { it.isBlank || it.isComplete } &&
            isReferenceRowValid &&
            // A weightless portion is the only thing this food can be measured in, so anything else
            // standing beside it would be an amount nobody can convert.
            (!isPortionOnly || otherUnits.all { it.isBlank }) &&
            price.isBlankOrValidNumber() &&
            (fluidTypeId == null || fluidMlPer100.isBlankOrValidNumber())

    /**
     * The reference row is the one row allowed to have no amount — that blank is what says "no
     * weight known". It still needs a name, because the name is what the portion is called
     * everywhere afterwards.
     */
    private val isReferenceRowValid: Boolean
        get() {
            val row = referenceUnit ?: return referenceUnitIndex == null
            return row.name.isNotBlank() && (row.amount.isBlank() || row.isComplete)
        }
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
                    val units = foodRepository.getUnits(food.id)
                    val portionIndex = food.portionUnitName
                        ?.let { name -> units.indexOfFirst { it.name == name } }
                        ?.takeIf { it >= 0 }
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
                        units = units.mapIndexed { index, unit ->
                            UnitRow(
                                name = unit.name,
                                // The reference portion of a weightless food shows an empty amount
                                // rather than the 100 it is stored as — that 100 is bookkeeping,
                                // not something the user ever typed or should be shown.
                                amount = if (index == portionIndex) "" else unit.amountBaseUnits.formatDecimal(3),
                            )
                        },
                        referenceUnitIndex = portionIndex,
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

    /**
     * Marks the row the entered values belong to, or unmarks it when it is already the reference.
     * Only one row can be it — the values are one set of numbers, and they are for one thing.
     *
     * Picking a row also moves the price onto it when the price was "pro 100 g": a food that turns
     * out to have no weight has no per-100-g price either, and leaving the old basis would keep a
     * number that no longer refers to anything.
     */
    fun onReferenceUnitToggle(index: Int) {
        val current = _state.value
        val next = if (current.referenceUnitIndex == index) null else index
        val updated = current.copy(referenceUnitIndex = next)
        _state.value = if (updated.isPortionOnly) {
            updated.copy(priceUnitName = updated.referenceUnit?.name)
        } else {
            updated
        }
    }

    fun removeUnitRow(index: Int) {
        val current = _state.value
        val removed = current.units.getOrNull(index)?.name
        _state.value = current.copy(
            units = current.units.filterIndexed { i, _ -> i != index },
            // An index into a list that just lost a row: the reference is gone with it, or has
            // shifted down one. Left alone it would silently come to mean a different row.
            referenceUnitIndex = current.referenceUnitIndex?.let { reference ->
                when {
                    reference == index -> null
                    reference > index -> reference - 1
                    else -> reference
                }
            },
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
            val portionUnitName = s.savedPortionUnitName
            val toPer100 = s.storageFactor
            fun String.toStoredValue(): Double = toNutrientValue() * toPer100

            val unitDrafts = if (portionUnitName != null) {
                listOf(FoodUnitDraft(name = portionUnitName, amountBaseUnits = PORTION_BASE_UNITS))
            } else {
                s.units.filter { it.isComplete }.map {
                    FoodUnitDraft(name = it.name, amountBaseUnits = it.amount.toLocaleDoubleOrNull() ?: 0.0)
                }
            }
            // A picked type with a blank amount still means "besteht ganz daraus" — 100 ml per 100 g,
            // or the whole of one portion. A typed amount is converted like every other value, so
            // "1 Dose = 330 ml" survives being entered against the portion rather than against 100 g.
            val fluidMlPer100 = s.fluidTypeId?.let {
                s.fluidMlPer100.toLocaleDoubleOrNull()?.let { ml -> ml * toPer100 } ?: 100.0
            }
            // A blank or non-positive price is "kein Preis erfasst", not "kostet 0 €". The basis is
            // only kept if it survived the unit edits above — otherwise the price is per 100 g.
            val price = s.price.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
            // A food without a weight has no "pro 100 g" price either, so its price is the
            // portion's whether or not the basis was ever switched over by hand.
            val priceUnitName = if (portionUnitName != null) {
                portionUnitName.takeIf { price != null }
            } else {
                s.priceUnitName?.trim()?.takeIf { name -> unitDrafts.any { it.name.trim() == name } }
            }
            val current = existing
            val savedFoodId: String
            if (current == null) {
                val created = foodRepository.create(
                    name = s.name,
                    brand = brand,
                    baseUnit = BaseUnit.G,
                    kcalPer100 = s.kcalPer100.toStoredValue(),
                    proteinPer100 = s.proteinPer100.toStoredValue(),
                    carbsPer100 = s.carbsPer100.toStoredValue(),
                    fatPer100 = s.fatPer100.toStoredValue(),
                    saturatedFatPer100 = s.saturatedFatPer100.toStoredValue(),
                    sugarPer100 = s.sugarPer100.toStoredValue(),
                    fiberPer100 = s.fiberPer100.toStoredValue(),
                    saltPer100 = s.saltPer100.toStoredValue(),
                    fluidTypeId = s.fluidTypeId,
                    fluidMlPer100 = fluidMlPer100,
                    price = price,
                    priceUnitName = priceUnitName,
                    portionUnitName = portionUnitName,
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
                        kcalPer100 = s.kcalPer100.toStoredValue(),
                        proteinPer100 = s.proteinPer100.toStoredValue(),
                        carbsPer100 = s.carbsPer100.toStoredValue(),
                        fatPer100 = s.fatPer100.toStoredValue(),
                        saturatedFatPer100 = s.saturatedFatPer100.toStoredValue(),
                        sugarPer100 = s.sugarPer100.toStoredValue(),
                        fiberPer100 = s.fiberPer100.toStoredValue(),
                        saltPer100 = s.saltPer100.toStoredValue(),
                        fluidTypeId = s.fluidTypeId,
                        fluidMlPer100 = fluidMlPer100,
                        price = price,
                        priceUnitName = priceUnitName,
                        portionUnitName = portionUnitName,
                    ),
                )
            }
            tagRepository.setFoodTagsByName(savedFoodId, s.tags.map { it.name })
            foodRepository.setUnits(savedFoodId, unitDrafts)
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
