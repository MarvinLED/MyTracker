package com.example.prokject2_tracker.fluid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * Categorical palette for the Flüssigkeiten pie charts: eight hues in a fixed order, stepped once
 * for each surface. The two lists are the *same* hues re-stepped for the dark background, not two
 * different palettes — every dark step clears 3:1 contrast on this app's navy chart surface, and
 * adjacent pairs stay separable for colour-vision deficiencies.
 *
 * The order is fixed on purpose: a type keeps its slot no matter which other types happen to be
 * drunk on a given day, so the same drink is never re-coloured between two days' charts.
 */
val FluidPaletteDark: List<Color> = listOf(
    Color(0xFF3987E5), // blau
    Color(0xFFD95926), // orange
    Color(0xFF199E70), // aqua
    Color(0xFFC98500), // gelb
    Color(0xFFD55181), // magenta
    Color(0xFF008300), // grün
    Color(0xFF9085E9), // violett
    Color(0xFFE66767), // rot
)

val FluidPaletteLight: List<Color> = listOf(
    Color(0xFF2A78D6),
    Color(0xFFEB6834),
    Color(0xFF1BAF7A),
    Color(0xFFEDA100),
    Color(0xFFE87BA4),
    Color(0xFF008300),
    Color(0xFF4A3AA7),
    Color(0xFFE34948),
)

/** The palette matching the current theme's chart surface. */
@Composable
@ReadOnlyComposable
fun fluidPalette(): List<Color> =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) FluidPaletteDark else FluidPaletteLight

/** The swatches offered in the colour picker — the palette for the surface the user is looking at. */
@Composable
@ReadOnlyComposable
fun fluidColorChoices(): List<Int> = fluidPalette().map { it.toArgb() }

/**
 * The colour a type is drawn with: the user's pick if they made one, otherwise the palette slot for
 * its position in the library. [index] must come from the type's stable library order, never from
 * its rank in the current chart.
 */
@Composable
@ReadOnlyComposable
fun FluidType.chartColor(index: Int): Color {
    val palette = fluidPalette()
    return colorArgb?.let { Color(it) } ?: palette[index.mod(palette.size)]
}
