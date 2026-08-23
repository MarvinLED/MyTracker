package com.example.mytracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Met and missed, as the two hues the app already reserves for them: the chart palette's grün and
 * rot slots, stepped for the surface being drawn on. Shared so "geschafft" looks the same wherever
 * it is claimed — the Ziele screen's goal rows and the Kraftübung's volume target.
 *
 * Nothing may be told apart by colour alone: every place using these also carries its numbers and
 * an icon whose shape differs as much as its colour does.
 */
private val MetGreenDark = Color(0xFF0E9A2B)
private val MetGreenLight = Color(0xFF008300)
private val MissedRedDark = Color(0xFFE66767)
private val MissedRedLight = Color(0xFFE34948)
private val CautionAmberDark = Color(0xFFE3B341)
private val CautionAmberLight = Color(0xFF8A6100)

@Composable
@ReadOnlyComposable
fun statusColor(isMet: Boolean): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when {
        isMet && dark -> MetGreenDark
        isMet -> MetGreenLight
        dark -> MissedRedDark
        else -> MissedRedLight
    }
}

/**
 * The step between met and missed: a top set below last time's is worth noticing, but it is not the
 * red of a missed goal — an easy day inside a plan is supposed to happen. Amber says "unter dem
 * letzten Mal" without calling it a failure.
 *
 * Darkened well past the chart's yellow in light mode: amber on white is the one hue that reliably
 * fails to be readable at body-text size.
 */
@Composable
@ReadOnlyComposable
fun cautionColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) CautionAmberDark else CautionAmberLight
