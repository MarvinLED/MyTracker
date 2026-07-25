package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.recipe.RecipeWithNutrition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryAddEntryScreen(
    onDone: () -> Unit,
    onCreateFood: () -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryAddEntryViewModel = hiltViewModel(),
) {
    val sourceType by viewModel.sourceType.collectAsState()
    val query by viewModel.query.collectAsState()
    val foodResults by viewModel.foodResults.collectAsState()
    val recipeResults by viewModel.recipeResults.collectAsState()
    val selectedFood by viewModel.selectedFood.collectAsState()
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    val amountText by viewModel.amountText.collectAsState()
    val quick by viewModel.quick.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSaved) {
        if (isSaved) onDone()
    }

    Scaffold(
        // imePadding on the Scaffold itself lifts the whole frame — including the bottom save
        // bar — above the keyboard, instead of letting it cover the lower fields.
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Eintrag hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = viewModel::save,
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Speichern", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sourceType == DiarySourceType.FOOD,
                    onClick = { viewModel.selectSourceType(DiarySourceType.FOOD) },
                    label = { Text("Lebensmittel") },
                )
                FilterChip(
                    selected = sourceType == DiarySourceType.RECIPE,
                    onClick = { viewModel.selectSourceType(DiarySourceType.RECIPE) },
                    label = { Text("Rezept") },
                )
                FilterChip(
                    selected = sourceType == DiarySourceType.QUICK,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.selectSourceType(DiarySourceType.QUICK)
                    },
                    label = { Text("Schnell hinzufügen") },
                    leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            if (sourceType == DiarySourceType.QUICK) {
                QuickEntryForm(state = quick, viewModel = viewModel)
            } else {
                SelectionCard(
                    sourceType = sourceType,
                    food = selectedFood,
                    recipe = selectedRecipe,
                    onClear = viewModel::clearSelection,
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Suche") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Suche leeren")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (sourceType == DiarySourceType.FOOD) {
                        items(foodResults, key = { it.id }) { food ->
                            ResultRow(
                                title = food.name,
                                subtitle = buildString {
                                    food.brand?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                                    append("${food.kcalPer100.formatCompact()} kcal / 100 g")
                                },
                                selected = selectedFood?.id == food.id,
                                onClick = {
                                    // Closing the keyboard here keeps the Menge field and the
                                    // Mahlzeit chips visible right after picking something.
                                    focusManager.clearFocus()
                                    viewModel.selectFood(food)
                                },
                            )
                        }
                    } else {
                        items(recipeResults, key = { it.recipe.id }) { recipe ->
                            ResultRow(
                                title = recipe.recipe.name,
                                subtitle = "${recipe.perServing.kcal.formatCompact()} kcal / Portion",
                                selected = selectedRecipe?.recipe?.id == recipe.recipe.id,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.selectRecipe(recipe)
                                },
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { if (sourceType == DiarySourceType.FOOD) onCreateFood() else onCreateRecipe() },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (sourceType == DiarySourceType.FOOD) "Neues Lebensmittel anlegen" else "Neues Rezept anlegen",
                    )
                }

                HorizontalDivider()

                val amountLabel = when {
                    selectedFood != null -> if (selectedFood?.baseUnit == BaseUnit.G) "Menge (g)" else "Menge (ml)"
                    selectedRecipe != null -> "Portionen"
                    else -> "Menge"
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text(amountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    singleLine = true,
                    enabled = selectedFood != null || selectedRecipe != null,
                    modifier = Modifier.fillMaxWidth(),
                )

                selectedFood?.let { food ->
                    food.servingAmount?.takeIf { it > 0.0 }?.let { serving ->
                        AssistChip(
                            onClick = { viewModel.onAmountChange(serving.formatCompact()) },
                            label = {
                                Text("1 ${food.servingName.orEmpty().ifBlank { "Portion" }} (${serving.formatCompact()} g)")
                            },
                        )
                    }
                }

                val amount = amountText.toLocaleDoubleOrNull()
                if (amount != null && amount > 0.0) {
                    val totals = when {
                        selectedFood != null -> selectedFood!!.let { food ->
                            val factor = amount / 100.0
                            listOf(
                                food.kcalPer100 * factor,
                                food.proteinPer100 * factor,
                                food.carbsPer100 * factor,
                                food.fatPer100 * factor,
                            )
                        }
                        selectedRecipe != null -> selectedRecipe!!.perServing.let { per ->
                            listOf(per.kcal * amount, per.protein * amount, per.carbs * amount, per.fat * amount)
                        }
                        else -> null
                    }
                    totals?.let { (kcal, protein, carbs, fat) ->
                        NutritionPreview(kcal = kcal, protein = protein, carbs = carbs, fat = fat)
                    }
                }
            }

            Text("Mahlzeit", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MealType.entries.forEach { type ->
                    FilterChip(
                        selected = mealType == type,
                        onClick = { viewModel.onMealTypeChange(type) },
                        label = { Text(type.label()) },
                    )
                }
            }
        }
    }
}

/**
 * The always-visible answer to "what did I actually pick?" — the old screen only reflected the
 * selection in a text-field label, which was easy to miss.
 */
@Composable
private fun SelectionCard(
    sourceType: DiarySourceType,
    food: FoodItem?,
    recipe: RecipeWithNutrition?,
    onClear: () -> Unit,
) {
    if (food == null && recipe == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                if (sourceType == DiarySourceType.FOOD) {
                    "Noch kein Lebensmittel ausgewählt — unten suchen und antippen."
                } else {
                    "Noch kein Rezept ausgewählt — unten suchen und antippen."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Ausgewählt",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    food?.name ?: recipe?.recipe?.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                val details = food?.let { item ->
                    buildString {
                        item.brand?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                        append("${item.kcalPer100.formatCompact()} kcal / 100 g")
                    }
                } ?: recipe?.let { "${it.perServing.kcal.formatCompact()} kcal / Portion" }
                Text(
                    details.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (food?.fluidTypeId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalDrink,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "wird auch als Flüssigkeit gezählt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Auswahl aufheben",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun NutritionPreview(kcal: Double, protein: Double, carbs: Double, fat: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${kcal.formatCompact()} kcal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "Protein ${protein.formatCompact()} g · Kohlenhydrate ${carbs.formatCompact()} g · " +
                    "Fett ${fat.formatCompact()} g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** One-off entry: kcal is the only required value, everything else is optional and already a total. */
@Composable
private fun QuickEntryForm(state: QuickEntryState, viewModel: DiaryAddEntryViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Einmaliger Eintrag — keine Angaben pro 100 g, nur die Gesamtwerte. " +
                    "Es wird nichts in der Bibliothek angelegt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onQuickNameChange,
                label = { Text("Bezeichnung (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.kcal,
                onValueChange = viewModel::onQuickKcalChange,
                label = { Text("Kalorien (kcal)", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.protein,
                onValueChange = viewModel::onQuickProteinChange,
                label = { Text("Protein (g), optional") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.carbs,
                onValueChange = viewModel::onQuickCarbsChange,
                label = { Text("Kohlenhydrate (g), optional") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fat,
                onValueChange = viewModel::onQuickFatChange,
                label = { Text("Fett (g), optional") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
