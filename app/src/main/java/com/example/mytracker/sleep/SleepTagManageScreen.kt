package com.example.mytracker.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

/**
 * The Schlaf-Tags library. Tags are created straight from the Schlaf screen — this is where a typo
 * gets corrected and an unused one goes away, mirroring the Körperstellen screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTagManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepTagManageViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsState()
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<SleepTag?>(null) }
    var deleting by remember { mutableStateOf<Pair<SleepTag, Int>?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.SLEEP.topAppBarColors(),
                title = { Text("Schlaf-Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Neuer Tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    viewModel.create(newName)
                    newName = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Tag anlegen")
                }
            }

            if (tags.isEmpty()) {
                Text(
                    "Noch keine Tags. Lege welche an — z.B. \"heiß\", \"viel geträumt\", \"durchgeschlafen\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it.id }) { tag ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tag.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editing = tag }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Umbenennen")
                            }
                            IconButton(onClick = {
                                viewModel.requestDelete(tag) { count -> deleting = tag to count }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { tag ->
        RenameDialog(
            initialName = tag.name,
            onConfirm = {
                viewModel.rename(tag, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    deleting?.let { (tag, count) ->
        ConfirmDeleteDialog(
            title = "\"${tag.name}\" löschen?",
            // The nights themselves stay; only the label comes off them. Saying which is which is
            // what makes this answerable.
            text = "Der Tag ist an $count ${if (count == 1) "Nacht" else "Nächten"} vergeben. " +
                "Die Nächte bleiben erhalten, verlieren aber diesen Tag.",
            onConfirm = { viewModel.delete(tag) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RenameDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag umbenennen") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
