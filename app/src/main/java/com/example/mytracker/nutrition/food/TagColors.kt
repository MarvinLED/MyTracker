package com.example.mytracker.nutrition.food

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mytracker.fluid.fluidPalette

/**
 * The colour a tag's dot is drawn in: the user's pick if they made one, otherwise the palette slot
 * for [index]. Same rule and same palette as
 * [FluidType.chartColor][com.example.mytracker.fluid.chartColor] — one set of validated
 * hues across the app.
 *
 * [index] must come from the tag's stable library order (alphabetical, as [TagDao.observeAll]
 * returns them), never from its position in whatever subset is on screen, or the same tag would
 * change colour between two lists.
 */
@Composable
@ReadOnlyComposable
fun Tag.displayColor(index: Int): Color {
    val palette = fluidPalette()
    return colorArgb?.let { Color(it) } ?: palette[index.mod(palette.size)]
}

/** How many dots fit before the row starts crowding out the text it sits next to. */
private const val MaxTagDots = 3

/**
 * A tag's colour as a small dot. Always outlined: the palette's brightest and dimmest slots would
 * otherwise dissolve into a light or dark surface respectively.
 */
@Composable
fun TagDot(color: Color, size: Int = 8, modifier: Modifier = Modifier) {
    Surface(
        color = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.size(size.dp),
    ) {}
}

/**
 * Up to [MaxTagDots] coloured dots for [tags], then a "+n" for the rest. Meant to sit in front of an
 * entry's name in a one-line row, where spelling the tag names out would push the name off screen.
 *
 * [tagOrder] is the full library order and supplies each tag's palette index, so a tag keeps its
 * colour no matter which entry it appears on.
 */
@Composable
fun TagDots(tags: List<Tag>, tagOrder: List<String>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.take(MaxTagDots).forEach { tag ->
            TagDot(color = tag.displayColor(tagOrder.indexOf(tag.id).coerceAtLeast(0)))
        }
        if (tags.size > MaxTagDots) {
            Text(
                "+${tags.size - MaxTagDots}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
