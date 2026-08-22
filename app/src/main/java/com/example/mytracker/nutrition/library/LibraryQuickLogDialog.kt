package com.example.mytracker.nutrition.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.ui.dismissingKeyboard
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.nutrition.diary.MealType
import com.example.mytracker.nutrition.diary.label
import com.example.mytracker.nutrition.food.FoodAmountInput
import com.example.mytracker.nutrition.food.FoodUnit

/**
 * "Ins Tagebuch" for one Lebensmittel or Rezept from the Bibliothek: the meal, the amount, done.
 *
 * The amount row is the same [FoodAmountInput] the Tagebuch and the Rezept editor use, so a food's
 * named units ("2 × Scheibe") work here too. A Rezept has no units and gets a plain Portionen field,
 * just like in the Tagebuch's picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryQuickLogDialog(
    target: QuickLogTarget,
    mealType: MealType,
    onMealTypeChange: (MealType) -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    units: List<FoodUnit>,
    selectedUnitId: String?,
    onUnitSelected: (String?) -> Unit,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.name) },
        text = {
            // Scrollable: with the keyboard open the dialog keeps little height, and the meal chips
            // plus a food's unit chips can fill more than that.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The day is not selectable here — say so rather than let the entry turn up on a day
                // the user did not pick.
                Text(
                    "Wird für heute eingetragen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { onMealTypeChange(type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
                when (target) {
                    is QuickLogTarget.Food -> FoodAmountInput(
                        amountText = amountText,
                        onAmountChange = onAmountChange,
                        units = units,
                        selectedUnitId = selectedUnitId,
                        onUnitSelected = onUnitSelected,
                        baseUnit = target.food.baseUnit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is QuickLogTarget.Recipe -> {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = onAmountChange,
                            label = { Text("Portionen") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${target.recipe.perServing.kcal.formatCompact()} kcal / Portion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismissingKeyboard(onConfirm), enabled = canConfirm) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
