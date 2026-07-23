package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.nutrition.food.BaseUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryAddEntryScreen(
    onDone: () -> Unit,
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
    val mealType by viewModel.mealType.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    LaunchedEffect(isSaved) {
        if (isSaved) onDone()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Eintrag hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = isValid) { Text("Speichern") }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Suche") },
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(modifier = Modifier.height(200.dp)) {
                if (sourceType == DiarySourceType.FOOD) {
                    items(foodResults, key = { it.id }) { food ->
                        Text(
                            food.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectFood(food) }
                                .padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(recipeResults, key = { it.recipe.id }) { recipe ->
                        Text(
                            recipe.recipe.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectRecipe(recipe) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }

            val amountLabel = when {
                selectedFood != null -> if (selectedFood?.baseUnit == BaseUnit.G) "Menge (g)" else "Menge (ml)"
                selectedRecipe != null -> "Portionen"
                else -> "Menge"
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text(amountLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = selectedFood != null || selectedRecipe != null,
                modifier = Modifier.fillMaxWidth(),
            )

            selectedFood?.let { food ->
                val amount = amountText.toDoubleOrNull()
                if (amount != null) {
                    val kcal = food.kcalPer100 * amount / 100.0
                    Text("${kcal.formatCompact()} kcal")
                }
            }
            selectedRecipe?.let { recipe ->
                val servings = amountText.toDoubleOrNull()
                if (servings != null) {
                    Text("${(recipe.perServing.kcal * servings).formatCompact()} kcal")
                }
            }

            Text("Mahlzeit")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
