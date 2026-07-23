package com.example.prokject2_tracker.fitness

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.prokject2_tracker.fitness.cardio.CardioListContent
import com.example.prokject2_tracker.fitness.strength.StrengthLogListContent

/** Hosts the fitness domain: Cardio and Kraft (strength) tabs, plus exercise-library management. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onAddCardioSession: () -> Unit,
    onEditCardioSession: (String) -> Unit,
    onAddStrengthLogEntry: () -> Unit,
    onEditStrengthLogEntry: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    val tabs = listOf("Cardio", "Kraft")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Fitness") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Mehr")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Übungen verwalten") },
                            onClick = {
                                showMenu = false
                                onOpenExerciseLibrary()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> CardioListContent(
                    onAddSession = onAddCardioSession,
                    onEditSession = onEditCardioSession,
                    modifier = Modifier.weight(1f),
                )
                1 -> StrengthLogListContent(
                    onAddEntry = onAddStrengthLogEntry,
                    onEditEntry = onEditStrengthLogEntry,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
