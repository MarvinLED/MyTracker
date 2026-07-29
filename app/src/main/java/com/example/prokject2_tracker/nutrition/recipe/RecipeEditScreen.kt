package com.example.prokject2_tracker.nutrition.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.nutrition.food.FoodAmountInput
import com.example.prokject2_tracker.nutrition.food.FoodPickerDialog
import com.example.prokject2_tracker.nutrition.food.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fluidTypeNames by viewModel.fluidTypeNames.collectAsState()
    val pickerQuery by viewModel.pickerQuery.collectAsState()
    val pickerResults by viewModel.pickerResults.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    // The ingredient whose amount field should take the cursor — set when one is just added, and
    // cleared again by the row itself so a later recomposition can't steal the focus back.
    var focusTargetFoodId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        // imePadding lifts the frame above the keyboard, so the row that just took the cursor stays
        // visible instead of sitting behind the number pad.
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) "Rezept hinzufügen" else "Rezept bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = dismissingKeyboard(viewModel::save), enabled = state.isValid) { Text("Speichern") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.servings,
                onValueChange = viewModel::onServingsChange,
                label = { Text("Portionen") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.instructions,
                onValueChange = viewModel::onInstructionsChange,
                label = { Text("Zubereitung") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Zutaten", style = MaterialTheme.typography.titleSmall)
            state.ingredients.forEach { row ->
                IngredientEditRow(
                    row = row,
                    fluidTypeNames = fluidTypeNames,
                    requestFocus = focusTargetFoodId == row.foodId,
                    onFocusHandled = { focusTargetFoodId = null },
                    onAmountChange = { viewModel.updateIngredientAmount(row.foodId, it) },
                    onUnitSelected = { viewModel.selectIngredientUnit(row.foodId, it) },
                    onRemove = { viewModel.removeIngredient(row.foodId) },
                )
            }
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zutat hinzufügen")
            }

            FluidSummaryCard(
                fluids = state.ingredients.fluidTotals(fluidTypeNames),
                servings = state.servings.toLocaleDoubleOrNull(),
            )
        }
    }

    if (showPicker) {
        FoodPickerDialog(
            query = pickerQuery,
            results = pickerResults,
            onQueryChange = viewModel::onPickerQueryChange,
            onDismiss = { showPicker = false },
            onPick = { food ->
                viewModel.addIngredient(food)
                focusTargetFoodId = food.id
                showPicker = false
            },
        )
    }
}

/**
 * One ingredient: name and delete on top, amount plus unit chips below. Two lines rather than one,
 * because a name, a number field and a chip per unit never fit across a phone's width.
 */
@Composable
private fun IngredientEditRow(
    row: IngredientRow,
    fluidTypeNames: Map<String, String>,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitSelected: (String?) -> Unit,
    onRemove: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Adding an ingredient puts the cursor straight into its amount field and opens the number pad:
    // typing the amount is always the next thing to do.
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.foodName)
                val fluidName = row.fluidTypeId?.let { fluidTypeNames[it] }
                if (fluidName != null && row.fluidMl > 0.0) {
                    Text(
                        "davon ${row.fluidMl.formatCompact()} ml $fluidName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Entfernen")
            }
        }
        FoodAmountInput(
            amountText = row.amountText,
            onAmountChange = onAmountChange,
            units = row.units,
            selectedUnitId = row.selectedUnitId,
            onUnitSelected = onUnitSelected,
            baseUnit = row.baseUnit,
            focusRequester = focusRequester,
            modifier = Modifier.fillMaxWidth(),
        )
        // What the row contributes in grams, once it was entered as a count of a unit.
        row.selectedUnit?.let {
            row.amountBaseUnits?.let { grams ->
                Text(
                    "= ${grams.formatCompact()} ${row.baseUnit.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Per Getränkeart totals over the ingredient rows, keyed by name and in first-ingredient order. */
private fun List<IngredientRow>.fluidTotals(fluidTypeNames: Map<String, String>): List<Pair<String, Double>> =
    filter { it.fluidTypeId != null && it.fluidMl > 0.0 }
        .groupBy { it.fluidTypeId }
        .mapNotNull { (typeId, rows) ->
            fluidTypeNames[typeId]?.let { name -> name to rows.sumOf { it.fluidMl } }
        }

/**
 * What the recipe's drink-linked ingredients add up to. Spelled out per portion as well, because
 * that — not the whole-recipe amount — is what a Tagebuch entry of one portion logs as fluid.
 */
@Composable
private fun FluidSummaryCard(fluids: List<Pair<String, Double>>, servings: Double?) {
    if (fluids.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Enthaltene Flüssigkeiten", style = MaterialTheme.typography.titleSmall)
            fluids.forEach { (name, totalMl) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(name, modifier = Modifier.weight(1f))
                    val perServing = servings?.takeIf { it > 0.0 }?.let { totalMl / it }
                    Text(
                        buildString {
                            append("${totalMl.formatCompact()} ml")
                            perServing?.let { append(" · ${it.formatCompact()} ml / Portion") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Wird beim Hinzufügen zum Tagebuch automatisch bei den Flüssigkeiten mitgezählt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

