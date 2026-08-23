package com.example.mytracker.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The app's filter chip: on means filled, off means an outline and nothing else.
 *
 * Material's default separates the two states by a container tint a few percent away from the
 * surface it sits on. On a card, in daylight, that is not a difference anyone can see — and on a
 * screen with eight of them in a row, "which ones am I actually looking at" is the only question the
 * chips exist to answer. The states here differ in fill, in ink, in border and by a check mark, so
 * the distinction survives glare, a colour-blind reader and a greyscale screenshot alike.
 *
 * [color] is the chart colour of the series the chip switches, where there is one: it then fills the
 * selected chip, which ties it to its line, and rings the unselected one, which keeps that tie while
 * it is switched off. Null — for a mode or range chip that picks between alternatives rather than
 * switching a line — falls back to the theme's primary and drops the ring, since there is no series
 * for it to name.
 */
@Composable
fun AppFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val fill = color ?: MaterialTheme.colorScheme.primary
    // Black or white, whichever the fill can actually carry. The chart palette runs from pale
    // yellows to deep blues, so a fixed onPrimary would be unreadable on half of it.
    val ink = if (fill.luminance() > 0.5f) Color.Black else Color.White

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = when {
            selected -> {
                {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            }
            color != null -> {
                { SeriesDot(color) }
            }
            else -> null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = fill,
            selectedLabelColor = ink,
        ),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    )
}

/** The unselected chip's colour ring — hollow, so it reads as "off" rather than as a filled dot. */
@Composable
private fun SeriesDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color.copy(alpha = 0.2f), CircleShape)
            .border(2.dp, color, CircleShape),
    )
}
