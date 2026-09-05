package com.example.mytracker.measurement

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
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

/** A site queued for deletion together with how many measurements would go with it. */
private data class PendingDelete(val site: BodySite, val measurementCount: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodySiteManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodySiteManageViewModel = hiltViewModel(),
) {
    val sites by viewModel.sites.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<BodySite?>(null) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Körperstellen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Körperstelle hinzufügen")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (sites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Noch keine Körperstellen angelegt.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sites, key = { it.id }) { site ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).clickable { editTarget = site },
                                ) {
                                    Text(site.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        site.measuringHint ?: "Kein Messhinweis",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.requestDelete(site) { count ->
                                        pendingDelete = PendingDelete(site, count)
                                    }
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
        BodySiteDialog(
            title = "Körperstelle hinzufügen",
            initialName = "",
            initialHint = "",
            onConfirm = { name, hint ->
                viewModel.create(name, hint)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { site ->
        BodySiteDialog(
            title = "Körperstelle bearbeiten",
            initialName = site.name,
            initialHint = site.measuringHint.orEmpty(),
            onConfirm = { name, hint ->
                viewModel.update(site, name, hint)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    pendingDelete?.let { pending ->
        ConfirmDeleteDialog(
            title = "\"${pending.site.name}\" löschen?",
            text = if (pending.measurementCount == 1) {
                "Dazu gehört 1 gespeicherte Messung, die mitgelöscht wird."
            } else {
                "Dazu gehören ${pending.measurementCount} gespeicherte Messungen, die mitgelöscht werden."
            },
            onConfirm = { viewModel.delete(pending.site) },
            onDismiss = { pendingDelete = null },
        )
    }
}

/**
 * Name plus the measuring hint that the Maße screen shows when the name is tapped. The hint is
 * optional but prompted for here, at the one moment the user is thinking about how they measure
 * this spot.
 */
@Composable
private fun BodySiteDialog(
    title: String,
    initialName: String,
    initialHint: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var hint by remember { mutableStateOf(initialHint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("z.B. Oberarm links") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Messhinweis (optional)") },
                    placeholder = { Text("z.B. angespannt, an der dicksten Stelle") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, hint) },
                enabled = name.isNotBlank(),
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
