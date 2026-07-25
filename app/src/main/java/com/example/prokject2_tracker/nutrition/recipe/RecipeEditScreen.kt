package com.example.prokject2_tracker.nutrition.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val fluidTypeNames by viewModel.fluidTypeNames.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        modifier = modifier,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                    OutlinedTextField(
                        value = row.amountText,
                        onValueChange = { viewModel.updateIngredientAmount(row.foodId, it) },
                        label = { Text(if (row.baseUnit == BaseUnit.G) "g" else "ml") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(100.dp),
                    )
                    IconButton(onClick = { viewModel.removeIngredient(row.foodId) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Entfernen")
                    }
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
            viewModel = viewModel,
            onDismiss = { showPicker = false },
            onPick = { food ->
                viewModel.addIngredient(food)
                showPicker = false
            },
        )
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

@Composable
private fun FoodPickerDialog(
    viewModel: RecipeEditViewModel,
    onDismiss: () -> Unit,
    onPick: (FoodItem) -> Unit,
) {
    val query by viewModel.pickerQuery.collectAsState()
    val results by viewModel.pickerResults.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        title = { Text("Lebensmittel wählen") },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onPickerQueryChange,
                    label = { Text("Suche") },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(results, key = { it.id }) { food ->
                        Text(
                            food.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(food) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
    )
}
