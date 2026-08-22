package com.example.mytracker.nutrition.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.ColorSwatchPicker

/**
 * The Tags library, third tab of the Bibliothek. Tags are still created as a by-product of typing a
 * name in the Lebensmittel editor — this is where one gets renamed, recoloured, related to another
 * tag, or removed.
 */
@Composable
fun TagListContent(
    modifier: Modifier = Modifier,
    viewModel: TagManageViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsState()
    val implications by viewModel.implications.collectAsState()
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Tag?>(null) }
    var deleting by remember { mutableStateOf<Pair<Tag, Int>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
    }

    val tagOrder = remember(tags) { tags.map { it.id } }
    val tagsById = remember(tags) { tags.associateBy { it.id } }

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
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
                "Noch keine Tags. Lege welche an — z.B. \"vegan\", \"Obst\", \"Meal Prep\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(tags, key = { _, tag -> tag.id }) { index, tag ->
                val impliedNames = implications
                    .filter { it.childTagId == tag.id }
                    .mapNotNull { tagsById[it.parentTagId]?.name }
                    .sorted()

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TagDot(color = tag.displayColor(index), size = 14)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(tag.name)
                            // Says what the filter will do with this tag, in the same words the
                            // dialog uses to set it.
                            if (impliedNames.isNotEmpty()) {
                                Text(
                                    "ist auch: ${impliedNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { editing = tag }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
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

        SnackbarHost(snackbarHostState)
    }

    editing?.let { tag ->
        TagEditDialog(
            tag = tag,
            allTags = tags,
            implications = implications,
            tagOrder = tagOrder,
            onConfirm = { name, colorArgb, impliedTagIds ->
                viewModel.save(tag, name, colorArgb, impliedTagIds)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    deleting?.let { (tag, count) ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("\"${tag.name}\" löschen?") },
            // The Lebensmittel themselves stay; only the label comes off them. Saying which is which
            // is what makes this answerable.
            text = {
                Text(
                    "Der Tag ist an $count ${if (count == 1) "Lebensmittel" else "Lebensmitteln"} vergeben. " +
                        "Die Lebensmittel bleiben erhalten, verlieren aber diesen Tag.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(tag)
                    deleting = null
                }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Abbrechen") } },
        )
    }
}

/**
 * Name, colour and dependencies of one tag in a single dialog, since all three are what "bearbeiten"
 * means here.
 */
@Composable
private fun TagEditDialog(
    tag: Tag,
    allTags: List<Tag>,
    implications: List<TagImplication>,
    tagOrder: List<String>,
    onConfirm: (String, Int?, Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(tag.name) }
    var colorArgb by remember { mutableStateOf(tag.colorArgb) }
    var implied by remember {
        mutableStateOf(implications.filter { it.childTagId == tag.id }.map { it.parentTagId }.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag bearbeiten") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Farbe", style = MaterialTheme.typography.titleSmall)
                ColorSwatchPicker(selectedArgb = colorArgb, onSelect = { colorArgb = it })

                if (allTags.size > 1) {
                    Text("Ist auch", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Ein Filter auf den gewählten Tag findet dann auch \"${tag.name}\" — " +
                            "z.B. ist alles Vegane auch vegetarisch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allTags.filter { it.id != tag.id }.forEach { other ->
                            val selected = other.id in implied
                            // A tag that already depends on this one is offered greyed out rather
                            // than accepted and then rejected: the loop is visible before the tap.
                            val cyclic = wouldCreateCycle(tag.id, other.id, implications)
                            FilterChip(
                                selected = selected,
                                enabled = selected || !cyclic,
                                onClick = {
                                    implied = if (selected) implied - other.id else implied + other.id
                                },
                                label = { Text(other.name) },
                                leadingIcon = {
                                    TagDot(
                                        color = other.displayColor(tagOrder.indexOf(other.id).coerceAtLeast(0)),
                                        size = 12,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, colorArgb, implied) },
                enabled = name.isNotBlank(),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
