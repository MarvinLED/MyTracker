package com.example.prokject2_tracker.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Back for a detail screen, as its "Zurück" button and its "Fertig"/"Speichern" action use it —
 * **one** screen per press, no matter how often the press arrives.
 *
 * A plain `popBackStack()` per tap is what emptied the app: tapping twice in quick succession pops
 * twice, and the second pop takes the start destination off the stack. The NavHost then has no
 * destination left to draw and the screen goes blank behind the bottom bar, with no way back other
 * than restarting. Two guards, against the two ways that happens:
 *
 *  * the second tap of a double tap lands while the first pop is still animating, so the current
 *    entry is not RESUMED yet — that press is dropped rather than skipping a second screen;
 *  * nothing is popped when there is no entry behind this one. Leaving the app is the system back
 *    button's job; a screen's own button emptying the graph is never what was meant.
 */
fun NavController.popBackStackOnce() {
    val entry = currentBackStackEntry ?: return
    if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
    if (previousBackStackEntry == null) return
    popBackStack()
}
