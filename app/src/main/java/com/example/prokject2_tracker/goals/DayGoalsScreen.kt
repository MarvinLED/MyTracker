package com.example.prokject2_tracker.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.statusColor
import com.example.prokject2_tracker.ui.theme.topAppBarColors

private val BarShape = RoundedCornerShape(6.dp)
private val BarHeight = 10.dp

/**
 * The day's goals in one place: what was set, where it stands, and whether that counts as met. A
 * pure overview — the targets are edited on the Ziele screen, which the flag in the bar leads to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayGoalsScreen(
    onOpenDrawer: () -> Unit,
    onEditGoals: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayGoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.GOALS.topAppBarColors(),
                title = { Text("Tagesziele") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                actions = {
                    IconButton(onClick = onEditGoals) {
                        Icon(Icons.Filled.Flag, contentDescription = "Ziele bearbeiten")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Noch keine Tagesziele gesetzt — unter Ziele lassen sich Nährwerte, " +
                        "Flüssigkeit, Habits und Training festlegen, unter Aufgaben einmalige " +
                        "und wiederkehrende To-dos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${uiState.metCount} von ${uiState.total} Zielen erreicht",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            uiState.sections.forEach { section ->
                item(key = "section-${section.title}") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(section.title, style = MaterialTheme.typography.titleSmall)
                            section.rows.forEach { row -> DayGoalRowItem(row) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayGoalRowItem(row: DayGoalRow) {
    val color = statusColor(row.isMet)
    val status = if (row.isMet) "Ziel erreicht" else "Ziel nicht erreicht"

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    row.valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // A goal that is a matter of degree gets the bar; the rest is answered by the icon
            // beside it, and drawing both would say the same thing twice.
            row.fraction?.let { fraction ->
                GoalBar(fraction = fraction, color = color, statusDescription = status)
            }
        }
        if (row.fraction == null) {
            Icon(
                if (row.isMet) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = status,
                tint = color,
                modifier = Modifier.padding(start = 12.dp).size(24.dp),
            )
        }
    }
}

@Composable
private fun GoalBar(fraction: Float, color: Color, statusDescription: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .clip(BarShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            // Spoken instead of nothing at all: the fill's colour is what says met or missed, and a
            // screen reader sees no colours.
            .semantics { contentDescription = statusDescription },
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}
