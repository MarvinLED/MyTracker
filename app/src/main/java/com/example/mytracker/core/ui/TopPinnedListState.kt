package com.example.mytracker.core.ui

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * A [LazyListState] that opens at the top of [items] and is held there until the user scrolls it
 * themselves.
 *
 * Not a no-op: a LazyColumn anchors on the first visible item's *key*, and the first rows reach the
 * screen before the order does — a sort that depends on data still arriving from Room shows up
 * later than the rows it sorts. When the real order lands, the row that happened to open at the top
 * takes the viewport with it to wherever it now sits, and the list ends up opened somewhere in its
 * own middle.
 *
 * [resetKeys] are the things that start a *new* list — a changed sort, filter or query. The pin
 * comes back for each of them, because a list the user has just asked to be reordered should be
 * looked at from its beginning.
 */
@Composable
fun rememberTopPinnedListState(items: Any?, vararg resetKeys: Any?): LazyListState {
    val listState = rememberLazyListState()
    var userScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) userScrolled = true
        }
    }
    LaunchedEffect(*resetKeys) { userScrolled = false }
    LaunchedEffect(items, userScrolled) {
        if (!userScrolled) listState.scrollToItem(0)
    }
    return listState
}
