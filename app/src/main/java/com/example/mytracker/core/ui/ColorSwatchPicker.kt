package com.example.prokject2_tracker.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.fluid.contrastingInk
import com.example.prokject2_tracker.fluid.fluidColorChoices

/**
 * The app's chart palette as tappable swatches, plus an "Automatisch" chip (null) that leaves the
 * colour to the caller's own fallback — normally the palette slot matching the item's position in
 * its library.
 *
 * Shared by the Getränkearten and the Tags: both pick from the same eight validated hues, so the
 * same colour means the same thing wherever it shows up.
 *
 * @param automaticLabel what null is called in this context.
 */
@Composable
fun ColorSwatchPicker(
    selectedArgb: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    automaticLabel: String = "Automatisch",
) {
    val choices = fluidColorChoices()
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedArgb == null,
            onClick = { onSelect(null) },
            label = { Text(automaticLabel) },
        )
        choices.forEach { argb ->
            val selected = selectedArgb == argb
            val swatch = Color(argb)
            Surface(
                color = swatch,
                shape = CircleShape,
                border = BorderStroke(
                    if (selected) 3.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.size(36.dp).clickable { onSelect(argb) },
            ) {
                if (selected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Ausgewählt",
                            tint = swatch.contrastingInk(),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
