package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.fluid.FluidQuickAddArea
import com.example.prokject2_tracker.fluid.fluidDistributionSlices
import com.example.prokject2_tracker.fluid.fluidQuickAddItems
import com.example.prokject2_tracker.nutrition.food.formatAmount
import com.example.prokject2_tracker.ui.theme.DiaryBlue
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The app's home screen. The page itself is [DiaryBlue]; everything below the date sits on a card,
 * because white body text does not carry enough contrast on that blue — see the colour's KDoc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddEntry: (Long, MealType) -> Unit,
    onEditEntry: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onManageFluidQuickAdds: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoableDelete by viewModel.undoableDelete.collectAsState()
    val quickAdds by viewModel.fluidQuickAdds.collectAsState()
    val undoableFluidAdd by viewModel.undoableFluidAdd.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }

    Scaffold(
        modifier = modifier,
        containerColor = DiaryBlue,
        topBar = {
            TopAppBar(
                // Same colour as the page, so the bar and the content read as one surface instead
                // of leaving a seam across the top.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DiaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::goToPreviousDay) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorheriger Tag")
                        }
                        Text(
                            DateUtils.localDateOfEpochDay(uiState.epochDay).format(dateFormatter),
                            modifier = Modifier.weight(1f),
                            // Large and semi-bold: at this size white clears the 3:1 that large text
                            // needs on DiaryBlue, which body-sized text on that blue would not.
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = viewModel::goToNextDay) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Nächster Tag")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            // Only the undo is a FAB now; adding has its own button in the page. It stays put rather
            // than living in a snackbar, so it's reachable for as long as the day is on screen.
            undoableDelete?.let { deleted ->
                SmallFloatingActionButton(
                    onClick = viewModel::undoDelete,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(
                        Icons.Filled.Undo,
                        contentDescription = "Löschen von \"${deleted.entry.sourceName}\" rückgängig machen",
                    )
                }
            }
        },
    ) { padding ->
        val fluidSlices = fluidDistributionSlices(uiState.fluidEntries, uiState.fluidTypes)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Macros, calories and fluid are one block, not three: they are all "how is the day
            // going", and page colour between them would split one answer into three.
            item(key = "day-bars") {
                DiaryCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MacroBars(totals = uiState.totals, goals = uiState.nutrientGoals)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                        CalorieBar(consumedKcal = uiState.totalKcal, goal = uiState.calorieGoal)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                        FluidBalanceBar(
                            slices = fluidSlices,
                            goalMl = uiState.fluidGoalMl,
                            // Unfolds with the legend: the legend says which drink is which colour,
                            // and these buttons are those same colours.
                            expandedContent = {
                                FluidQuickAddArea(
                                    items = fluidQuickAddItems(quickAdds, uiState.fluidTypes),
                                    onQuickAdd = viewModel::quickAddFluid,
                                    onUndo = viewModel::undoFluidAdd,
                                    canUndo = undoableFluidAdd != null,
                                    onManage = onManageFluidQuickAdds,
                                )
                            },
                        )
                    }
                }
            }
            item(key = "add") {
                // The cards' own surface, so the buttons sit in the same family as the blocks
                // above and below them instead of being a third colour on the page.
                val buttonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        // Read at tap time, not at composition: the screen can sit open across the
                        // boundary between two meals.
                        onClick = { onAddEntry(uiState.epochDay, defaultMealType(LocalTime.now())) },
                        colors = buttonColors,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        // Half the width no longer fits the full "Lebensmittel hinzufügen"; the
                        // plus carries the "add" half of the meaning.
                        Text("Lebensmittel")
                    }
                    Button(
                        onClick = onOpenLibrary,
                        colors = buttonColors,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Kitchen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bibliothek")
                    }
                }
            }
            // Only the meals that hold something, all in one card: an empty block says nothing the
            // missing block doesn't, and four of them on a fresh day are four rows of "Nichts
            // eingetragen." above the first real entry.
            val loggedMeals = MealType.entries.filter { !uiState.entriesByMeal[it].isNullOrEmpty() }
            if (loggedMeals.isNotEmpty()) {
                item(key = "meals") {
                    DiaryCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            loggedMeals.forEach { mealType ->
                                MealBlock(
                                    mealType = mealType,
                                    entries = uiState.entriesByMeal[mealType].orEmpty(),
                                    onAddEntry = { onAddEntry(uiState.epochDay, mealType) },
                                    onEditEntry = onEditEntry,
                                    onDeleteEntry = viewModel::deleteEntry,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A dark card on the blue page — the surface every value and label on this screen sits on. */
@Composable
private fun DiaryCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun MealBlock(
    mealType: MealType,
    entries: List<DiaryEntry>,
    onAddEntry: () -> Unit,
    onEditEntry: (String) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // The heading is the shortcut for logging into this meal — it is the one place on the page
        // that already names the meal, so nothing else has to ask which one you meant.
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onAddEntry).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                mealType.label(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${entries.sumOf { it.kcal }.formatCompact()} kcal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Add,
                contentDescription = "Lebensmittel zu ${mealType.label()} hinzufügen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Indented under their heading: the entries and the meal names are otherwise two stacks of
        // similar-looking lines, and nothing says which belongs to which.
        Column(modifier = Modifier.padding(start = 16.dp)) {
            entries.forEach { entry ->
                DiaryEntryRow(
                    entry = entry,
                    onEdit = { onEditEntry(entry.id) },
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }
}

@Composable
private fun DiaryEntryRow(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
            // Smaller than the meal heading above it — it used to be the larger of the two, which
            // made the entries read as the headings.
            Text(entry.sourceName, style = MaterialTheme.typography.bodyMedium)
            // A Schnelleintrag has no meaningful quantity — its "1 Schnelleintrag" would just be noise.
            val details = if (entry.sourceType == DiarySourceType.QUICK) {
                "${entry.quantityUnit} · ${entry.kcal.formatCompact()} kcal"
            } else {
                val amount = formatAmount(
                    amountBaseUnits = entry.quantity,
                    unitName = entry.unitName,
                    unitCount = entry.unitCount,
                    baseUnitLabel = entry.quantityUnit,
                )
                "$amount · ${entry.kcal.formatCompact()} kcal"
            }
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Löschen")
        }
    }
}
