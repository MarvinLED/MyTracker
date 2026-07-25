package com.example.prokject2_tracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dark scheme: a blue surface *ladder* rather than near-black navy, so cards and sheets read as
 * distinct layers instead of merging into the background.
 *
 * The steps are deliberate, not a gradient: every ink below clears 4.5:1 on all of them (the
 * lightest, [DarkBlueSurfaceHighest], is the tight case at 9.3:1), and the chart palette in
 * `fluid/FluidChartColors.kt` is validated against [DarkBlueSurfaceContainer] — the step the chart
 * cards sit on. Changing these values means re-running that validation.
 */
val DarkBlueBackground = Color(0xFF101A30)
val DarkBlueSurface = Color(0xFF14203C)
val DarkBlueSurfaceLowest = Color(0xFF0E1830)
val DarkBlueSurfaceLow = Color(0xFF17243F)
val DarkBlueSurfaceContainer = Color(0xFF1C2A48)
val DarkBlueSurfaceHigh = Color(0xFF223252)
val DarkBlueSurfaceHighest = Color(0xFF293B5E)
val DarkBlueSurfaceVariant = Color(0xFF2A3B5E)

val DarkBlueOnBackground = Color(0xFFE6EAF5)
val DarkBlueOnSurfaceVariant = Color(0xFFBFC9DE)
val DarkBlueOutline = Color(0xFF93A0BC)

val BlueDark80 = Color(0xFF9DC2FF)
val BlueGreyDark80 = Color(0xFFB9C6E3)
val CyanDark80 = Color(0xFF8FCEE3)

val DarkBluePrimaryContainer = Color(0xFF1E4C82)
val DarkBlueOnPrimaryContainer = Color(0xFFD3E3FF)
val DarkBlueSecondaryContainer = Color(0xFF2C3A54)
val DarkBlueOnSecondaryContainer = Color(0xFFD6E2FF)
val DarkBlueTertiaryContainer = Color(0xFF1D4C5C)
val DarkBlueOnTertiaryContainer = Color(0xFFC5E8F5)

// Light scheme: same blue family, kept light so it's still usable in daylight.
val Blue40 = Color(0xFF2F5DA8)
val BlueGrey40 = Color(0xFF56637A)
val Cyan40 = Color(0xFF3C6478)

val LightBlueBackground = Color(0xFFF5F7FF)
val LightBlueSurfaceVariant = Color(0xFFE1E6F0)

val LightBluePrimaryContainer = Color(0xFFD6E3FF)
val LightBlueOnPrimaryContainer = Color(0xFF0A2E5C)
val LightBlueSecondaryContainer = Color(0xFFDCE4F5)
val LightBlueOnSecondaryContainer = Color(0xFF16233D)
val LightBlueTertiaryContainer = Color(0xFFCFEBF5)
val LightBlueOnTertiaryContainer = Color(0xFF0B2F3A)
