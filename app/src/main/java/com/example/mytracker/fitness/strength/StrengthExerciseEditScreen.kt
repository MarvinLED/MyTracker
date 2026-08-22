package com.example.prokject2_tracker.fitness.strength

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthExerciseEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StrengthExerciseEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val muscleGroups by viewModel.muscleGroups.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) "Übung hinzufügen" else "Übung bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = dismissingKeyboard(viewModel::save), enabled = state.isValid) { Text("Speichern") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Muskelgruppen", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                muscleGroups.forEach { group ->
                    FilterChip(
                        selected = group.id in state.muscleGroupIds,
                        onClick = { viewModel.onMuscleGroupToggle(group) },
                        label = { Text(group.name) },
                    )
                }
            }
            Text("Bewegungsrichtung (optional)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementDirection.entries.forEach { direction ->
                    FilterChip(
                        selected = state.movementDirection == direction,
                        onClick = { viewModel.onMovementDirectionToggle(direction) },
                        label = { Text(direction.label()) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Körpergewichtsübung", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Klimmzüge, Liegestütze … Das Eintragen startet dann beim Körpergewicht, " +
                            "Zusatzgewicht lässt sich weiterhin dazurechnen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.isBodyweight,
                    onCheckedChange = viewModel::onBodyweightToggle,
                )
            }
        }
    }
}
