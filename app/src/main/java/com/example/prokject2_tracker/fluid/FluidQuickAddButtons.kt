package com.example.prokject2_tracker.fluid

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.formatCompact

/** One button ready to draw: what it logs, plus the name and colour of the drink type behind it. */
data class FluidQuickAddItem(
    val quickAdd: FluidQuickAdd,
    val typeName: String,
    val color: Color,
)

/**
 * The configured buttons paired with their drink types. Buttons whose type has been deleted are
 * dropped rather than drawn colourless: the cascade normally removes them, and a button that cannot
 * name what it logs is worse than a missing one.
 *
 * The colour comes from the type's position in the library — the same rule the charts use, so a
 * button and its slice in the bar above it are the same colour.
 */
@Composable
fun fluidQuickAddItems(quickAdds: List<FluidQuickAdd>, types: List<FluidType>): List<FluidQuickAddItem> =
    quickAdds.mapNotNull { quickAdd ->
        val index = types.indexOfFirst { it.id == quickAdd.fluidTypeId }
        val type = types.getOrNull(index) ?: return@mapNotNull null
        FluidQuickAddItem(quickAdd = quickAdd, typeName = type.name, color = type.chartColor(index))
    }

/**
 * The Schnellauswahl: up to two rows of one-tap buttons with the undo for the last of them on the
 * right. Every button logs immediately and without a dialog — that is the whole point — so the undo
 * sits next to them rather than somewhere else on the screen.
 */
@Composable
fun FluidQuickAddArea(
    items: List<FluidQuickAddItem>,
    onQuickAdd: (FluidQuickAdd) -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isEmpty()) {
                Text(
                    "Keine Schnellauswahl angelegt — über das Zahnrad einrichten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            fluidQuickAddRows(items).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        FluidQuickAddButton(item = item, onClick = { onQuickAdd(item.quickAdd) })
                    }
                }
            }
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.Filled.Undo, contentDescription = "Letzte Schnellauswahl rückgängig machen")
        }
        // Quieter than the buttons it configures, but on the same row: with nothing set up yet the
        // area would otherwise be a dead end.
        IconButton(onClick = onManage) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Schnellauswahl verwalten",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A single shortcut. The fill is the drink's own colour and the symbol is drawn in whatever ink
 * stays readable on it — the palette runs from near-white to brown, so a fixed tint would vanish on
 * one end of it.
 */
@Composable
fun FluidQuickAddButton(
    item: FluidQuickAddItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = item.color.contrastingInk()
    val description = "${item.typeName}, ${item.quickAdd.amountMl.formatCompact()} ml hinzufügen"
    Surface(
        color = item.color,
        shape = CircleShape,
        // The white swatch is all but invisible on a light card without an edge of its own.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .size(44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (item.quickAdd.symbol) {
                FluidQuickAddSymbol.GLASS -> Icon(
                    Icons.Filled.LocalDrink,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(22.dp),
                )
                FluidQuickAddSymbol.BOTTLE -> Icon(
                    Icons.Filled.Liquor,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(22.dp),
                )
                // The one symbol that is its own amount, so it is written rather than drawn.
                FluidQuickAddSymbol.ML_100 -> Text(
                    "100",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                )
            }
        }
    }
}
