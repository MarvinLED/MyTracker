package com.example.prokject2_tracker.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * One accent hue per area of the app, so a screen is recognisable before its title is read.
 *
 * The hues are slots out of the same validated chart palette as `fluid/FluidChartColors.kt` — not a
 * second, uncoordinated set. The four bottom-nav accents are the ones that appear side by side, and
 * they validate as a categorical set against the nav's surface (worst adjacent CVD ΔE 17.3, all
 * ≥ 3:1 contrast). Nav items and headings always carry their label too, so identity is never on
 * colour alone.
 *
 * [ANALYSE], [GOALS] and [LIBRARY] deliberately stay on the theme primary: they are cross-cutting
 * views over the other areas' data, so claiming a hue of their own would imply a peer relationship
 * that isn't there — and red, the only slot left, reads as an error state.
 */
enum class AppDomain(private val accent: Color?) {
    DIARY(Color(0xFF199E70)), // aqua
    FLUID(Color(0xFF3987E5)), // blau
    FITNESS(Color(0xFFD95926)), // orange
    WEIGHT(Color(0xFFD55181)), // magenta
    HABIT(Color(0xFFC98500)), // gelb
    ANALYSE(null),
    GOALS(null),
    LIBRARY(null),
    ;

    /** The area's hue, falling back to the theme primary for the cross-cutting views. */
    @Composable
    @ReadOnlyComposable
    fun accent(): Color = accent ?: MaterialTheme.colorScheme.primary

    /** Ink that stays readable on top of [accent] — the accents span a wide lightness range. */
    @Composable
    @ReadOnlyComposable
    fun onAccent(): Color = if (accent().luminance() > 0.45f) Color.Black else Color.White

    /**
     * The accent laid over a surface at low alpha: enough to tint a large area like a top bar
     * without turning it into a saturated block that fights the content on it.
     */
    @Composable
    @ReadOnlyComposable
    fun accentSurface(alpha: Float = 0.22f): Color =
        accent().copy(alpha = alpha).compositeOver(MaterialTheme.colorScheme.surface)
}

/** Tints a top-level screen's app bar with its area's accent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDomain.topAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = accentSurface(),
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)
