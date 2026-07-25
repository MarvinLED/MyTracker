package com.example.prokject2_tracker.fluid

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidScreen(
    onOpenDrawer: () -> Unit,
    onOpenTypeManagement: () -> Unit,
    onOpenUnitManagement: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FluidViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val types by viewModel.types.collectAsState()
    val units by viewModel.units.collectAsState()
    var editingEntryId by remember { mutableStateOf<String?>(null) }
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
                        DropdownMenuItem(
                            text = { Text("Maßeinheiten verwalten") },
                            onClick = {
                                showMenu = false
                                onOpenUnitManagement()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        val distributionSlices = uiState.entries.distributionSlices(types)
        val goalSlices = goalSlices(consumedMl = uiState.totalMl, goalMl = uiState.goalMl)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "charts") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${uiState.totalMl.formatDecimal(3)} / ${uiState.goalMl.formatDecimal(3)} ml",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FluidChartBlock(
                                title = "Verteilung",
                                slices = distributionSlices,
                                centerValue = "${(uiState.totalMl / 1000.0).formatDecimal(3)} l",
                                centerLabel = "gesamt",
                                valueLabel = { "${it.value.formatDecimal(3)} ml" },
                                emptyText = "Noch nichts getrunken.",
                                modifier = Modifier.weight(1f),
                            )
                            FluidChartBlock(
                                title = "Tagesziel",
                                slices = goalSlices,
                                centerValue = goalPercentLabel(uiState.totalMl, uiState.goalMl),
                                centerLabel = "vom Ziel",
                                valueLabel = { "${it.value.formatDecimal(3)} ml" },
                                emptyText = "Kein Ziel gesetzt.",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (types.isNotEmpty()) {
                item(key = "quick-add-chips") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        types.forEach { type ->
                            AssistChip(
                                onClick = { viewModel.quickAdd(type, type.defaultQuickAddMl) },
                                label = { Text("${type.name} +${type.defaultQuickAddMl.formatDecimal(3)}") },
                            )
                        }
                    }
                }
            }

            item(key = "add-row") {
                AddFluidRow(types = types, units = units, onAdd = viewModel::addWithUnit)
            }

            if (uiState.entries.isEmpty()) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Noch nichts für diesen Tag getrunken.")
                    }
                }
            } else {
                item(key = "entries-hint") {
                    Text(
                        "Einträge — rechts die Menge bearbeiten oder den Eintrag löschen.",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(uiState.entries.groupedByType(), key = { it.typeId }) { group ->
                    FluidTypeGroupCard(
                        group = group,
                        editingEntryId = editingEntryId,
                        onStartEdit = { editingEntryId = it.id },
                        onCancelEdit = { editingEntryId = null },
                        onSaveEdit = { entry, amountMl ->
                            viewModel.updateAmount(entry, amountMl)
                            editingEntryId = null
                        },
                        onDelete = { entry ->
                            if (editingEntryId == entry.id) editingEntryId = null
                            viewModel.delete(entry)
                        },
                    )
                }
            }
        }
    }
}

/** One drink type's entries for the day: total in the header, one editable row per logged amount. */
@Composable
private fun FluidTypeGroupCard(
    group: FluidTypeGroup,
    editingEntryId: String?,
    onStartEdit: (FluidEntry) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (FluidEntry, Double) -> Unit,
    onDelete: (FluidEntry) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "${group.totalMl.formatDecimal(3)} ml",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            group.entries.forEach { entry ->
                FluidEntryRow(
                    entry = entry,
                    isEditing = editingEntryId == entry.id,
                    onStartEdit = { onStartEdit(entry) },
                    onCancelEdit = onCancelEdit,
                    onSaveEdit = { amountMl -> onSaveEdit(entry, amountMl) },
                    onDelete = { onDelete(entry) },
                )
            }
        }
    }
}

/**
 * Editing happens in place — the amount text swaps for an input field — so changing a mistyped
 * amount never costs the user the context of the rest of the day.
 *
 * Entries mirrored from a Tagebuch entry are read-only here: their amount follows that entry's
 * Lebensmittel, so the only honest place to change or remove them is the Tagebuch itself.
 */
