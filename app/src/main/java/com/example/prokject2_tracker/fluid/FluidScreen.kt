package com.example.prokject2_tracker.fluid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PRESET_AMOUNTS_ML = listOf(100.0, 150.0, 200.0, 250.0, 300.0, 330.0, 400.0, 500.0, 750.0, 1000.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidScreen(
    onOpenDrawer: () -> Unit,
    onOpenTypeManagement: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FluidViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val types by viewModel.types.collectAsState()
    var expandedType by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::goToPreviousDay) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorheriger Tag")
                        }
                        Text(
                            DateUtils.localDateOfEpochDay(uiState.epochDay).format(dateFormatter),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = viewModel::goToNextDay) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Nächster Tag")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Mehr")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Getränkearten verwalten") },
                            onClick = {
                                showMenu = false
                                onOpenTypeManagement()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${uiState.totalMl.formatCompact()} / ${uiState.goalMl.formatCompact()} ml",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val progress = if (uiState.goalMl > 0) {
                        (uiState.totalMl / uiState.goalMl).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }

            if (types.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    types.forEach { type ->
                        AssistChip(
                            onClick = { viewModel.quickAdd(type, type.defaultQuickAddMl) },
                            label = { Text("${type.name} +${type.defaultQuickAddMl.formatCompact()}") },
                        )
                    }
                }
            }

            AddFluidRow(types = types, onAdd = viewModel::quickAdd)

            if (uiState.entries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Noch nichts für diesen Tag getrunken.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.entries.groupedSummaryLines(), key = { it.first }) { (typeId, line) ->
                        Text(
                            line,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedType = typeId }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }

    expandedType?.let { typeId ->
        val entriesForType = uiState.entries.filter { it.fluidTypeId == typeId }
        AlertDialog(
            onDismissRequest = { expandedType = null },
            title = { Text(entriesForType.firstOrNull()?.fluidTypeName.orEmpty()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entriesForType.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${entry.amountMl.formatCompact()} ml", modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.delete(entry) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Löschen")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expandedType = null }) { Text("Schließen") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFluidRow(types: List<FluidType>, onAdd: (FluidType, Double) -> Unit) {
    var selectedType by remember { mutableStateOf<FluidType?>(null) }
    var selectedAmount by remember { mutableStateOf<Double?>(null) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var amountMenuExpanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ExposedDropdownMenuBox(
            expanded = typeMenuExpanded,
            onExpandedChange = { typeMenuExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                readOnly = true,
                value = selectedType?.name.orEmpty(),
                onValueChange = {},
                label = { Text("Flüssigkeit") },
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
                            selectedType = type
                            typeMenuExpanded = false
                        },
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = amountMenuExpanded,
            onExpandedChange = { amountMenuExpanded = it },
            modifier = Modifier.width(120.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                readOnly = true,
                value = selectedAmount?.let { "${it.formatCompact()} ml" }.orEmpty(),
                onValueChange = {},
                label = { Text("Menge") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = amountMenuExpanded) },
            )
            ExposedDropdownMenu(
                expanded = amountMenuExpanded,
                onDismissRequest = { amountMenuExpanded = false },
            ) {
                PRESET_AMOUNTS_ML.forEach { amount ->
                    DropdownMenuItem(
                        text = { Text("${amount.formatCompact()} ml") },
                        onClick = {
                            selectedAmount = amount
                            amountMenuExpanded = false
                        },
                    )
                }
            }
        }

        IconButton(
            onClick = {
                val type = selectedType
                val amount = selectedAmount
                if (type != null && amount != null) {
                    onAdd(type, amount)
                    selectedType = null
                    selectedAmount = null
                }
            },
            enabled = selectedType != null && selectedAmount != null,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Getränk hinzufügen")
        }
    }
}

/** "-1,5l Wasser (400ml+400ml+700ml)" per fluid type, in the types' library sort order. */
private fun List<FluidEntry>.groupedSummaryLines(): List<Pair<String, String>> =
    groupBy { it.fluidTypeId }
        .toList()
        .sortedBy { (_, entries) -> entries.minOf { it.createdAt } }
        .map { (typeId, entries) ->
            val totalLiters = entries.sumOf { it.amountMl } / 1000.0
            val liters = String.format(Locale.GERMAN, "%.1f", totalLiters)
            val breakdown = entries
                .sortedBy { it.createdAt }
                .joinToString("+") { "${it.amountMl.formatCompact()}ml" }
            typeId to "-${liters}l ${entries.first().fluidTypeName} ($breakdown)"
        }
