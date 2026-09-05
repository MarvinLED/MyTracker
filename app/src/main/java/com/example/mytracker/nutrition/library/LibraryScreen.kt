package com.example.mytracker.nutrition.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.nutrition.diary.MealType
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.FoodListContent
import com.example.mytracker.nutrition.food.TagListContent
import com.example.mytracker.nutrition.recipe.RecipeListContent
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

private const val TAB_FOODS = 0
private const val TAB_RECIPES = 1
private const val TAB_TAGS = 2
private const val TAB_QUICK = 3

/**
 * The Bibliothek — and, since it is the same list of the same things, the one place entries are
 * added from as well.
 *
 * Two ways in, one screen: from the drawer it has no day of its own and writes to today; from a
 * meal's "+" in the Tagebuch it is handed [epochDay] and [mealType] and writes there instead, with
 * [onBack] in place of the drawer button. Everything else about it is the same either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddFood: () -> Unit,
    onEditFood: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    epochDay: Long? = null,
    mealType: MealType? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    quickLogViewModel: LibraryQuickLogViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_FOODS) }
    val tabs = listOf("Lebensmittel", "Rezepte", "Tags", "Schnell")

    val query by viewModel.query.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val tagOrder = remember(allTags) { allTags.map { it.id } }
    val foodItems by viewModel.foodItems.collectAsState()
    val recipeItems by viewModel.recipeItems.collectAsState()

    val quickLogTarget by quickLogViewModel.target.collectAsState()
    val contextDay by quickLogViewModel.contextDay.collectAsState()
    val quickLogMealType by quickLogViewModel.mealType.collectAsState()
    val quickLogAmount by quickLogViewModel.amountText.collectAsState()
    val quickLogUnits by quickLogViewModel.units.collectAsState()
    val quickLogUnitId by quickLogViewModel.selectedUnitId.collectAsState()
    val quickLogPreview by quickLogViewModel.preview.collectAsState()
    val quickLogCanConfirm by quickLogViewModel.canConfirm.collectAsState()
    val quickEntry by quickLogViewModel.quick.collectAsState()
    val quickEntryMeal by quickLogViewModel.quickMealType.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var blockedDeleteFood by remember { mutableStateOf<FoodItem?>(null) }

    // The day and meal are the route's, not the ViewModels' own — hand them over whenever they
    // change, and let both sides fall back to "today, meal by the clock" when there are none.
    LaunchedEffect(epochDay, mealType) {
        quickLogViewModel.setContext(epochDay, mealType)
        // The Zuletzt sort groups by the meal one is adding to, so it follows the same choice.
        mealType?.let(viewModel::setMealContext)
    }

    LaunchedEffect(Unit) {
        quickLogViewModel.logged.collect { name ->
            snackbarHostState.showSnackbar("\"$name\" ins Tagebuch eingetragen")
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.LIBRARY.topAppBarColors(),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menü")
                        }
                    }
                },
                // No Export/Import here any more: backing up is its own drawer destination now, and
                // it covers every category rather than only what this screen happens to show.
                //
                // The title stays "Bibliothek" on both ways in. Coming from a "+" one is still in
                // the Bibliothek — which day is being written to is the dialog's job to say, and it
                // does, on every entry.
                title = { Text("Bibliothek") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            // One search row for both lists — the same query, sort and tag filter apply to each, so
            // switching tabs must not silently change what is being looked at.
            //
            // "+" beside the search field rather than as a FAB in the bottom corner: with the
            // keyboard open for the search, a bottom-corner button is behind it.
            if (selectedTab == TAB_FOODS || selectedTab == TAB_RECIPES) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        label = { Text("Suche") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    SortButton(
                        sort = sort,
                        onCycle = viewModel::cycleSort,
                        onSelect = viewModel::onSortChange,
                    )
                    TagFilterButton(
                        tags = allTags,
                        selectedTagId = selectedTagId,
                        onCycle = viewModel::cycleTag,
                        onSelect = viewModel::onTagSelected,
                    )
                    FilledIconButton(
                        onClick = if (selectedTab == TAB_FOODS) onAddFood else onAddRecipe,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = if (selectedTab == TAB_FOODS) {
                                "Lebensmittel anlegen"
                            } else {
                                "Rezept anlegen"
                            },
                        )
                    }
                }
            }

            // Asking for a different order or filter means asking to look at the list from its
            // beginning again.
            val listResetKey = remember(sort, selectedTagId, query) { Triple(sort, selectedTagId, query) }

            when (selectedTab) {
                TAB_FOODS -> FoodListContent(
                    items = foodItems,
                    tagOrder = tagOrder,
                    onLogFood = quickLogViewModel::startFood,
                    onEditFood = onEditFood,
                    onDeleteFood = { food ->
                        viewModel.deleteFoodIfUnused(food) { blockedDeleteFood = food }
                    },
                    listResetKey = listResetKey,
                    modifier = Modifier.weight(1f),
                )
                TAB_RECIPES -> RecipeListContent(
                    items = recipeItems,
                    tagOrder = tagOrder,
                    onLogRecipe = quickLogViewModel::startRecipe,
                    onEditRecipe = onEditRecipe,
                    onDeleteRecipe = viewModel::deleteRecipe,
                    listResetKey = listResetKey,
                    modifier = Modifier.weight(1f),
                )
                TAB_TAGS -> TagListContent(modifier = Modifier.weight(1f))
                TAB_QUICK -> QuickEntryContent(
                    state = quickEntry,
                    onNameChange = quickLogViewModel::onQuickNameChange,
                    onKcalChange = quickLogViewModel::onQuickKcalChange,
                    onProteinChange = quickLogViewModel::onQuickProteinChange,
                    onCarbsChange = quickLogViewModel::onQuickCarbsChange,
                    onFatChange = quickLogViewModel::onQuickFatChange,
                    contextDay = contextDay,
                    mealType = quickEntryMeal,
                    onMealTypeChange = quickLogViewModel::onQuickMealTypeChange,
                    onConfirm = quickLogViewModel::confirmQuick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    quickLogTarget?.let { target ->
        LibraryQuickLogDialog(
            target = target,
            contextDay = contextDay,
            mealType = quickLogMealType,
            onMealTypeChange = quickLogViewModel::onMealTypeChange,
            amountText = quickLogAmount,
            onAmountChange = quickLogViewModel::onAmountChange,
            units = quickLogUnits,
            selectedUnitId = quickLogUnitId,
            onUnitSelected = quickLogViewModel::selectUnit,
            preview = quickLogPreview,
            canConfirm = quickLogCanConfirm,
            onConfirm = quickLogViewModel::confirm,
            onDismiss = quickLogViewModel::dismiss,
        )
    }

    blockedDeleteFood?.let { food ->
        AlertDialog(
            onDismissRequest = { blockedDeleteFood = null },
            confirmButton = { TextButton(onClick = { blockedDeleteFood = null }) { Text("OK") } },
            title = { Text("Kann nicht gelöscht werden") },
            text = { Text("\"${food.name}\" wird in mindestens einem Rezept verwendet.") },
        )
    }
}
