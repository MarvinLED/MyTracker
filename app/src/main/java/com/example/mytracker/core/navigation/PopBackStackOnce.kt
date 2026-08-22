package com.example.mytracker.core.navigation

import androidx.navigation.NavBackStackEntry
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
 *  * [entry] is the screen's own back stack entry, handed in by its `composable { entry -> }` block.
 *    A press from a screen that is no longer the current entry is a press on a screen that has
 *    already popped itself — the second tap of a double tap lands on the outgoing screen, which is
 *    still on display while it animates away. No timing is involved, so this holds just as well when
 *    the device has animations turned off.
 *  * nothing is popped when there is no entry behind this one. Leaving the app is the system back
 *    button's job; a screen's own button emptying the graph is never what was meant.
 *
 * Both guards have to sit *before* the call: `popBackStack()` carries the pop out even when it
 * returns false, so its return value can never be used to decide whether popping was safe.
 */
fun NavController.popBackStackOnce(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id != entry.id) return
    if (previousBackStackEntry == null) return
    popBackStack()
}
