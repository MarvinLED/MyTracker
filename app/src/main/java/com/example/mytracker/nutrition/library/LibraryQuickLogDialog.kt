package com.example.mytracker.nutrition.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.ui.dismissingKeyboard
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.nutrition.diary.MealType
import com.example.mytracker.nutrition.diary.icon
import com.example.mytracker.nutrition.diary.label
import com.example.mytracker.nutrition.diary.shortLabel
import com.example.mytracker.nutrition.food.FoodAmountInput
import com.example.mytracker.nutrition.food.FoodUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "Hinzufügen" for one Lebensmittel or Rezept from the Bibliothek: the meal, the amount, what it
 * comes to, done. Opened by tapping the row itself — the two buttons on a row edit and delete.
 *
 * The amount row is the same [FoodAmountInput] the Rezept editor uses, so a food's named units
 * ("2 × Scheibe") work here too. A Rezept has no units and gets a plain Portionen field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryQuickLogDialog(
    target: QuickLogTarget,
    /** The day the entry lands on; null means the screen has none of its own, so: today. */
    contextDay: Long?,
    mealType: MealType,
    onMealTypeChange: (MealType) -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    units: List<FoodUnit>,
    selectedUnitId: String?,
    onUnitSelected: (String?) -> Unit,
    preview: LoggedNutrition?,
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
                // Which day this writes to is never guessable from the Bibliothek — say it.
                Text(
                    "Wird für ${logDayLabel(contextDay)} eingetragen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MealTypeChips(selected = mealType, onSelect = onMealTypeChange)
                when (target) {
                    is QuickLogTarget.Food -> FoodAmountInput(
                        amountText = amountText,
                        onAmountChange = onAmountChange,
                        units = units,
                        selectedUnitId = selectedUnitId,
                        onUnitSelected = onUnitSelected,
                        baseUnit = target.food.baseUnit,
                        portionUnitName = target.food.portionUnitName,
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
                preview?.let { NutritionPreview(it) }
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

/**
 * The four meals in one row. Shortened names, because four full ones ("Frühstück", "Mittagessen",
 * …) do not fit across a dialog; Snack is its own picture, and carries its name for the screen
 * reader instead.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealTypeChips(
    selected: MealType,
    onSelect: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        MealType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = {
                    if (type == MealType.SNACK) {
                        Icon(
                            type.icon(),
                            contentDescription = type.label(),
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(type.shortLabel())
                    }
                },
            )
        }
    }
}

/** What the typed amount comes to. The kcal figure is the one people look for, so it leads. */
@Composable
fun NutritionPreview(nutrition: LoggedNutrition, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${nutrition.kcal.formatCompact()} kcal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "Protein ${nutrition.protein.formatCompact()} g · " +
                    "Kohlenhydrate ${nutrition.carbs.formatCompact()} g · " +
                    "Fett ${nutrition.fat.formatCompact()} g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** "heute" for today whichever way the screen was reached, otherwise the date itself. */
@Composable
fun logDayLabel(contextDay: Long?): String {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN) }
    val day = contextDay ?: return "heute"
    if (day == DateUtils.todayEpochDay()) return "heute"
    return formatter.format(DateUtils.localDateOfEpochDay(day))
}
