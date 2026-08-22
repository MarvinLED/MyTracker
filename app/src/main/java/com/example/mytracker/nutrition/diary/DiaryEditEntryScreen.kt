package com.example.mytracker.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.dismissingKeyboard
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.nutrition.food.FoodAmountInput
import com.example.mytracker.nutrition.food.FoodPickerDialog
import com.example.mytracker.nutrition.food.label

/**
 * Editing an existing Tagebuch entry: the amount, the meal it belongs to, and — for a Rezept — how
 * that recipe was actually cooked *on this day*. The per-day ingredient list is deliberately scoped
 * to this one entry: the library recipe and every other day stay exactly as they are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditEntryScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryEditEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fluidTypeNames by viewModel.fluidTypeNames.collectAsState()
    val pickerQuery by viewModel.pickerQuery.collectAsState()
    val pickerResults by viewModel.pickerResults.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        // No imePadding here: AppScaffold's contentWindowInsets are safeDrawing, which already
        // includes the IME, so adding it again left a keyboard-high empty strip at the bottom.
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Eintrag bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = dismissingKeyboard(viewModel::save),
                    enabled = state.isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Speichern", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        val entry = state.entry
        if (entry == null) {
            Text("Eintrag nicht gefunden.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entry.sourceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "${entry.kcal.formatCompact()} kcal · Protein ${entry.protein.formatCompact()} g · " +
                            "Kohlenhydrate ${entry.carbs.formatCompact()} g · Fett ${entry.fat.formatCompact()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (state.isQuantityEditable) {
                val food = state.sourceFood
                if (food != null) {
                    FoodAmountInput(
                        amountText = state.quantityText,
                        onAmountChange = viewModel::onQuantityChange,
                        units = state.entryUnits,
                        selectedUnitId = state.selectedUnitId,
                        onUnitSelected = viewModel::onSelectUnit,
                        baseUnit = food.baseUnit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = state.quantityText,
                        onValueChange = viewModel::onQuantityChange,
                        label = { Text(if (state.isRecipe) "Portionen" else "Menge (${entry.quantityUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Text("Mahlzeit", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = state.mealType == type,
                        onClick = { viewModel.onMealTypeChange(type) },
                        label = { Text(type.label()) },
                    )
                }
            }

            if (state.isRecipe) {
                HorizontalDivider()
                DayRecipeSection(
                    state = state,
                    fluidTypeNames = fluidTypeNames,
                    onAmountChange = viewModel::onIngredientAmountChange,
                    onUnitSelected = viewModel::onSelectIngredientUnit,
                    onRemove = viewModel::removeIngredient,
                    onAdd = { showPicker = true },
                    onReset = viewModel::resetIngredientsToRecipe,
                )
            }
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
                showPicker = false
            },
        )
    }
}

/** "Was heute anders war": the recipe's ingredients for this entry's day only. */
@Composable
private fun DayRecipeSection(
    state: DiaryEditEntryState,
    fluidTypeNames: Map<String, String>,
    onAmountChange: (String, String) -> Unit,
    onUnitSelected: (String, String?) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onReset: () -> Unit,
) {
    Text("Rezept an diesem Tag", style = MaterialTheme.typography.titleSmall)
    Text(
        "Änderungen hier gelten nur für diesen Eintrag — das Rezept in der Bibliothek und andere " +
            "Tage bleiben unverändert.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.ingredients.forEach { row ->
        // Two lines like the Rezept editor's rows: name and delete on top, amount plus unit chips
        // below, since a name, a number field and a chip per unit never fit across a phone's width.
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                IconButton(onClick = { onRemove(row.foodId) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Zutat entfernen")
                }
            }
            FoodAmountInput(
                amountText = row.amountText,
                onAmountChange = { onAmountChange(row.foodId, it) },
                units = row.units,
                selectedUnitId = row.selectedUnitId,
                onUnitSelected = { onUnitSelected(row.foodId, it) },
                baseUnit = row.baseUnit,
                modifier = Modifier.fillMaxWidth(),
            )
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Zutat hinzufügen")
        }
        if (state.hadDayIngredients) {
            OutlinedButton(onClick = onReset) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Auf Rezept zurücksetzen")
            }
        }
    }
}

