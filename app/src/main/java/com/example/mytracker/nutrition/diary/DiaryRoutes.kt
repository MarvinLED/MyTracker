package com.example.mytracker.nutrition.diary

import kotlinx.serialization.Serializable

@Serializable
data object DiaryRoute

// Adding an entry is not a destination of this package any more: the Bibliothek does it, for a day
// and a meal it is handed — see `LibraryLogRoute` in the library package.

@Serializable
data class DiaryEditEntryRoute(val entryId: String)

/**
 * The Verlauf. A plain detail destination, not a top-level one: it is reached from the Tagebuch's
 * own button and appears in neither the bottom bar nor the drawer, so it navigates with a plain
 * `navigate` rather than through `navigateToTopLevel`.
 */
@Serializable
data object DiaryHistoryRoute
