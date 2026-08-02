package com.example.prokject2_tracker.nutrition.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
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
            // Tighter than the 12.dp between the form fields above: the ingredients read as one
            // list, not as a stack of separate controls.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.ingredients.forEach { row ->
                    IngredientEditRow(
                        row = row,
                        requestFocus = focusTargetFoodId == row.foodId,
                        onFocusHandled = { focusTargetFoodId = null },
                        onAmountChange = { viewModel.updateIngredientAmount(row.foodId, it) },
                        onUnitSelected = { viewModel.selectIngredientUnit(row.foodId, it) },
                        onRemove = { viewModel.removeIngredient(row.foodId) },
                    )
                }
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

/** The amount field: wide enough for "1000", narrow enough to leave the name room to breathe. */
private val AmountFieldWidth = 76.dp

/**
 * One ingredient, one line: name, amount, unit, remove. The unit is a menu rather than the chip row
 * [FoodAmountInput] draws — chips are right where a single amount is the whole screen (Tagebuch),
 * but a recipe is a *list* of amounts, and a chip row per ingredient turned five ingredients into a
 * screenful of scrolling.
 *
 * What the row no longer spells out: the per-ingredient "davon x ml Milch" (the Flüssigkeiten card
 * below sums the same thing) and the gram equivalent of a unit amount, which is shown next to the
 * name only when the number alone would be ambiguous.
 */
@Composable
private fun IngredientEditRow(
    row: IngredientRow,
    requestFocus: Boolean,
    onFocusHandled: () -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitSelected: (String?) -> Unit,
    onRemove: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Adding an ingredient puts the cursor straight into its amount field and opens the number pad:
    // typing the amount is always the next thing to do.
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.foodName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // fill = false: the name gives its leftover width to the gram equivalent instead of
                // pushing it off the row.
                modifier = Modifier.weight(1f, fill = false),
            )
            // "2 × Scheibe" says nothing about how much that is — in grams the number speaks for
            // itself, so this only appears in unit mode.
            row.selectedUnit?.let {
                row.amountBaseUnits?.let { grams ->
                    Text(
                        "${grams.formatCompact()} ${row.baseUnit.label()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        OutlinedTextField(
            value = row.amountText,
            onValueChange = onAmountChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            // "Bestätigen" on the number pad should put the keyboard away, not jump to the next row.
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            ),
            modifier = Modifier
                .width(AmountFieldWidth)
                .focusRequester(focusRequester),
        )
        UnitPicker(row = row, onUnitSelected = onUnitSelected)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "${row.foodName} entfernen")
        }
    }
}

/**
 * Base unit or one of the food's named units. A food without named units has nothing to pick, so it
 * shows the plain "g"/"ml" the number is in rather than a menu that opens onto a single entry.
 */
@Composable
private fun UnitPicker(row: IngredientRow, onUnitSelected: (String?) -> Unit) {
    val baseLabel = row.baseUnit.label()
    if (row.units.isEmpty()) {
        Text(baseLabel, style = MaterialTheme.typography.bodyMedium)
        return
    }

    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(start = 8.dp, end = 0.dp),
        ) {
            Text(
                row.selectedUnit?.name ?: baseLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 72.dp),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Einheit wählen")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(baseLabel) },
                onClick = {
                    onUnitSelected(null)
                    expanded = false
                },
            )
            row.units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.name} (${unit.amountBaseUnits.formatCompact()} $baseLabel)") },
                    onClick = {
                        onUnitSelected(unit.id)
                        expanded = false
                    },
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

