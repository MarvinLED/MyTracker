package com.example.prokject2_tracker.fitness

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.prokject2_tracker.fitness.cardio.CardioListContent

/**
 * Hosts the fitness domain. Currently just Cardio; when Fitnessübungen (strength) lands, this
 * becomes a tabbed host the same way [com.example.prokject2_tracker.nutrition.library.LibraryScreen]
 * hosts Lebensmittel/Rezepte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onAddCardioSession: () -> Unit,
    onEditCardioSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Fitness") }) },
    ) { padding ->
        CardioListContent(
            onAddSession = onAddCardioSession,
            onEditSession = onEditCardioSession,
            modifier = Modifier.padding(padding),
        )
    }
}
