package com.example.prokject2_tracker.fitness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.fitness.cardio.CardioEntryForm
import com.example.prokject2_tracker.fitness.strength.StrengthEntryForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingEntryScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val activityTypes by viewModel.activityTypes.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    val isValid = when (state.selectedType) {
        TrainingType.CARDIO -> state.cardioState.isValid
        TrainingType.STRENGTH -> state.strengthState.isValid
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (state.showTypeChooser) "Training hinzufügen" else "Training bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = dismissingKeyboard(viewModel::save), enabled = isValid) { Text("Speichern") }
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
            if (state.showTypeChooser) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.selectedType == TrainingType.CARDIO,
                        onClick = { viewModel.onTypeSelected(TrainingType.CARDIO) },
                        label = { Text("Cardio") },
                    )
                    FilterChip(
                        selected = state.selectedType == TrainingType.STRENGTH,
                        onClick = { viewModel.onTypeSelected(TrainingType.STRENGTH) },
                        label = { Text("Kraft") },
                    )
                }
            }

            when (state.selectedType) {
                TrainingType.CARDIO -> CardioEntryForm(
                    state = state.cardioState,
                    activityTypes = activityTypes,
                    onEpochDayChange = viewModel::onCardioEpochDayChange,
                    onActivityTypeChange = viewModel::onCardioActivityTypeChange,
                    onDurationChange = viewModel::onCardioDurationChange,
                    onDistanceChange = viewModel::onCardioDistanceChange,
                    onCaloriesChange = viewModel::onCardioCaloriesChange,
                    onHeartRateChange = viewModel::onCardioHeartRateChange,
                    onNoteChange = viewModel::onCardioNoteChange,
                )
                TrainingType.STRENGTH -> StrengthEntryForm(
                    state = state.strengthState,
                    exercises = exercises,
                    onEpochDayChange = viewModel::onStrengthEpochDayChange,
                    onExerciseChange = viewModel::onStrengthExerciseChange,
                    onSetRepsChange = viewModel::onStrengthSetRepsChange,
                    onSetWeightChange = viewModel::onStrengthSetWeightChange,
                    onAddSet = viewModel::onAddStrengthSet,
                    onRemoveSet = viewModel::onRemoveStrengthSet,
                    onNoteChange = viewModel::onStrengthNoteChange,
                )
            }
        }
    }
}
