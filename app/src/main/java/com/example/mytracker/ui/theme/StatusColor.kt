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
