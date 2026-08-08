package com.example.prokject2_tracker.nutrition.library

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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.prokject2_tracker.nutrition.food.FoodListContent
import com.example.prokject2_tracker.nutrition.food.TagListContent
import com.example.prokject2_tracker.nutrition.recipe.RecipeListContent
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.topAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddFood: () -> Unit,
    onEditFood: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Lebensmittel", "Rezepte", "Tags")

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
                0 -> FoodListContent(onAddFood = onAddFood, onEditFood = onEditFood, modifier = Modifier.weight(1f))
                1 -> RecipeListContent(onAddRecipe = onAddRecipe, onEditRecipe = onEditRecipe, modifier = Modifier.weight(1f))
                2 -> TagListContent(modifier = Modifier.weight(1f))
            }
        }
    }
}
