package com.example.prokject2_tracker.task

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.statusColor
import com.example.prokject2_tracker.ui.theme.topAppBarColors

/**
 * The task list, split into what is owed now and what is merely coming. Ticking something off is
 * the whole interaction; the rhythm behind it is set once in the dialog and then stays out of the
 * way, showing only as the line under each name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.TASK.topAppBarColors(),
                title = { Text("Aufgaben") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Aufgabe hinzufügen")
            }
        },
    ) { padding ->
        if (uiState.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Noch keine Aufgaben angelegt. Über das Plus lassen sich einmalige und " +
                        "wiederkehrende Aufgaben anlegen — fällige erscheinen auch in den Tageszielen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.due.isNotEmpty()) {
                item(key = "due-title") {
                    Text("Fällig", style = MaterialTheme.typography.titleSmall)
                }
                items(uiState.due, key = { it.task.id }) { status ->
                    TaskRow(
                        status = status,
                        onToggle = { viewModel.toggleCompleted(status) },
                        onEdit = { editTarget = status.task },
                        onDelete = { viewModel.deleteTask(status.task) },
                    )
                }
            }
            if (uiState.upcoming.isNotEmpty()) {
                item(key = "upcoming-title") {
                    Text(
                        "Später",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(uiState.upcoming, key = { it.task.id }) { status ->
                    TaskRow(
                        status = status,
                        onToggle = { viewModel.toggleCompleted(status) },
                        onEdit = { editTarget = status.task },
                        onDelete = { viewModel.deleteTask(status.task) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        TaskEditDialog(
            existing = null,
            today = uiState.today,
            onConfirm = { name, recurrence, start, interval, weekdayMask, dayOfMonth ->
                viewModel.addTask(name, recurrence, start, interval, weekdayMask, dayOfMonth)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editTarget?.let { task ->
        TaskEditDialog(
            existing = task,
            today = uiState.today,
            onConfirm = { name, recurrence, start, interval, weekdayMask, dayOfMonth ->
                viewModel.updateTask(
                    task.copy(
                        name = name,
                        recurrence = recurrence,
                        startEpochDay = start,
                        intervalCount = interval,
                        weekdayMask = weekdayMask,
                        dayOfMonth = dayOfMonth,
                    ),
                )
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }
}

@Composable
private fun TaskRow(
    status: TaskStatus,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // Only an open occurrence or one settled today can be ticked; a task waiting for its next turn
    // has nothing to check off yet, so its box is inert rather than misleading.
    val checkable = status.isOpen || status.completedToday
    val done = !status.isOpen && status.completedToday

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = done, onCheckedChange = { onToggle() }, enabled = checkable)
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(
                    status.task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                Text(
                    buildString {
                        append(status.task.recurrenceLabel())
                        val state = when {
                            done -> "erledigt"
                            status.openDueDay != null -> dueLabel(status.openDueDay, status.today)
                            status.nextDueDay != null -> dueLabel(status.nextDueDay, status.today)
                            else -> null
                        }
                        state?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    // Overdue is the one thing on this row worth a colour: it is the difference
                    // between a list to work through and a list that is quietly rotting.
                    color = if ((status.overdueDays ?: 0) > 0) {
                        statusColor(isMet = false)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Aufgabe löschen")
            }
        }
    }
}
