package com.example.prokject2_tracker.fitness.strength

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun StrengthExerciseListContent(
    onAddExercise: () -> Unit,
    onEditExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StrengthExerciseListViewModel = hiltViewModel(),
) {
    val exercises by viewModel.exercises.collectAsState()
    var blockedDeleteExercise by remember { mutableStateOf<StrengthExercise?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Noch keine Übungen angelegt.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(exercises, key = { it.exercise.id }) { item ->
                    val exercise = item.exercise
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEditExercise(exercise.id) },
                            ) {
                                Text(exercise.name)
                                Text(item.muscleGroups.joinToString(", ") { it.name })
                            }
                            IconButton(onClick = {
                                viewModel.deleteIfUnused(exercise) { blockedDeleteExercise = exercise }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddExercise,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Übung hinzufügen")
        }
    }

    blockedDeleteExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = { blockedDeleteExercise = null },
            confirmButton = { TextButton(onClick = { blockedDeleteExercise = null }) { Text("OK") } },
            title = { Text("Kann nicht gelöscht werden") },
            text = { Text("\"${exercise.name}\" wird in mindestens einem Trainingseintrag verwendet.") },
        )
    }
}
