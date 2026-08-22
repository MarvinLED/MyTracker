package com.example.mytracker.fitness.cardio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * The measured fields of one cardio session. Activity type and date are *not* here: the detail page
 * owns both — the activity is fixed by the route, and the date belongs to the page's day selector.
 */
@Composable
fun CardioEntryForm(
    state: CardioEditState,
    onDurationChange: (String) -> Unit,
    onDistanceChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onHeartRateChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.durationMinutes,
            onValueChange = onDurationChange,
            label = { Text("Dauer (Minuten)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.distanceKm,
            onValueChange = onDistanceChange,
            label = { Text("Distanz (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.caloriesBurned,
            onValueChange = onCaloriesChange,
            label = { Text("Verbrannte kcal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.avgHeartRateBpm,
            onValueChange = onHeartRateChange,
            label = { Text("Ø Herzfrequenz (bpm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text("Notiz") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
