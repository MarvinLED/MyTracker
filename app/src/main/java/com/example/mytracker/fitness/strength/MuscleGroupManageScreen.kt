package com.example.mytracker.fitness.strength

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
fun MuscleGroupManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MuscleGroupManageViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<MuscleGroup?>(null) }
    var blockedDelete by remember { mutableStateOf<MuscleGroup?>(null) }
    var pendingDelete by remember { mutableStateOf<MuscleGroup?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Muskelgruppen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Muskelgruppe hinzufügen")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Muskelgruppen angelegt.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups, key = { it.id }) { group ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    group.name,
                                    modifier = Modifier.weight(1f).clickable { editTarget = group },
                                )
                                IconButton(onClick = {
                                    pendingDelete = group
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
        MuscleGroupDialog(
            title = "Muskelgruppe hinzufügen",
            initialName = "",
            onConfirm = { name ->
                viewModel.create(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { group ->
        MuscleGroupDialog(
            title = "Muskelgruppe bearbeiten",
            initialName = group.name,
            onConfirm = { name ->
                viewModel.update(group, name)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    pendingDelete?.let { group ->
        ConfirmDeleteDialog(
            title = "\"${group.name}\" löschen?",
            text = "Die Muskelgruppe wird entfernt und fällt bei jeder Übung weg, der sie zugeordnet war.",
            // The in-use guard still runs after the answer: confirming says the user meant
            // it, not that it is safe.
            onConfirm = { viewModel.deleteIfUnused(group) { blockedDelete = group } },
            onDismiss = { pendingDelete = null },
        )
    }

    blockedDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { blockedDelete = null },
            confirmButton = { TextButton(onClick = { blockedDelete = null }) { Text("OK") } },
            title = { Text("Kann nicht gelöscht werden") },
            text = { Text("\"${group.name}\" wird noch bei mindestens einer Übung verwendet.") },
        )
    }
}

@Composable
private fun MuscleGroupDialog(
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
