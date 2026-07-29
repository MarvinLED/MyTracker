package com.example.prokject2_tracker.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.fluid.FluidType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val allBrands by viewModel.allBrands.collectAsState()
    val fluidTypes by viewModel.fluidTypes.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) "Lebensmittel hinzufügen" else "Lebensmittel bearbeiten") },
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
                value = state.brand,
                onValueChange = viewModel::onBrandChange,
                label = { Text("Marke") },
                modifier = Modifier.fillMaxWidth(),
            )
            val brandSuggestions = if (state.brand.isBlank()) {
                emptyList()
            } else {
                allBrands.filter { it.contains(state.brand, ignoreCase = true) && !it.equals(state.brand, ignoreCase = true) }
            }
            if (brandSuggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    brandSuggestions.forEach { brand ->
                        AssistChip(onClick = { viewModel.onBrandChange(brand) }, label = { Text(brand) })
                    }
                }
            }
            OutlinedTextField(
                value = state.kcalPer100,
                onValueChange = viewModel::onKcalChange,
                label = { Text("kcal pro 100 g", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fatPer100,
                onValueChange = viewModel::onFatChange,
                label = { Text("Fett (g) pro 100 g", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.saturatedFatPer100,
                onValueChange = viewModel::onSaturatedFatChange,
                label = { Text("davon gesättigte Fettsäuren (g) pro 100 g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.carbsPer100,
                onValueChange = viewModel::onCarbsChange,
                label = { Text("Kohlenhydrate (g) pro 100 g", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sugarPer100,
                onValueChange = viewModel::onSugarChange,
                label = { Text("davon Zucker (g) pro 100 g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fiberPer100,
                onValueChange = viewModel::onFiberChange,
                label = { Text("Ballaststoffe (g) pro 100 g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.proteinPer100,
                onValueChange = viewModel::onProteinChange,
                label = { Text("Protein (g) pro 100 g", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.saltPer100,
                onValueChange = viewModel::onSaltChange,
                label = { Text("Salz (g) pro 100 g") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Einheiten (z.B. \"Scheibe\")", style = MaterialTheme.typography.titleSmall)
            Text(
                "Beim Eintragen ins Tagebuch oder in ein Rezept kann statt Gramm eine dieser " +
                    "Einheiten gewählt werden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.units.forEachIndexed { index, unit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = unit.name,
                        onValueChange = { viewModel.onUnitNameChange(index, it) },
                        label = { Text("Bezeichnung", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                    )
                    OutlinedTextField(
                        value = unit.amount,
                        onValueChange = { viewModel.onUnitAmountChange(index, it) },
                        label = { Text("Menge in g", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.removeUnitRow(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Einheit entfernen")
                    }
                }
            }
            TextButton(onClick = viewModel::addUnitRow) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Einheit hinzufügen")
            }
            Text("Flüssigkeit", style = MaterialTheme.typography.titleSmall)
            Text(
                "Besteht dieses Lebensmittel aus einer Flüssigkeit, wird sie beim Eintragen ins " +
                    "Tagebuch automatisch mitgezählt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FluidTypePicker(
                types = fluidTypes,
                selectedId = state.fluidTypeId,
                onSelect = viewModel::onFluidTypeChange,
            )
            if (state.fluidTypeId != null) {
                OutlinedTextField(
                    value = state.fluidMlPer100,
                    onValueChange = viewModel::onFluidMlPer100Change,
                    label = { Text("davon Flüssigkeit (ml) pro 100 g") },
                    supportingText = { Text("100 = besteht ganz aus dieser Flüssigkeit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("Tags", style = MaterialTheme.typography.titleSmall)
            if (state.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.tags.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text(tag.name) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Entfernen", modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.tagInput,
                    onValueChange = viewModel::onTagInputChange,
                    label = { Text("Tag hinzufügen (z.B. vegan)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.addTagFromInput() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::addTagFromInput) {
                    Icon(Icons.Filled.Add, contentDescription = "Tag hinzufügen")
                }
            }
            val suggestions = allTags.filter { candidate -> state.tags.none { it.name.equals(candidate.name, ignoreCase = true) } }
            if (suggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.forEach { tag ->
                        AssistChip(onClick = { viewModel.addTag(tag) }, label = { Text(tag.name) })
                    }
                }
            }
        }
    }
}

/** "Keine" plus one chip per Getränkeart; tapping the selected chip again clears the link. */
@Composable
private fun FluidTypePicker(types: List<FluidType>, selectedId: String?, onSelect: (String?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text("Keine") },
        )
        types.forEach { type ->
            FilterChip(
                selected = selectedId == type.id,
                onClick = { onSelect(if (selectedId == type.id) null else type.id) },
                label = { Text(type.name) },
            )
        }
    }
}
