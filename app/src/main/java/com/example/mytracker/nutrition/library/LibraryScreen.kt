package com.example.mytracker.nutrition.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.nutrition.food.FoodListContent
import com.example.mytracker.nutrition.food.TagListContent
import com.example.mytracker.nutrition.recipe.RecipeListContent
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddFood: () -> Unit,
    onEditFood: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    quickLogViewModel: LibraryQuickLogViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Lebensmittel", "Rezepte", "Tags")

    val quickLogTarget by quickLogViewModel.target.collectAsState()
    val quickLogMealType by quickLogViewModel.mealType.collectAsState()
    val quickLogAmount by quickLogViewModel.amountText.collectAsState()
    val quickLogUnits by quickLogViewModel.units.collectAsState()
    val quickLogUnitId by quickLogViewModel.selectedUnitId.collectAsState()
    val quickLogCanConfirm by quickLogViewModel.canConfirm.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                // No Export/Import here any more: backing up is its own drawer destination now, and
                // it covers all three categories rather than only what this screen happens to show.
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
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> FoodListContent(
                    onAddFood = onAddFood,
                    onEditFood = onEditFood,
                    onLogToDiary = quickLogViewModel::startFood,
                    modifier = Modifier.weight(1f),
                )
                1 -> RecipeListContent(
                    onAddRecipe = onAddRecipe,
                    onEditRecipe = onEditRecipe,
                    onLogToDiary = quickLogViewModel::startRecipe,
                    modifier = Modifier.weight(1f),
                )
                2 -> TagListContent(modifier = Modifier.weight(1f))
            }
        }
    }

    quickLogTarget?.let { target ->
        LibraryQuickLogDialog(
            target = target,
            mealType = quickLogMealType,
            onMealTypeChange = quickLogViewModel::onMealTypeChange,
            amountText = quickLogAmount,
            onAmountChange = quickLogViewModel::onAmountChange,
            units = quickLogUnits,
            selectedUnitId = quickLogUnitId,
            onUnitSelected = quickLogViewModel::selectUnit,
            canConfirm = quickLogCanConfirm,
            onConfirm = quickLogViewModel::confirm,
            onDismiss = quickLogViewModel::dismiss,
        )
    }
}
