package com.example.mytracker.nutrition.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.ui.ConfirmDeleteDialog
import com.example.mytracker.core.ui.rememberTopPinnedListState
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.nutrition.diary.DiaryPickerItem
import com.example.mytracker.nutrition.food.TagDots

/**
 * The Rezepte tab of the Bibliothek. Same rule as the Lebensmittel tab: the row logs, the two
 * buttons edit and delete.
 */
@Composable
fun RecipeListContent(
    items: List<DiaryPickerItem.Recipe>,
    /** The library order behind every tag colour — see `Tag.displayColor`. */
    tagOrder: List<String>,
    onLogRecipe: (RecipeWithNutrition) -> Unit,
    onEditRecipe: (String) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    /** Whatever makes this a new list — sort, tag filter, query. See [rememberTopPinnedListState]. */
    listResetKey: Any?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberTopPinnedListState(items, listResetKey)
    var pendingDelete by remember { mutableStateOf<Recipe?>(null) }

    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Keine Rezepte gefunden.")
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.recipe.recipe.id }) { item ->
            val withNutrition = item.recipe
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLogRecipe(withNutrition) },
                    ) {
                        Text(withNutrition.recipe.name)
                        Text(
                            "${withNutrition.perServing.kcal.formatCompact()} kcal / Portion " +
                                "(${withNutrition.recipe.servings.formatCompact()} Portionen)",
                        )
                        if (withNutrition.fluids.isNotEmpty()) {
                            Text(
                                withNutrition.fluids.joinToString(" · ") {
                                    "${it.name} ${it.totalMl.formatCompact()} ml"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    IconButton(onClick = { onEditRecipe(withNutrition.recipe.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { pendingDelete = withNutrition.recipe }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }

    pendingDelete?.let { recipe ->
        ConfirmDeleteDialog(
            title = "\"${recipe.name}\" löschen?",
            // The ingredient list goes with the recipe (CASCADE on recipe_ingredients); the
            // Lebensmittel it was built from do not.
            text = "Das Rezept und seine Zutatenliste werden entfernt. Die einzelnen Lebensmittel " +
                "bleiben, und bereits eingetragene Mahlzeiten behalten ihre Werte.",
            onConfirm = { onDeleteRecipe(recipe) },
            onDismiss = { pendingDelete = null },
        )
    }
}
