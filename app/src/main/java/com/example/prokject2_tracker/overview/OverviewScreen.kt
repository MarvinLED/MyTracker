package com.example.prokject2_tracker.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.datastore.WeightUnit
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.kgToLb
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fluid.FluidType
import com.example.prokject2_tracker.habit.Habit
import com.example.prokject2_tracker.habit.HabitType
import com.example.prokject2_tracker.habit.HabitValueDialog
import com.example.prokject2_tracker.nutrition.diary.MealType
import com.example.prokject2_tracker.nutrition.diary.label
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val foodQuery by viewModel.foodQuery.collectAsState()
    val foodResults by viewModel.foodResults.collectAsState()
    val selectedFood by viewModel.selectedFood.collectAsState()
    val amountText by viewModel.amountText.collectAsState()
    val mealType by viewModel.mealType.collectAsState()

    var valueDialogHabit by remember { mutableStateOf<Habit?>(null) }
    var weightInput by remember { mutableStateOf("") }

    val unitLabel = when (uiState.weightUnit) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }

    LaunchedEffect(uiState.todayWeightKg, uiState.weightUnit) {
        val kg = uiState.todayWeightKg
        weightInput = if (kg == null) {
            ""
        } else {
            when (uiState.weightUnit) {
                WeightUnit.KG -> kg
                WeightUnit.LB -> kg.kgToLb()
            }.formatDecimal(1)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Übersicht") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HabitsSection(
                habits = uiState.habits,
                checkedInHabitIds = uiState.checkedInHabitIds,
                habitValues = uiState.habitValues,
                habitStreaks = uiState.habitStreaks,
                onToggleHabit = viewModel::onToggleHabit,
                onOpenValueDialog = { habit -> valueDialogHabit = habit },
            )

            FluidSection(
                fluidTypes = uiState.fluidTypes,
                totalMl = uiState.fluidTotalMl,
                goalMl = uiState.fluidGoalMl,
                onQuickAdd = viewModel::onQuickAddFluid,
            )

            FoodSection(
                foodQuery = foodQuery,
                foodResults = foodResults,
                selectedFood = selectedFood,
                amountText = amountText,
                mealType = mealType,
                onFoodQueryChange = viewModel::onFoodQueryChange,
                onSelectFood = viewModel::onSelectFood,
                onAmountChange = viewModel::onAmountChange,
                onMealTypeChange = viewModel::onMealTypeChange,
                onConfirm = viewModel::confirmLogFood,
            )

            WeightSection(
                unitLabel = unitLabel,
                weightInput = weightInput,
                onWeightInputChange = { weightInput = it },
                onSave = {
                    viewModel.onSaveWeight(weightInput)
                },
            )
        }
    }

    valueDialogHabit?.let { habit ->
        HabitValueDialog(
            habit = habit,
            initialValue = uiState.habitValues[habit.id],
            onConfirm = { value ->
                viewModel.onLogHabitValue(habit.id, value)
                valueDialogHabit = null
            },
            onDismiss = { valueDialogHabit = null },
        )
    }
}

@Composable
private fun HabitsSection(
    habits: List<Habit>,
    checkedInHabitIds: Set<String>,
    habitValues: Map<String, Double>,
    habitStreaks: Map<String, Int>,
    onToggleHabit: (String) -> Unit,
    onOpenValueDialog: (Habit) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Habits", style = MaterialTheme.typography.titleMedium)
        if (habits.isEmpty()) {
            Text("Keine aktiven Habits.", style = MaterialTheme.typography.bodyMedium)
        }
        habits.forEach { habit ->
            val streak = habitStreaks[habit.id] ?: 0
            when (habit.type) {
                HabitType.YES_NO -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = habit.id in checkedInHabitIds,
                            onCheckedChange = { onToggleHabit(habit.id) },
                        )
                        Text(habit.name, modifier = Modifier.weight(1f))
                        if (streak > 0) {
                            Text("🔥 $streak", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                HabitType.COUNT, HabitType.DURATION -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenValueDialog(habit) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(habit.name, modifier = Modifier.weight(1f))
                        val subtitle = buildString {
                            if (streak > 0) append("🔥 $streak")
                            if (isNotEmpty()) append(" · ")
                            append(habitValues[habit.id]?.let { formatHabitValue(it) } ?: "–")
                        }
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun formatHabitValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
private fun FluidSection(
    fluidTypes: List<FluidType>,
    totalMl: Double,
    goalMl: Double,
    onQuickAdd: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Flüssigkeiten", style = MaterialTheme.typography.titleMedium)
        if (fluidTypes.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fluidTypes.forEach { type ->
                    AssistChip(
                        onClick = { onQuickAdd(type.id) },
                        label = { Text("${type.name} +${type.defaultQuickAddMl.formatDecimal(3)}") },
                    )
                }
            }
        }
        Text(
            "${totalMl.formatDecimal(3)} / ${goalMl.formatDecimal(3)} ml",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun FoodSection(
    foodQuery: String,
    foodResults: List<FoodItem>,
    selectedFood: FoodItem?,
    amountText: String,
    mealType: MealType,
    onFoodQueryChange: (String) -> Unit,
    onSelectFood: (FoodItem) -> Unit,
    onAmountChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Essen", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = foodQuery,
            onValueChange = onFoodQueryChange,
            label = { Text("Lebensmittel suchen") },
            modifier = Modifier.fillMaxWidth(),
        )
        foodResults.take(5).forEach { food ->
            Text(
                food.brand?.let { "${food.name} ($it)" } ?: food.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFood(food) }
                    .padding(vertical = 4.dp),
            )
        }
        selectedFood?.let { food ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(food.name, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    label = { Text(if (food.baseUnit == BaseUnit.G) "Menge (g)" else "Menge (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { candidate ->
                        FilterChip(
                            selected = mealType == candidate,
                            onClick = { onMealTypeChange(candidate) },
                            label = { Text(candidate.label()) },
                        )
                    }
                }
                Button(
                    onClick = onConfirm,
                    enabled = amountText.toDoubleOrNull()?.let { it > 0.0 } == true,
                ) {
                    Text("Eintragen")
                }
            }
        }
    }
}

@Composable
private fun WeightSection(
    unitLabel: String,
    weightInput: String,
    onWeightInputChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gewicht", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = onWeightInputChange,
                label = { Text("Gewicht ($unitLabel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSave,
                enabled = weightInput.toLocaleDoubleOrNull() != null,
            ) {
                Text("Speichern")
            }
        }
    }
}
