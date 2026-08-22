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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.util.formatCompact

@Composable
fun RecipeListContent(
    onAddRecipe: () -> Unit,
    onEditRecipe: (String) -> Unit,
    /** Opens the "ins Tagebuch" dialog for this recipe — see LibraryQuickLogViewModel. */
    onLogToDiary: (RecipeWithNutrition) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeListViewModel = hiltViewModel(),
) {
    val recipes by viewModel.recipes.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (recipes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Noch keine Rezepte angelegt.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recipes, key = { it.recipe.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onEditRecipe(item.recipe.id) },
                            ) {
                                Text(item.recipe.name)
                                Text(
                                    "${item.perServing.kcal.formatCompact()} kcal / Portion " +
                                        "(${item.recipe.servings.formatCompact()} Portionen)",
                                )
                                if (item.fluids.isNotEmpty()) {
                                    Text(
                                        item.fluids.joinToString(" · ") {
                                            "${it.name} ${it.totalMl.formatCompact()} ml"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (item.tags.isNotEmpty()) {
                                    Text(
                                        item.tags.joinToString(" · ") { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { onLogToDiary(item) }) {
                                Icon(Icons.Filled.PostAdd, contentDescription = "Ins Tagebuch")
                            }
                            IconButton(onClick = { viewModel.delete(item.recipe) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddRecipe,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Rezept hinzufügen")
        }
    }
}
