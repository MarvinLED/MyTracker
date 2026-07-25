package com.example.prokject2_tracker.fluid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/**
 * Categorical palette for the app's charts: eight hues in a fixed order, stepped once for each
 * surface. The two lists are the *same* hues re-stepped for the dark background, not two different
 * palettes.
 *
 * The dark column is validated as a set against [com.example.prokject2_tracker.ui.theme
 * .DarkBlueSurfaceContainer] — the surface chart cards sit on: all eight inside the dark lightness
 * band, all ≥ 3:1 contrast, worst adjacent CVD ΔE 8.4 (protan) and worst adjacent normal-vision
 * ΔE 19.3. Re-run that validation if either the palette or the surface ladder changes; the green
 * slot in particular was re-stepped from `#008300` when the surfaces were lightened, because at the
 * old step it fell to 2.88:1 against the new card surface.
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
    Color(0xFF0E9A2B), // grün
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

/**
 * Extra hues offered in the colour picker but never handed out automatically, because a drink's real
 * colour is sometimes the point (Kaffee/Kakao braun, Milch weiß). They stay out of the eight
 * auto-assigned slots deliberately: brown is the dimmest fill in the set and white the brightest, so
 * neither is a good default neighbour for the palette hues — they only appear when the user picks
 * them. Swatches and legend dots carry an outline so a fill close to the surface still has an edge.
 */
val FluidExtraColorsDark: List<Color> = listOf(
    Color(0xFF9C6644), // braun
    Color(0xFFF5F5F5), // weiß
)

val FluidExtraColorsLight: List<Color> = listOf(
    Color(0xFF7A4A28),
    Color(0xFFFFFFFF),
)

/** The palette matching the current theme's chart surface. */
@Composable
@ReadOnlyComposable
fun fluidPalette(): List<Color> =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) FluidPaletteDark else FluidPaletteLight

/**
 * The swatches offered in the colour picker: the eight palette slots for the surface the user is
 * looking at, followed by the deliberate-pick-only extras.
 */
@Composable
@ReadOnlyComposable
fun fluidColorChoices(): List<Int> {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val extras = if (dark) FluidExtraColorsDark else FluidExtraColorsLight
    return (fluidPalette() + extras).map { it.toArgb() }
}

/**
 * Ink that stays readable *on* [this] fill — needed since the picker's white and brown swatches sit
 * at opposite ends of the luminance range and a single fixed tint would vanish on one of them.
 */
fun Color.contrastingInk(): Color = if (luminance() > 0.45f) Color.Black else Color.White

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
