package com.example.mytracker.fitness.cardio

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.ConfirmDeleteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioActivityTypeManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardioActivityTypeManageViewModel = hiltViewModel(),
) {
    val types by viewModel.types.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CardioActivityType?>(null) }
    var blockedDelete by remember { mutableStateOf<CardioActivityType?>(null) }
    var pendingDelete by remember { mutableStateOf<CardioActivityType?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cardio-Aktivitäten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Cardio-Aktivität hinzufügen")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (types.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Cardio-Aktivitäten angelegt.")
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
                                    type.name,
                                    modifier = Modifier.weight(1f).clickable { editTarget = type },
                                )
                                IconButton(onClick = {
                                    pendingDelete = type
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
        CardioActivityTypeDialog(
            title = "Cardio-Aktivität hinzufügen",
            initialName = "",
            onConfirm = { name ->
                viewModel.create(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { type ->
        CardioActivityTypeDialog(
            title = "Cardio-Aktivität bearbeiten",
            initialName = type.name,
            onConfirm = { name ->
                viewModel.update(type, name)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    pendingDelete?.let { type ->
        ConfirmDeleteDialog(
            title = "\"${type.name}\" löschen?",
            text = "Die Aktivitätsart wird entfernt.",
            // The in-use guard still runs after the answer: confirming says the user meant
            // it, not that it is safe.
            onConfirm = { viewModel.deleteIfUnused(type) { blockedDelete = type } },
            onDismiss = { pendingDelete = null },
        )
    }

    blockedDelete?.let { type ->
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            confirmButton = { TextButton(onClick = { blockedDelete = null }) { Text("OK") } },
            title = { Text("Kann nicht gelöscht werden") },
            text = { Text("\"${type.name}\" wird noch bei geloggten Cardio-Einheiten verwendet.") },
        )
    }
}

@Composable
private fun CardioActivityTypeDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
