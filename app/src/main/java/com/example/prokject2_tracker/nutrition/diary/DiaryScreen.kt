package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.fluid.FluidQuickAddArea
import com.example.prokject2_tracker.fluid.fluidDistributionSlices
import com.example.prokject2_tracker.fluid.fluidQuickAddItems
import com.example.prokject2_tracker.nutrition.food.Tag
import com.example.prokject2_tracker.nutrition.food.TagDots
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
    val copiedMeal by viewModel.copiedMeal.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN) }
    var showPasteTargetPicker by remember { mutableStateOf(false) }

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
            copiedMeal?.let { copied ->
                item(key = "clipboard") {
                    // Above the meals rather than below: on a day with nothing logged yet there is
                    // nothing below, and that is exactly when this bar is the only way to paste.
                    ClipboardBar(
                        copied = copied,
                        dateFormatter = shortDateFormatter,
                        onPaste = { showPasteTargetPicker = true },
                        onDiscard = viewModel::clearClipboard,
                    )
                }
            }
            if (loggedMeals.isNotEmpty()) {
                item(key = "meals") {
                    DiaryCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            loggedMeals.forEach { mealType ->
                                MealBlock(
                                    mealType = mealType,
                                    entries = uiState.entriesByMeal[mealType].orEmpty(),
                                    tagsBySource = uiState.tagsBySource,
                                    tagOrder = uiState.tagOrder,
                                    canPaste = copiedMeal != null,
                                    onAddEntry = { onAddEntry(uiState.epochDay, mealType) },
                                    onCopyMeal = { viewModel.copyMeal(mealType) },
                                    onPasteIntoMeal = { viewModel.pasteInto(mealType) },
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

    if (showPasteTargetPicker) {
        PasteTargetDialog(
            onPick = { mealType ->
                viewModel.pasteInto(mealType)
                showPasteTargetPicker = false
            },
            onDismiss = { showPasteTargetPicker = false },
        )
    }
}

/**
 * What is on the clipboard and where it came from, plus the two things to do with it. The day and
 * meal are spelled out because the copy outlives the day it was made on — "3 Einträge" alone would
 * leave you guessing which meal is about to land.
 */
@Composable
private fun ClipboardBar(
    copied: CopiedMeal,
    dateFormatter: DateTimeFormatter,
    onPaste: () -> Unit,
    onDiscard: () -> Unit,
) {
    DiaryCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${copied.mealType.label()} kopiert",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${DateUtils.localDateOfEpochDay(copied.epochDay).format(dateFormatter)} · " +
                        "${copied.entries.size} ${if (copied.entries.size == 1) "Eintrag" else "Einträge"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onPaste) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Einfügen")
            }
            IconButton(onClick = onDiscard) {
                Icon(Icons.Filled.Close, contentDescription = "Kopie verwerfen")
            }
        }
    }
}

/** Which Tageszeit the copy goes into — all four, not just the ones the day already shows. */
@Composable
private fun PasteTargetDialog(onPick: (MealType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("In welche Tageszeit einfügen?") },
        text = {
            Column {
                MealType.entries.forEach { mealType ->
                    Text(
                        mealType.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(mealType) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealBlock(
    mealType: MealType,
    entries: List<DiaryEntry>,
    tagsBySource: Map<Pair<DiarySourceType, String>, List<Tag>>,
    tagOrder: List<String>,
    canPaste: Boolean,
    onAddEntry: () -> Unit,
    onCopyMeal: () -> Unit,
    onPasteIntoMeal: () -> Unit,
    onEditEntry: (String) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // The heading is the shortcut for logging into this meal — it is the one place on the page
        // that already names the meal, so nothing else has to ask which one you meant. Long-pressing
        // it copies the whole Tageszeit; the buzz says the copy took, the paste bar shows what.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onAddEntry,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCopyMeal()
                    },
                    onLongClickLabel = "${mealType.label()} kopieren",
                )
                .padding(vertical = 4.dp),
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
            // Only while something is copied: a paste button with an empty clipboard behind it is
            // just a dead control on every meal of every day.
            if (canPaste) {
                IconButton(onClick = onPasteIntoMeal) {
                    Icon(
                        Icons.Filled.ContentPaste,
                        contentDescription = "In ${mealType.label()} einfügen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
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
                    tags = tagsBySource[entry.sourceType to entry.sourceId].orEmpty(),
                    tagOrder = tagOrder,
                    onEdit = { onEditEntry(entry.id) },
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }
}

@Composable
private fun DiaryEntryRow(
    entry: DiaryEntry,
    tags: List<Tag>,
    tagOrder: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
            // Smaller than the meal heading above it — it used to be the larger of the two, which
            // made the entries read as the headings.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Dots rather than names: this row is already tight, and spelling the tags out
                // would either wrap it or crowd out the food's own name.
                TagDots(tags = tags, tagOrder = tagOrder)
                Text(entry.sourceName, style = MaterialTheme.typography.bodyMedium)
            }
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
