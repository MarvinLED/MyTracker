package com.example.prokject2_tracker.fluid

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
import com.example.prokject2_tracker.core.util.formatCompact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidTypeManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FluidTypeManageViewModel = hiltViewModel(),
) {
    val types by viewModel.types.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<FluidType?>(null) }
    var blockedDelete by remember { mutableStateOf<FluidType?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Getränkearten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Getränkeart hinzufügen")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (types.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Getränkearten angelegt.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(types, key = { it.id }) { type ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${type.name} (+${type.defaultQuickAddMl.formatCompact()} ml)",
                                    modifier = Modifier.weight(1f).clickable { editTarget = type },
                                )
                                IconButton(onClick = {
                                    viewModel.deleteIfUnused(type) { blockedDelete = type }
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
        FluidTypeDialog(
            title = "Getränkeart hinzufügen",
            initialName = "",
            initialAmountMl = "",
            onConfirm = { name, amountMl ->
                viewModel.create(name, amountMl)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { type ->
        FluidTypeDialog(
            title = "Getränkeart bearbeiten",
            initialName = type.name,
            initialAmountMl = type.defaultQuickAddMl.formatCompact(),
            onConfirm = { name, amountMl ->
                viewModel.update(type, name, amountMl)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    blockedDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            confirmButton = { TextButton(onClick = { blockedDelete = null }) { Text("OK") } },
            title = { Text("Kann nicht gelöscht werden") },
            text = { Text("\"${type.name}\" wird noch bei geloggten Getränken verwendet.") },
        )
    }
}

@Composable
private fun FluidTypeDialog(
    title: String,
    initialName: String,
    initialAmountMl: String,
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmountMl) }
    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Standardmenge (ml)") },
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
