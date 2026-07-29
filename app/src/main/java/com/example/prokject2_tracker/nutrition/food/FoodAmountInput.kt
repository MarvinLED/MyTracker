package com.example.prokject2_tracker.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull

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
 * The amount field plus a chip row to switch between the food's base unit and its named
 * [FoodUnit]s ("2 × Scheibe" instead of "50 g"). Shown wherever a Lebensmittel amount is entered —
 * Tagebuch, Tagebuch-Bearbeiten, Rezept-Zutaten — so all three behave identically.
 *
 * [amountText] always means "the number in the currently selected mode": grams when
 * [selectedUnitId] is null, otherwise a count of that unit. Converting is the caller's job via
 * [amountInBaseUnits], because only the caller knows what to do with the result.
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
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    fieldModifier: Modifier = Modifier.fillMaxWidth(),
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val selectedUnit = units.firstOrNull { it.id == selectedUnitId }
    val baseLabel = baseUnit.label()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (units.isNotEmpty()) {
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

        OutlinedTextField(
            value = amountText,
            onValueChange = onAmountChange,
            label = {
                Text(
                    if (selectedUnit == null) "Menge ($baseLabel)" else "Anzahl (${selectedUnit.name})",
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
            modifier = focusRequester?.let { fieldModifier.focusRequester(it) } ?: fieldModifier,
        )
    }
}

/**
 * The amount text to show after switching modes: keep the *same real amount*, re-expressed. Typing
 * 50 g and then tapping "Scheibe" (25 g) leaves "2", not "50".
 */
fun convertAmountText(amountText: String, from: FoodUnit?, to: FoodUnit?): String {
    val base = amountInBaseUnits(amountText, from) ?: return amountText
    val converted = if (to == null) base else base / to.amountBaseUnits
    return converted.formatDecimal(3)
}
