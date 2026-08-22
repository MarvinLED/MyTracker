package com.example.mytracker.fluid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull

/**
 * Where the Tagebuch's Schnellauswahl is put together: which drink is offered under which symbol,
 * and how much one tap logs. Deliberately a small, capped list — [FluidQuickAddLimit] buttons is the
 * two rows the Tagebuch draws, and a shortcut that has to be hunted for in a longer list is not one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidQuickAddManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FluidQuickAddManageViewModel = hiltViewModel(),
) {
    val quickAdds by viewModel.quickAdds.collectAsState()
    val types by viewModel.types.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<FluidQuickAdd?>(null) }
    val items = fluidQuickAddItems(quickAdds, types)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Schnellauswahl") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            // Hidden rather than disabled at the cap: the hint below the list already says why, and
            // a button that can never do anything is the worse of the two.
            if (quickAdds.size < FluidQuickAddLimit && types.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Schnellauswahl hinzufügen")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (types.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Erst eine Getränkeart anlegen — eine Schnellauswahl zeigt immer auf eine davon.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "hint") {
                        Text(
                            "Bis zu $FluidQuickAddLimit Buttons, im Tagebuch in zwei Reihen zu " +
                                "je $FluidQuickAddsPerRow. Die Farbe kommt von der Getränkeart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(items, key = { it.quickAdd.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // The button exactly as the Tagebuch draws it, so what is being
                                // configured is never in doubt.
                                FluidQuickAddButton(item = item, onClick = { editTarget = item.quickAdd })
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "${item.typeName} · ${item.quickAdd.symbol.label()} · " +
                                        "${item.quickAdd.amountMl.formatDecimal(3)} ml",
                                    modifier = Modifier.weight(1f).clickable { editTarget = item.quickAdd },
                                )
                                IconButton(onClick = { viewModel.delete(item.quickAdd) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        FluidQuickAddDialog(
            title = "Schnellauswahl hinzufügen",
            types = types,
            initialTypeId = types.first().id,
            initialSymbol = FluidQuickAddSymbol.GLASS,
            initialAmountMl = FluidQuickAddSymbol.GLASS.defaultAmountMl(),
            onConfirm = { typeId, symbol, amountMl ->
                viewModel.create(typeId, symbol, amountMl)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { quickAdd ->
        FluidQuickAddDialog(
            title = "Schnellauswahl bearbeiten",
            types = types,
            initialTypeId = quickAdd.fluidTypeId,
            initialSymbol = quickAdd.symbol,
            initialAmountMl = quickAdd.amountMl,
            onConfirm = { typeId, symbol, amountMl ->
                viewModel.update(quickAdd, typeId, symbol, amountMl)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }
}

/**
 * The amount field disappears for "100": that symbol names its own amount, and a 100-button that
 * logs 250 ml would be a button that lies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FluidQuickAddDialog(
    title: String,
    types: List<FluidType>,
    initialTypeId: String,
    initialSymbol: FluidQuickAddSymbol,
    initialAmountMl: Double,
    onConfirm: (String, FluidQuickAddSymbol, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var typeId by remember { mutableStateOf(initialTypeId) }
    var symbol by remember { mutableStateOf(initialSymbol) }
    var amountText by remember { mutableStateOf(initialAmountMl.formatDecimal(3)) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val selectedType = types.firstOrNull { it.id == typeId }
    val amount = amountText.toLocaleDoubleOrNull()
    val amountIsFixed = symbol == FluidQuickAddSymbol.ML_100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        readOnly = true,
                        value = selectedType?.name.orEmpty(),
                        onValueChange = {},
                        label = { Text("Getränkeart") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    typeId = type.id
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Text("Symbol", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FluidQuickAddSymbol.entries.forEach { option ->
                        FilterChip(
                            selected = symbol == option,
                            onClick = {
                                // The amount follows the symbol only while it is still the previous
                                // symbol's default — a typed 350 ml survives switching Glas/Flasche.
                                val wasDefault = amountText.toLocaleDoubleOrNull() == symbol.defaultAmountMl()
                                symbol = option
                                if (wasDefault || option == FluidQuickAddSymbol.ML_100) {
                                    amountText = option.defaultAmountMl().formatDecimal(3)
                                }
                            },
                            label = { Text(option.label()) },
                        )
                    }
                }

                if (amountIsFixed) {
                    Text(
                        "Dieser Button trägt immer ${FluidQuickAdd100Ml.formatDecimal(3)} ml ein.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Menge (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = if (amountIsFixed) FluidQuickAdd100Ml else amount
                    if (selectedType != null && value != null) onConfirm(selectedType.id, symbol, value)
                },
                enabled = selectedType != null && (amountIsFixed || (amount != null && amount > 0.0)),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
