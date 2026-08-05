package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flatware
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.nutrition.food.FoodAmountInput
import com.example.prokject2_tracker.nutrition.food.amountInBaseUnits
import com.example.prokject2_tracker.nutrition.recipe.RecipeWithNutrition
import com.example.prokject2_tracker.ui.theme.AppDomain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryAddEntryScreen(
    onDone: () -> Unit,
    onCreateFood: () -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryAddEntryViewModel = hiltViewModel(),
) {
    val mode by viewModel.mode.collectAsState()
    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val pickerItems by viewModel.pickerItems.collectAsState()
    val expandedItem by viewModel.expandedItem.collectAsState()
    val amountText by viewModel.amountText.collectAsState()
    val foodUnits by viewModel.foodUnits.collectAsState()
    val selectedUnitId by viewModel.selectedUnitId.collectAsState()
    val quick by viewModel.quick.collectAsState()
    val mealType by viewModel.mealType.collectAsState()
    val addedConfirmation by viewModel.addedConfirmation.collectAsState(initial = "")
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.addedConfirmation.collect { name ->
            snackbarHostState.showSnackbar("\"$name\" hinzugefügt")
        }
    }

    Scaffold(
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
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Mode and MealType icon buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiaryPickerMode.entries.forEach { m ->
                    IconButtonWithTooltip(
                        isSelected = mode == m,
                        onClick = { viewModel.onModeChange(m) },
                        icon = getModeIcon(m),
                        label = m.label(),
                    )
                }
                MealType.entries.forEach { type ->
                    MealTypeIconButton(
                        mealType = type,
                        isSelected = mealType == type,
                        onClick = { viewModel.onMealTypeChange(type) },
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = AppDomain.DIARY.accent())

            if (mode == DiaryPickerMode.QUICK) {
                QuickEntryForm(state = quick, viewModel = viewModel)
            } else {
                // Search field
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

                // Sort chips
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DiaryPickerSort.entries.forEach { s ->
                        FilterChip(
                            selected = sort == s,
                            onClick = { viewModel.onSortChange(s) },
                            label = { Text(s.label()) },
                        )
                    }
                }

                // Tag chips (horizontally scrolling)
                if (allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedTagId == null,
                            onClick = { viewModel.onTagSelected(null) },
                            label = { Text("Alle") },
                        )
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTagId == tag.id,
                                onClick = { viewModel.onTagSelected(tag.id) },
                                label = { Text(tag.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }

                // Unified picker list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (pickerItems.isNotEmpty()) 200.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(pickerItems, key = { "${it.sourceType}:${it.id}" }) { item ->
                        ItemRow(
                            item = item,
                            isExpanded = expandedItem?.id == item.id && expandedItem?.sourceType == item.sourceType,
                            showTypeLabel = mode == DiaryPickerMode.ALL,
                            onClick = { viewModel.onRowTapped(item) },
                        )

                        if (expandedItem?.id == item.id && expandedItem?.sourceType == item.sourceType) {
                            ExpandedItemPanel(
                                item = item,
                                amountText = amountText,
                                onAmountChange = viewModel::onAmountChange,
                                units = foodUnits,
                                selectedUnitId = selectedUnitId,
                                onUnitSelected = viewModel::selectUnit,
                                onConfirm = {
                                    focusManager.clearFocus()
                                    when (item) {
                                        is DiaryPickerItem.Food -> viewModel.confirmAdd()
                                        is DiaryPickerItem.Recipe -> viewModel.confirmAdd()
                                    }
                                },
                            )
                        }
                    }

                    // Create buttons
                    item(key = "create") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (mode == DiaryPickerMode.ALL || mode == DiaryPickerMode.FOOD) {
                                TextButton(
                                    onClick = onCreateFood,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Lebensmittel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (mode == DiaryPickerMode.ALL || mode == DiaryPickerMode.RECIPE) {
                                TextButton(
                                    onClick = onCreateRecipe,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rezept", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: DiaryPickerItem,
    isExpanded: Boolean,
    showTypeLabel: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    if (showTypeLabel) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (item) {
                                is DiaryPickerItem.Food -> ""
                                is DiaryPickerItem.Recipe -> "Rezept"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val subtitle = when (item) {
                    is DiaryPickerItem.Food -> {
                        buildString {
                            item.food.brand?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                            append("${item.food.kcalPer100.formatCompact()} kcal / 100 g")
                        }
                    }
                    is DiaryPickerItem.Recipe -> "${item.recipe.perServing.kcal.formatCompact()} kcal / Portion"
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.tags.isNotEmpty()) {
                    Text(
                        item.tags.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isExpanded) {
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
private fun ExpandedItemPanel(
    item: DiaryPickerItem,
    amountText: String,
    onAmountChange: (String) -> Unit,
    units: List<com.example.prokject2_tracker.nutrition.food.FoodUnit>,
    selectedUnitId: String?,
    onUnitSelected: (String?) -> Unit,
    onConfirm: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (item) {
                is DiaryPickerItem.Food -> {
                    FoodAmountInput(
                        amountText = amountText,
                        onAmountChange = onAmountChange,
                        units = units,
                        selectedUnitId = selectedUnitId,
                        onUnitSelected = onUnitSelected,
                        baseUnit = item.food.baseUnit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is DiaryPickerItem.Recipe -> {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = onAmountChange,
                        label = { Text("Portionen") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val amount = when (item) {
                is DiaryPickerItem.Food -> {
                    val unit = units.firstOrNull { it.id == selectedUnitId }
                    amountInBaseUnits(amountText, unit)
                }
                is DiaryPickerItem.Recipe -> amountText.toLocaleDoubleOrNull()
            }

            if (amount != null && amount > 0.0) {
                when {
                    item is DiaryPickerItem.Food -> {
                        val factor = amount / 100.0
                        NutritionPreview(
                            kcal = item.food.kcalPer100 * factor,
                            protein = item.food.proteinPer100 * factor,
                            carbs = item.food.carbsPer100 * factor,
                            fat = item.food.fatPer100 * factor,
                        )
                    }
                    item is DiaryPickerItem.Recipe -> {
                        item.recipe.perServing.let { per ->
                            NutritionPreview(
                                kcal = per.kcal * amount,
                                protein = per.protein * amount,
                                carbs = per.carbs * amount,
                                fat = per.fat * amount,
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = dismissingKeyboard(onConfirm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hinzufügen")
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
                "Protein ${protein.formatCompact()} g · Kohlenhydrate ${carbs.formatCompact()} g · Fett ${fat.formatCompact()} g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun QuickEntryForm(state: QuickEntryState, viewModel: DiaryAddEntryViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Einmaliger Eintrag — keine Angaben pro 100 g, nur die Gesamtwerte. Es wird nichts in der Bibliothek angelegt.",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.kcal,
                    onValueChange = viewModel::onQuickKcalChange,
                    label = { Text("Kalorien (kcal)", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.protein,
                    onValueChange = viewModel::onQuickProteinChange,
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.carbs,
                    onValueChange = viewModel::onQuickCarbsChange,
                    label = { Text("Kohlenhydrate (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.fat,
                    onValueChange = viewModel::onQuickFatChange,
                    label = { Text("Fett (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                onClick = dismissingKeyboard { viewModel.confirmQuick() },
                enabled = state.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hinzufügen")
            }
        }
    }
}

@Composable
private fun IconButtonWithTooltip(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    val showTooltip = remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showTooltip.value = true },
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )
        }

        if (showTooltip.value) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                showTooltip.value = false
            }
        }
    }
}

@Composable
private fun MealTypeIcon(mealType: MealType, color: Color, label: String) {
    when (mealType) {
        MealType.BREAKFAST -> AnalogClockIcon(hour = 9, color = color)
        MealType.LUNCH -> AnalogClockIcon(hour = 12, color = color)
        MealType.DINNER -> AnalogClockIcon(hour = 15, color = color)
        MealType.SNACK -> Icon(
            Icons.Filled.Cake,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun getModeIcon(mode: DiaryPickerMode) = when (mode) {
    DiaryPickerMode.ALL -> Icons.Filled.AllInclusive
    DiaryPickerMode.FOOD -> Icons.Filled.Flatware
    DiaryPickerMode.RECIPE -> Icons.Filled.Restaurant
    DiaryPickerMode.QUICK -> Icons.Filled.Bolt
}

@Composable
private fun MealTypeIconButton(
    mealType: MealType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val showTooltip = remember { mutableStateOf(false) }
    val label = mealType.label()
    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
                .clip(androidx.compose.foundation.shape.CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showTooltip.value = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            MealTypeIcon(mealType, iconColor, label)
        }

        if (showTooltip.value) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                showTooltip.value = false
            }
        }
    }
}
