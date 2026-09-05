package com.example.mytracker.goals

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.formatDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.TimeOfDayField
import com.example.mytracker.core.ui.dismissingKeyboard
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.label
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Only one section at a time by default — all four at once is more than fits on a screen.
    // null means "Alle". Pure view state: the whole form stays loaded and is saved either way.
    var selectedCategory by rememberSaveable { mutableStateOf<GoalCategory?>(GoalCategory.NUTRITION) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { snackbarHostState.showSnackbar("Ziele gespeichert") }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        topBar = {
            TopAppBar(
                colors = AppDomain.GOALS.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Ziele") },
                actions = {
                    TextButton(onClick = dismissingKeyboard(viewModel::save)) { Text("Speichern") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Outside the scrolling column on purpose: the filter stays reachable while a long
            // section is scrolled.
            GoalCategoryFilter(
                selected = selectedCategory,
                onSelected = { selectedCategory = it },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                fun shows(category: GoalCategory) = selectedCategory == null || selectedCategory == category

                if (shows(GoalCategory.NUTRITION)) NutritionGoalsSection(state, viewModel)
                if (shows(GoalCategory.SLEEP)) SleepGoalsSection(state, viewModel)
                if (shows(GoalCategory.FLUID)) FluidGoalsSection(state, viewModel)
                if (shows(GoalCategory.FITNESS)) FitnessGoalsSection(state, viewModel)
            }
        }
    }
}

/**
 * The category filter as one chip that opens a menu. Single selection: "Alle" shows every
 * section again, any other entry replaces what was picked.
 *
 * Same shape as the Bibliothek's tag filter (`TagFilterButton` in `LibraryFilterButtons`): a plain
 * [DropdownMenu] anchored on the chip rather than an `ExposedDropdownMenuBox`, which would want a
 * text field as its anchor.
 */
@Composable
private fun GoalCategoryFilter(
    selected: GoalCategory?,
    onSelected: (GoalCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.label ?: "Alle") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Kategorie wählen") },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle") },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selected == null) Icon(Icons.Filled.Check, contentDescription = null)
                },
            )
            GoalCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selected == category) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun NutritionGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.NUTRITION.label, style = MaterialTheme.typography.titleMedium)
        Text(
            "Minimum, Maximum oder beides — leer lassen heißt \"kein Ziel\". Der Balken im " +
                "Tagebuch läuft bis zum Maximum und markiert das Minimum darin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.nutrientGoals.forEach { row ->
            NutrientGoalRow(
                row = row,
                onMinChange = { viewModel.onNutrientGoalMinChange(row.nutrient, it) },
                onMaxChange = { viewModel.onNutrientGoalMaxChange(row.nutrient, it) },
            )
        }
    }
}