@Composable
private fun FluidEntryRow(
    entry: FluidEntry,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    if (entry.sourceDiaryEntryId != null) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${entry.amountMl.formatDecimal(3)} ml", modifier = Modifier.weight(1f))
            Text(
                "aus dem Tagebuch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    if (isEditing) {
        var amountText by remember(entry.id) { mutableStateOf(entry.amountMl.formatDecimal(3)) }
        val amount = amountText.toLocaleDoubleOrNull()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Menge (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { amount?.let(onSaveEdit) },
                enabled = amount != null && amount > 0.0,
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Menge speichern")
            }
            IconButton(onClick = onCancelEdit) {
                Icon(Icons.Filled.Close, contentDescription = "Bearbeiten abbrechen")
            }
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${entry.amountMl.formatDecimal(3)} ml", modifier = Modifier.weight(1f))
        // Small glyphs, but the IconButtons keep their full 48dp touch target around them.
        IconButton(onClick = onStartEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Eintrag bearbeiten",
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eintrag löschen",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFluidRow(types: List<FluidType>, units: List<FluidUnit>, onAdd: (FluidType, FluidUnit) -> Unit) {
    var selectedType by remember { mutableStateOf<FluidType?>(null) }
    var selectedUnit by remember { mutableStateOf<FluidUnit?>(null) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ExposedDropdownMenuBox(
            expanded = typeMenuExpanded,
            onExpandedChange = { typeMenuExpanded = it },
            modifier = Modifier.weight(1.2f),
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                readOnly = true,
                value = selectedType?.name.orEmpty(),
                onValueChange = {},
                label = { Text("Flüssigkeit", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            expanded = unitMenuExpanded,
            onExpandedChange = { unitMenuExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                readOnly = true,
                value = selectedUnit?.name.orEmpty(),
                onValueChange = {},
                label = { Text("Menge", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
            )
            ExposedDropdownMenu(
                expanded = unitMenuExpanded,
                onDismissRequest = { unitMenuExpanded = false },
            ) {
                units.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit.name) },
                        onClick = {
                            selectedUnit = unit
                            unitMenuExpanded = false
                        },
                    )
                }
            }
        }

        IconButton(
            onClick = {
                val type = selectedType
                val unit = selectedUnit
                if (type != null && unit != null) {
                    onAdd(type, unit)
                    selectedType = null
                    selectedUnit = null
                }
            },
            enabled = selectedType != null && selectedUnit != null,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Getränk hinzufügen")
        }
    }
}

/**
 * One slice per drink type consumed today, ordered by the library's own order so a type keeps its
 * colour across days. Types past the palette's eight slots (or logged under a type that has since
 * been deleted from the library) fold into one "Sonstige" slice rather than repeating a hue.
 */
@Composable
private fun List<FluidEntry>.distributionSlices(types: List<FluidType>): List<FluidSlice> {
    val palette = fluidPalette()
    val otherColor = MaterialTheme.colorScheme.onSurfaceVariant
    val totalsByType = groupBy { it.fluidTypeId }.mapValues { (_, entries) -> entries.sumOf { it.amountMl } }

    val named = mutableListOf<Pair<Int, FluidSlice>>()
    var otherTotal = 0.0
    totalsByType.forEach { (typeId, total) ->
        val index = types.indexOfFirst { it.id == typeId }
        val type = types.getOrNull(index)
        if (type == null || index >= palette.size) {
            otherTotal += total
        } else {
            named += index to FluidSlice(label = type.name, value = total, color = type.chartColor(index))
        }
    }
    val ordered = named.sortedBy { (index, _) -> index }.map { (_, slice) -> slice }
    return if (otherTotal > 0.0) ordered + FluidSlice("Sonstige", otherTotal, otherColor) else ordered
}

/** Drunk vs. still open against the daily goal; once the goal is reached the ring is simply full. */
@Composable
private fun goalSlices(consumedMl: Double, goalMl: Double): List<FluidSlice> {
    val reachedColor = MaterialTheme.colorScheme.primary
    val openColor = MaterialTheme.colorScheme.outline
    if (goalMl <= 0.0) return emptyList()
    val open = goalMl - consumedMl
    return if (open <= 0.0) {
        listOf(FluidSlice("Getrunken", consumedMl, reachedColor))
    } else {
        listOf(
            FluidSlice("Getrunken", consumedMl, reachedColor),
            FluidSlice("Offen", open, openColor),
        )
    }
}

private fun goalPercentLabel(consumedMl: Double, goalMl: Double): String =
    if (goalMl <= 0.0) "–" else "${Math.round(consumedMl / goalMl * 100.0)} %"

private data class FluidTypeGroup(
    val typeId: String,
    val name: String,
    val totalMl: Double,
    val entries: List<FluidEntry>,
)

/** The day's entries per drink type, types in first-logged order and entries in logging order. */
private fun List<FluidEntry>.groupedByType(): List<FluidTypeGroup> =
    groupBy { it.fluidTypeId }
        .toList()
        .sortedBy { (_, entries) -> entries.minOf { it.createdAt } }
        .map { (typeId, entries) ->
            FluidTypeGroup(
                typeId = typeId,
                name = entries.first().fluidTypeName,
                totalMl = entries.sumOf { it.amountMl },
                entries = entries.sortedBy { it.createdAt },
            )
        }
