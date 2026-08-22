package com.example.mytracker.nutrition.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flatware
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.dismissingKeyboard
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.nutrition.food.FoodAmountInput
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagDot
import com.example.mytracker.nutrition.food.TagDots
import com.example.mytracker.nutrition.food.amountInBaseUnits
import com.example.mytracker.nutrition.food.displayColor
import com.example.mytracker.nutrition.recipe.RecipeWithNutrition
import com.example.mytracker.ui.theme.AppDomain
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
    val listMode by viewModel.listMode.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchExpanded by viewModel.searchExpanded.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    // The library order behind every tag colour on this screen — see TagColors.displayColor.
    val tagOrder = remember(allTags) { allTags.map { it.id } }
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
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.addedConfirmation.collect { name ->
            snackbarHostState.showSnackbar("\"$name\" hinzugefügt")
        }
    }

    // Unfolding the field is the whole gesture — asking for a second tap to start typing would
    // make the button worse than the always-visible field it replaced.
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) searchFocusRequester.requestFocus()
    }

    Scaffold(
        // No imePadding here: AppScaffold's contentWindowInsets are safeDrawing, which already
        // includes the IME, so adding it again left a keyboard-high empty strip below the list.
        modifier = modifier,
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
                .fillMaxSize()
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
                // Alle, Lebensmittel and Rezept share one button that cycles through them. Its label
                // stays visible: with no sibling buttons left to compare against, the icon on its own
                // would not say which of the three is currently on.
                IconButtonWithTooltip(
                    isSelected = mode != DiaryPickerMode.QUICK,
                    onClick = viewModel::cycleListMode,
                    icon = getModeIcon(listMode),
                    label = listMode.label(),
                    alwaysShowLabel = true,
                )
                IconButtonWithTooltip(
                    isSelected = mode == DiaryPickerMode.QUICK,
                    onClick = { viewModel.onModeChange(DiaryPickerMode.QUICK) },
                    icon = getModeIcon(DiaryPickerMode.QUICK),
                    label = DiaryPickerMode.QUICK.label(),
                )
                // Sits with the list controls rather than among the four Tageszeiten, which are a
                // different kind of choice: these three say what the list shows, those say which
                // meal the entry lands in.
                if (mode != DiaryPickerMode.QUICK) {
                    IconButtonWithTooltip(
                        isSelected = searchExpanded,
                        onClick = viewModel::onSearchToggle,
                        icon = Icons.Filled.Search,
                        label = "Suche",
                    )
                }
                MealType.entries.forEach { type ->
                    IconButtonWithTooltip(
                        isSelected = mealType == type,
                        onClick = { viewModel.onMealTypeChange(type) },
                        icon = getMealTypeIcon(type),
                        label = type.label(),
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = AppDomain.DIARY.accent())

            if (mode == DiaryPickerMode.QUICK) {
                QuickEntryForm(state = quick, viewModel = viewModel)
            } else {
                // Search field — only while unfolded, so the list starts higher up the rest of the
                // time. Folding it back clears the query (see the ViewModel), so what is on screen
                // always accounts for how short the list is.
                if (searchExpanded) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester),
                    )
                }

                // Sort chips and the tag filter share one row.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DiaryPickerSort.entries.forEach { s ->
                        FilterChip(
                            selected = sort == s,
                            onClick = { viewModel.onSortChange(s) },
                            label = { Text(s.label()) },
                        )
                    }
                    if (allTags.isNotEmpty()) {
                        TagFilterDropdown(
                            tags = allTags,
                            selectedTagId = selectedTagId,
                            onTagSelected = viewModel::onTagSelected,
                            // Gives way before the sort chips do, and only as far as it needs to —
                            // a long tag name ellipsizes instead of pushing the row off screen.
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }

                // Unified picker list. Takes the whole remaining height rather than only as much as
                // its rows need, so the scrollable area always runs down to the bottom of the frame
                // — which, with the keyboard open, is the top of the keyboard.
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(pickerItems, key = { "${it.sourceType}:${it.id}" }) { item ->
                        ItemRow(
                            item = item,
                            isExpanded = expandedItem?.id == item.id && expandedItem?.sourceType == item.sourceType,
                            showTypeLabel = mode == DiaryPickerMode.ALL,
                            tagOrder = tagOrder,
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

/**
 * The tag filter as one chip that opens a menu, rather than a scrolling row of chips. Single
 * selection: "Alle" clears it, any other entry replaces what was picked.
 *
 * A plain [DropdownMenu] anchored on the chip, not an `ExposedDropdownMenuBox` — that one expects a
 * text field as its anchor, and this row has no room for one.
 */
@Composable
private fun TagFilterDropdown(
    tags: List<Tag>,
    selectedTagId: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTag = tags.firstOrNull { it.id == selectedTagId }
    val tagOrder = tags.map { it.id }

    Box(modifier = modifier) {
        FilterChip(
            selected = selectedTagId != null,
            onClick = { expanded = true },
            label = {
                Text(
                    selectedTag?.name ?: "Tags",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = selectedTag?.let { tag ->
                { TagDot(color = tag.displayColor(tagOrder.indexOf(tag.id).coerceAtLeast(0)), size = 12) }
            },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Tag wählen") },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle") },
                onClick = {
                    onTagSelected(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selectedTagId == null) Icon(Icons.Filled.Check, contentDescription = null)
                },
            )
            tags.forEachIndexed { index, tag ->
                DropdownMenuItem(
                    text = { Text(tag.name) },
                    onClick = {
                        onTagSelected(tag.id)
                        expanded = false
                    },
                    leadingIcon = { TagDot(color = tag.displayColor(index), size = 12) },
                    trailingIcon = {
                        if (selectedTagId == tag.id) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: DiaryPickerItem,
    isExpanded: Boolean,
    showTypeLabel: Boolean,
    tagOrder: List<String>,
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
                // Names *and* dots here: the row has the width for both, and it is where the user
                // learns which colour belongs to which tag before meeting the dots alone in the
                // Tagebuch.
                if (item.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TagDots(tags = item.tags, tagOrder = tagOrder)
                        Text(
                            item.tags.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
    units: List<com.example.mytracker.nutrition.food.FoodUnit>,
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
                        trailingContent = { AddButton(onConfirm) },
                    )
                }
                is DiaryPickerItem.Recipe -> {
                    // Same row, same place as for a Lebensmittel — the button must not move just
                    // because the amount is called "Portionen" here.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = onAmountChange,
                            label = { Text("Portionen") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { }),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        AddButton(onConfirm)
                    }
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
        }
    }
}

/**
 * Confirms the amount, from the amount field's own line. Filled rather than outlined: reduced to a
 * bare "+" it is still the panel's main action, and an outlined one would read as a peer of the
 * field's own outline. The content description is the only name it has left, so it carries the word
 * the button used to show.
 */
@Composable
private fun AddButton(onConfirm: () -> Unit) {
    FilledIconButton(onClick = dismissingKeyboard(onConfirm)) {
        Icon(Icons.Filled.Add, contentDescription = "Hinzufügen")
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
    /** Keeps the label under the icon permanently, for a button whose icon changes as it is tapped. */
    alwaysShowLabel: Boolean = false,
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

        if (alwaysShowLabel || showTooltip.value) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    // A permanent label gets a fixed slot, so the buttons to the right of a cycling
                    // one do not shift as its label changes length from tap to tap.
                    .then(if (alwaysShowLabel) Modifier.width(88.dp) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showTooltip.value) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                showTooltip.value = false
            }
        }
    }
}

private fun getModeIcon(mode: DiaryPickerMode) = when (mode) {
    DiaryPickerMode.ALL -> Icons.Filled.AllInclusive
    DiaryPickerMode.FOOD -> Icons.Filled.Flatware
    DiaryPickerMode.RECIPE -> Icons.Filled.Restaurant
    DiaryPickerMode.QUICK -> Icons.Filled.Bolt
}

private fun getMealTypeIcon(type: MealType) = when (type) {
    MealType.BREAKFAST -> Icons.Filled.Brightness5
    MealType.LUNCH -> Icons.Filled.Brightness7
    MealType.DINNER -> Icons.Filled.Brightness1
    MealType.SNACK -> Icons.Filled.Cake
}