@Composable
private fun SleepGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.SLEEP.label, style = MaterialTheme.typography.titleMedium)
        Text(
            "Schlafdauer in Stunden (7,5 = 7 h 30 min) und die Uhrzeit, zu der du spätestens " +
                "schlafen willst. Leer lassen heißt \"kein Ziel\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.sleepDurationMinHours,
                onValueChange = viewModel::onSleepDurationMinChange,
                label = { Text("Mindestens (h)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.sleepDurationMaxHours,
                onValueChange = viewModel::onSleepDurationMaxChange,
                label = { Text("Höchstens (h)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TimeOfDayField(
                label = "Schlafenszeit spätestens",
                value = state.bedtimeGoalMinuteOfDay,
                onValueChange = viewModel::onBedtimeGoalChange,
                emptyLabel = "kein Ziel",
                defaultMinuteOfDay = 23 * 60,
            )
            if (state.bedtimeGoalMinuteOfDay != null) {
                TextButton(onClick = { viewModel.onBedtimeGoalChange(null) }) { Text("Ziel entfernen") }
            }
        }
    }
}

@Composable
private fun FluidGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.FLUID.label, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.waterGoal,
            onValueChange = viewModel::onWaterGoalChange,
            label = { Text("Gesamtziel pro Tag (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        state.fluidTypeGoals.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.type.name, style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = row.minText,
                        onValueChange = { viewModel.onFluidTypeMinChange(row.type.id, it) },
                        label = { Text("Minimum (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = row.maxText,
                        onValueChange = { viewModel.onFluidTypeMaxChange(row.type.id, it) },
                        label = { Text("Maximum (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Every fitness goal there is, in sections — not a dropdown that has to be asked before anything can
 * be typed. Each row offers the same goal weekly and monthly side by side; an empty field means
 * "kein Ziel" and clears it on save.
 *
 * The per-exercise sections are folded away by default. A library of twenty exercises is four
 * fields each, and unfolded that is a screen nobody scrolls to the bottom of.
 */
@Composable
private fun FitnessGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(GoalCategory.FITNESS.label, style = MaterialTheme.typography.titleMedium)

        state.fitnessGoalSections.forEach { section ->
            val maxWeightGoal = section.rows.firstOrNull()?.exerciseId?.let { exerciseId ->
                state.maxWeightGoals.firstOrNull { it.exerciseId == exerciseId }
            }
            when {
                maxWeightGoal != null ->
                    FoldableGoalGroup(section, maxWeightGoal, state.bodyWeightKg, viewModel)
                // Muskelgruppen and Bewegungsrichtungen carry two rows each and run long; Cardio and
                // Kraft gesamt are three rows in total and fold to nothing worth hiding.
                section.rows.size > MaxUnfoldedGoalRows ->
                    FoldableGoalGroup(section, maxWeightGoal = null, bodyWeightKg = null, viewModel = viewModel)
                else -> FitnessGoalGroup(section = section, viewModel = viewModel)
            }
        }

        GoalChangeHistory(rows = state.goalChanges)
    }
}

/**
 * What the targets have been, and since when. The goal rows above are overwritten in place, so this
 * log is the only place the question "seit wann trainiere ich eigentlich auf 100 kg?" has an answer
 * at all — and the only way to tell a target that was never reached from one that was raised twice.
 *
 * Folded away by default: it is for looking back, not for setting anything.
 */
@Composable
private fun GoalChangeHistory(rows: List<GoalChangeRow>) {
    if (rows.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Zieländerungen",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
            )
        }
        if (expanded) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(row.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${row.dateText} · ${row.changeText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One section's heading and its rows. */
@Composable
private fun FitnessGoalGroup(section: FitnessGoalSection, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(section.title, style = MaterialTheme.typography.titleSmall)
        section.rows.forEach { row -> FitnessGoalRowFields(row, viewModel) }
    }
}

/**
 * A section folded away until opened: an exercise's three goals, or one of the long scope lists.
 * A library of twenty exercises is four fields each, and unfolded that is a screen nobody scrolls
 * to the bottom of.
 *
 * [maxWeightGoal] is what makes it an exercise block — the two Steigerungen and the long-term target
 * belong together because they are three answers to the same question, how this lift is meant to
 * move, at three horizons.
 */
@Composable
private fun FoldableGoalGroup(
    section: FitnessGoalSection,
    maxWeightGoal: MaxWeightGoalRow?,
    bodyWeightKg: Double?,
    viewModel: GoalsViewModel,
) {
    val hasGoals = section.rows.any { it.weeklyText.isNotBlank() || it.monthlyText.isNotBlank() } ||
        (maxWeightGoal != null && maxWeightGoal.targetText.isNotBlank() && maxWeightGoal.targetEpochDay != null)
    // Opened when something is set: a goal that is folded out of sight is one nobody remembers is on.
    var expanded by rememberSaveable(section.title) { mutableStateOf(hasGoals) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(section.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (hasGoals && !expanded) {
                Text(
                    "Ziel gesetzt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
            )
        }
        if (expanded) {
            section.rows.forEach { row -> FitnessGoalRowFields(row, viewModel) }
            maxWeightGoal?.let { MaxWeightGoalFields(row = it, bodyWeightKg = bodyWeightKg, viewModel = viewModel) }
        }
    }
}

/** The label and the two period fields of one goal. */
@Composable
private fun FitnessGoalRowFields(row: FitnessGoalRow, viewModel: GoalsViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.bodyMedium)
            if (row.isIncrease) {
                // +2,5 kg is a different demand on Kreuzheben than on Seitheben; the percentage
                // scales with whatever the lift already is. One pair of chips governs both periods.
                UnitChoiceChips(
                    absoluteLabel = row.unit,
                    isRelative = row.isPercent,
                    onChange = { viewModel.onFitnessGoalPercentChange(row.key, it) },
                    relativeLabel = "%",
                )
            } else if (row.unit.isNotBlank()) {
                Text(
                    "in ${row.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = row.weeklyText,
            onValueChange = { viewModel.onFitnessGoalTargetChange(row.key, GoalPeriod.WEEKLY, it) },
            label = { Text("Woche") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp),
        )
        OutlinedTextField(
            value = row.monthlyText,
            onValueChange = { viewModel.onFitnessGoalTargetChange(row.key, GoalPeriod.MONTHLY, it) },
            label = { Text("Monat") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp),
        )
    }
}

/**
 * The long-term target for one exercise's top set: a weight and the date it is due by. Both or
 * neither — the date is what separates a plan from a wish, and it is also what "auf Kurs" is
 * computed against on the Fitness screen.
 *
 * The target can be a multiple of body weight instead of a fixed number. It then moves with the
 * body weight, which is the honest thing for a relative-strength goal: dropping five kilos really
 * does lower the bar, and a goal that ignored that would quietly turn into a heavier one.
 */
@Composable
private fun MaxWeightGoalFields(row: MaxWeightGoalRow, bodyWeightKg: Double?, viewModel: GoalsViewModel) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN) }
    var showDatePicker by remember { mutableStateOf(false) }
    val typed = row.targetText.toLocaleDoubleOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Langfristiges Ziel Maximalgewicht", style = MaterialTheme.typography.bodyMedium)
        UnitChoiceChips(
            absoluteLabel = "kg",
            isRelative = row.isRelative,
            onChange = { viewModel.onMaxWeightGoalRelativeChange(row.exerciseId, it) },
            relativeLabel = "× KG",
        )
        val current = row.currentMaxKg
        val relativeNow = current?.let { max -> bodyWeightKg?.takeIf { it > 0 }?.let { max / it } }
        current?.let {
            Text(
                "Aktuell: ${it.formatCompact()} kg" +
                    relativeNow?.let { relative -> " (${relative.formatDecimal(2)} × KG)" }.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (row.isRelative) {
            Text(
                bodyWeightKg?.let { weight ->
                    typed?.let { "Entspricht heute ${(it * weight).formatCompact()} kg bei ${weight.formatCompact()} kg KG" }
                        ?: "Körpergewicht: ${weight.formatCompact()} kg"
                }
                    // Without a logged body weight the multiple has nothing to multiply, and the goal
                    // would silently store a target of zero kilos.
                    ?: "Noch kein Körpergewicht erfasst — ein relatives Ziel braucht eines.",
                style = MaterialTheme.typography.labelSmall,
                color = if (bodyWeightKg == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = row.targetText,
                onValueChange = { viewModel.onMaxWeightGoalTargetChange(row.exerciseId, it) },
                label = { Text(if (row.isRelative) "Ziel (× KG)" else "Ziel (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(120.dp),
            )
            TextButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Text(
                    row.targetEpochDay
                        ?.let { "bis ${DateUtils.localDateOfEpochDay(it).format(dateFormatter)}" }
                        ?: "Zieldatum wählen",
                )
            }
            if (row.targetEpochDay != null) {
                IconButton(onClick = { viewModel.onMaxWeightGoalDateChange(row.exerciseId, null) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Zieldatum entfernen")
                }
            }
        }
    }

    if (showDatePicker) {
        TargetDatePickerDialog(
            epochDay = row.targetEpochDay,
            onPick = {
                viewModel.onMaxWeightGoalDateChange(row.exerciseId, it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * The two ways one target can be read: in its own unit, or relative — percent for a Steigerung,
 * a multiple of body weight for a long-term target. Chips rather than a switch because neither of
 * the two is the "on" state.
 */
@Composable
private fun UnitChoiceChips(
    absoluteLabel: String,
    relativeLabel: String,
    isRelative: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = !isRelative,
            onClick = { onChange(false) },
            label = { Text(absoluteLabel, style = MaterialTheme.typography.labelSmall) },
        )
        FilterChip(
            selected = isRelative,
            onClick = { onChange(true) },
            label = { Text(relativeLabel, style = MaterialTheme.typography.labelSmall) },
        )
    }
}

/** Past this many rows a section folds; below it, folding hides less than the fold itself costs. */
private const val MaxUnfoldedGoalRows = 4

/**
 * The calendar for a target date. The mirror image of the Blutdruck screen's: only days **after**
 * today are selectable, because a deadline in the past is one that can only be missed.
 *
 * [DatePickerState] works in UTC midnights while the app stores local epoch days, so both ends are
 * converted explicitly rather than by dividing millis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetDatePickerDialog(epochDay: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = DateUtils.todayEpochDay()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (epochDay ?: (today + 90)).epochDayToUtcMillis(),
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis.utcMillisToEpochDay() > today

                override fun isSelectableYear(year: Int): Boolean =
                    year >= DateUtils.localDateOfEpochDay(today).year
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(it.utcMillisToEpochDay()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(state = state)
    }
}

private fun Long.epochDayToUtcMillis(): Long =
    LocalDate.ofEpochDay(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToEpochDay(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

/**
 * One nutrient's goal: a lower and an upper bound, either of which may stay blank. Same two-field
 * shape as the Getränkearten below, so both kinds of goal are entered the same way.
 */
@Composable
private fun NutrientGoalRow(
    row: NutrientGoalInput,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
) {
    val min = row.minText.toLocaleDoubleOrNull()
    val max = row.maxText.toLocaleDoubleOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${row.nutrient.label} (${row.nutrient.unit})", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = row.minText,
                onValueChange = onMinChange,
                label = { Text("Minimum") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = row.maxText,
                onValueChange = onMaxChange,
                label = { Text("Maximum") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        // Such a goal can never be met, and nothing else in the app would say so.
        if (min != null && max != null && min > max) {
            Text(
                "Minimum liegt über dem Maximum — dieses Ziel ist nicht erreichbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
