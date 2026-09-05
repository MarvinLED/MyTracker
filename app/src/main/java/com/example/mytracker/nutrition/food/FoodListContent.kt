package com.example.mytracker.nutrition.food

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

/**
 * The Lebensmittel tab of the Bibliothek.
 *
 * Tapping the row itself logs the food — that is what one comes here to do most of the time. The two
 * buttons are the rarer jobs: correcting the food, and getting rid of it.
 */
@Composable
fun FoodListContent(
    items: List<DiaryPickerItem.Food>,
    /** The library order behind every tag colour — see [displayColor]. */
    tagOrder: List<String>,
    onLogFood: (FoodItem) -> Unit,
    onEditFood: (String) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    /** Whatever makes this a new list — sort, tag filter, query. See [rememberTopPinnedListState]. */
    listResetKey: Any?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberTopPinnedListState(items, listResetKey)
    var pendingDelete by remember { mutableStateOf<FoodItem?>(null) }

    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Keine Lebensmittel gefunden.")
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.food.id }) { item ->
            val food = item.food
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLogFood(food) },
                    ) {
                        Text(food.name)
                        // A food without a weight states its figure per portion: "/ 100 g"
                        // would be a comparison value it does not have.
                        val unit = food.portionUnitName
                            ?: if (food.baseUnit == BaseUnit.G) "100 g" else "100 ml"
                        val brandSuffix = food.brand?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                        Text("${food.kcalPer100.formatCompact()} kcal / $unit$brandSuffix")
                        food.formatPrice()?.let { price ->
                            Text(
                                price,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // Names *and* dots: this is where the user learns which colour belongs to
                        // which tag before meeting the dots alone in the Tagebuch.
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
                    IconButton(onClick = { onEditFood(food.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { pendingDelete = food }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }

    pendingDelete?.let { food ->
        ConfirmDeleteDialog(
            title = "\"${food.name}\" löschen?",
            // Worth saying, because it is the fear that makes people hesitate: the diary snapshots
            // its own values (see DiaryEntry), so past meals are not touched by this.
            text = "Das Lebensmittel verschwindet aus der Bibliothek. " +
                "Bereits eingetragene Mahlzeiten behalten ihre Werte.",
            onConfirm = { onDeleteFood(food) },
            onDismiss = { pendingDelete = null },
        )
    }
}
