package com.example.prokject2_tracker.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun TagManageContent(
    modifier: Modifier = Modifier,
    viewModel: TagManageViewModel = hiltViewModel(),
) {
    val tags by viewModel.allTags.collectAsState()
    var editingTagId by remember { mutableStateOf<String?>(null) }
    var editingTagName by remember { mutableStateOf("") }
    var showHierarchyDialog by remember { mutableStateOf(false) }
    var selectedParentTagId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        if (tags.isEmpty()) {
            Text(
                "Noch keine Tags vorhanden",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(tags, key = { it.id }) { tag ->
                    TagManageItem(
                        tag = tag,
                        onEditName = {
                            editingTagId = tag.id
                            editingTagName = tag.name
                        },
                        onAddHierarchy = {
                            selectedParentTagId = tag.id
                            showHierarchyDialog = true
                        },
                    )
                }
            }
        }

        if (editingTagId != null) {
            EditTagNameDialog(
                currentName = editingTagName,
                onSave = { newName ->
                    viewModel.updateTagName(editingTagId!!, newName)
                    editingTagId = null
                },
                onDismiss = { editingTagId = null },
            )
        }

        if (showHierarchyDialog && selectedParentTagId != null) {
            AddHierarchyDialog(
                parentTagId = selectedParentTagId!!,
                allTags = tags,
                onAdd = { childTagId ->
                    viewModel.addTagHierarchy(selectedParentTagId!!, childTagId)
                    showHierarchyDialog = false
                },
                onDismiss = { showHierarchyDialog = false },
            )
        }
    }
}

@Composable
private fun TagManageItem(
    tag: Tag,
    onEditName: () -> Unit,
    onAddHierarchy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    IconButton(onClick = onEditName, modifier = Modifier.then(Modifier)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Tag-Name bearbeiten")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onAddHierarchy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.then(Modifier).then(Modifier).padding(4.dp))
                    Text("Als Kind hinzufügen")
                }
            }
        }
    }
}

@Composable
private fun EditTagNameDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf(currentName) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag-Namen ändern") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tag-Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (newName.isNotBlank()) onSave(newName) }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}

@Composable
private fun AddHierarchyDialog(
    parentTagId: String,
    allTags: List<Tag>,
    onAdd: (childTagId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    val selectedChild = selectedChildId?.let { id -> allTags.find { it.id == id } }
    val childOptions = allTags.filter { it.id != parentTagId }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kind-Tag auswählen") },
        text = {
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(selectedChild?.name ?: "Tag auswählen")
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    childOptions.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag.name) },
                            onClick = {
                                selectedChildId = tag.id
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selectedChildId != null) onAdd(selectedChildId!!) },
                enabled = selectedChildId != null,
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}
