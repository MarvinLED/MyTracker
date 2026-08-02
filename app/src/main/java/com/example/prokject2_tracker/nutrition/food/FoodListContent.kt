package com.example.prokject2_tracker.nutrition.food

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.formatCompact

@Composable
fun FoodListContent(
    onAddFood: () -> Unit,
    onEditFood: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodListViewModel = hiltViewModel(),
) {
    val foods by viewModel.foods.collectAsState()
    val query by viewModel.query.collectAsState()
    val tagsByFoodId by viewModel.tagsByFoodId.collectAsState()
    var blockedDeleteFood by remember { mutableStateOf<FoodItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Suche") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            if (foods.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Lebensmittel angelegt.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(foods, key = { it.id }) { food ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onEditFood(food.id) },
                                ) {
                                    Text(food.name)
                                    val unit = if (food.baseUnit == BaseUnit.G) "100 g" else "100 ml"
                                    val brandSuffix = food.brand?.let { " · $it" }.orEmpty()
                                    Text("${food.kcalPer100.formatCompact()} kcal / $unit$brandSuffix")
                                    food.formatPrice()?.let { price ->
                                        Text(
                                            price,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    val tags = tagsByFoodId[food.id].orEmpty()
                                    if (tags.isNotEmpty()) {
                                        Text(
                                            tags.joinToString(" · ") { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    viewModel.deleteIfUnused(food) { blockedDeleteFood = food }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddFood,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Lebensmittel hinzufügen")
        }
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
