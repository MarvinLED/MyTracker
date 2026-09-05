package com.example.mytracker.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull

/** "g" or "ml" — what one unit of a food is measured in. */
fun BaseUnit.label(): String = if (this == BaseUnit.G) "g" else "ml"

/**
 * The amount in base units that [amountText] stands for: the number itself when no unit is picked,
 * otherwise that many [unit]s. Null when the text isn't a number.
 */
fun amountInBaseUnits(amountText: String, unit: FoodUnit?): Double? =
    amountText.toLocaleDoubleOrNull()?.let { typed ->
        if (unit == null) typed else typed * unit.amountBaseUnits
    }

/**
 * The same, but safe for a food that has no weight — see [FoodItem.portionUnitName].
 *
 * Without that name a missing unit means "the number is grams", which for such a food would read a
 * count of portions as a count of grams: a hundredfold error in the logged nutrition, with nothing
 * on screen to give it away. Takes the name loose rather than a whole [FoodItem] for the same
 * reason [com.example.mytracker.nutrition.food.fluidMlOf] does — the rows that need it carry the
 * loose fields, not the food.
 */
fun amountInBaseUnits(amountText: String, unit: FoodUnit?, portionUnitName: String?): Double? =
    amountText.toLocaleDoubleOrNull()?.let { typed ->
        when {
            unit != null -> typed * unit.amountBaseUnits
            portionUnitName != null -> typed * PORTION_BASE_UNITS
            else -> typed
        }
    }

/**
 * How a logged amount reads back: "2 × Scheibe (50 g)" when it was entered by unit, plain
 * "50 g" otherwise. The base-unit amount stays visible either way — it's the number the nutrition
 * was actually computed from.
 */
fun formatAmount(amountBaseUnits: Double, unitName: String?, unitCount: Double?, baseUnitLabel: String): String {
    val base = "${amountBaseUnits.formatDecimal(3)} $baseUnitLabel"
    if (unitName == null || unitCount == null) return base
    return "${unitCount.formatDecimal(3)} × $unitName ($base)"
}

/**
 * The same, for a food that has no weight: "2 × Riegel" and nothing in brackets. The gram figure is
 * bookkeeping there — one portion is stored as 100 base units — and showing it would state a weight
 * that was never known, the very thing such a food exists to avoid.
 */
fun formatPortionAmount(amountBaseUnits: Double, unitName: String?, unitCount: Double?, portionUnitName: String): String {
    val count = unitCount ?: (amountBaseUnits / PORTION_BASE_UNITS)
    return "${count.formatDecimal(3)} × ${unitName ?: portionUnitName}"
}

/**
 * The amount field plus a chip row to switch between the food's base unit and its named
 * [FoodUnit]s ("2 × Scheibe" instead of "50 g"). Shown wherever a Lebensmittel amount is entered —
 * Tagebuch, Tagebuch-Bearbeiten, Rezept-Zutaten — so all three behave identically.
 *
 * [amountText] always means "the number in the currently selected mode": grams when
 * [selectedUnitId] is null, otherwise a count of that unit. Converting to base units is the caller's
 * job via [amountInBaseUnits], because only the caller knows what to do with the result; on a mode
 * switch the caller prefills the field via [defaultAmountText].
 *
 * [trailingContent] puts something beside the field, on its line — the Tagebuch's "+" confirms the
 * amount from there. The unit chips stay above and full width either way: pulled into the same row
 * they would only have part of the width left to wrap in.
 */
@Composable
fun FoodAmountInput(
    amountText: String,
    onAmountChange: (String) -> Unit,
    units: List<FoodUnit>,
    selectedUnitId: String?,
    onUnitSelected: (String?) -> Unit,
    baseUnit: BaseUnit,
    modifier: Modifier = Modifier,
    /**
     * Non-null for a food that has no weight — see [FoodItem.portionUnitName]. The base-unit chip
     * then disappears: grams are not a smaller amount of such a food, they are a number nobody has.
     */
    portionUnitName: String? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    fieldModifier: Modifier = Modifier.fillMaxWidth(),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectedUnit = units.firstOrNull { it.id == selectedUnitId }
    val baseLabel = baseUnit.label()
    val isPortionOnly = portionUnitName != null

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // One chip and no choice would be a control that cannot be operated; the field's own label
        // already says which portion is being counted.
        if (units.isNotEmpty() && !isPortionOnly) {
            // Above the field, not below: focusing the field scrolls it to the bottom of the frame
            // with the keyboard open, and anything underneath would be hidden behind the keyboard.
            // FlowRow, not Row: several unit names in one bounded row would squeeze the last chip
            // down to a character-per-line label.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = selectedUnit == null,
                    onClick = { onUnitSelected(null) },
                    enabled = enabled,
                    label = { Text(baseLabel) },
                )
                units.forEach { unit ->
                    FilterChip(
                        selected = unit.id == selectedUnitId,
                        onClick = { onUnitSelected(unit.id) },
                        enabled = enabled,
                        label = { Text("${unit.name} (${unit.amountBaseUnits.formatCompact()} $baseLabel)") },
                    )
                }
            }
        }

        val field: @Composable (Modifier) -> Unit = { fieldOwnModifier ->
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChange,
                label = {
                    Text(
                        when {
                            portionUnitName != null -> "Anzahl ($portionUnitName)"
                            selectedUnit == null -> "Menge ($baseLabel)"
                            else -> "Anzahl (${selectedUnit.name})"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                // "Bestätigen" on the number pad should put the keyboard away, not insert a newline.
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                ),
                singleLine = true,
                enabled = enabled,
                modifier = focusRequester?.let { fieldOwnModifier.focusRequester(it) } ?: fieldOwnModifier,
            )
        }

        if (trailingContent == null) {
            field(fieldModifier)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The field takes the row, so the trailing control keeps its intrinsic width no
                // matter how long the unit's name made the label.
                field(Modifier.weight(1f))
                trailingContent()
            }
        }
    }
}

/** Prefilled for the base unit: a food's values are given per 100 g/ml. */
const val DEFAULT_BASE_AMOUNT = "100"

/** Prefilled for a named unit: tapping "Scheibe" almost always means one of them. */
const val DEFAULT_UNIT_COUNT = "1"

/**
 * The amount text to show after switching modes: not the old amount re-expressed, but the usual
 * starting value of the new mode — 100 g/ml, or 1 × the named unit. Only prefilled, so it stays a
 * single tap away from confirming and can be typed over.
 */
fun defaultAmountText(unit: FoodUnit?): String =
    if (unit == null) DEFAULT_BASE_AMOUNT else DEFAULT_UNIT_COUNT

/** The same, for a food whose amounts are only ever a count of portions — never 100 of them. */
fun defaultAmountText(unit: FoodUnit?, portionUnitName: String?): String =
    if (unit == null && portionUnitName == null) DEFAULT_BASE_AMOUNT else DEFAULT_UNIT_COUNT
