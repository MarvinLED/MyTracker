package com.example.mytracker.fluid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.mytracker.core.ui.ConfirmDeleteDialog
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidUnitManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FluidUnitManageViewModel = hiltViewModel(),
) {
    val units by viewModel.units.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<FluidUnit?>(null) }
    var blockedDelete by remember { mutableStateOf<FluidUnit?>(null) }
    var pendingDelete by remember { mutableStateOf<FluidUnit?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Maßeinheiten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Maßeinheit hinzufügen")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (units.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Maßeinheiten angelegt.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(units, key = { it.id }) { unit ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${unit.name} (${unit.amountMl.formatDecimal(3)} ml)",
                                    modifier = Modifier.weight(1f).clickable { editTarget = unit },
                                )
                                IconButton(onClick = {
                                    pendingDelete = unit
                                }) {
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
        FluidUnitDialog(
            title = "Maßeinheit hinzufügen",
            initialName = "",
            initialAmountMl = "",
            onConfirm = { name, amountMl ->
                viewModel.create(name, amountMl)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { unit ->
        FluidUnitDialog(
            title = "Maßeinheit bearbeiten",
            initialName = unit.name,
            initialAmountMl = unit.amountMl.formatDecimal(3),
            onConfirm = { name, amountMl ->
                viewModel.update(unit, name, amountMl)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    pendingDelete?.let { unit ->
        ConfirmDeleteDialog(
            title = "\"${unit.name}\" löschen?",
            text = "Die Einheit steht danach beim Eintragen nicht mehr zur Auswahl.",
            // The in-use guard still runs after the answer: confirming says the user meant
            // it, not that it is safe.
            onConfirm = { viewModel.deleteIfUnused(unit) { blockedDelete = unit } },
            onDismiss = { pendingDelete = null },
        )
    }

    blockedDelete?.let { unit ->
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            confirmButton = { TextButton(onClick = { blockedDelete = null }) { Text("OK") } },
            title = { Text("Kann nicht gelöscht werden") },
            text = { Text("\"${unit.name}\" wird noch bei geloggten Getränken verwendet.") },
        )
    }
}

@Composable
private fun FluidUnitDialog(
    title: String,
    initialName: String,
    initialAmountMl: String,
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmountMl) }
    val amount = amountText.toLocaleDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (z.B. Glas)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Menge (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onConfirm(name, it) } },
                enabled = name.isNotBlank() && amount != null && amount > 0.0,
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
