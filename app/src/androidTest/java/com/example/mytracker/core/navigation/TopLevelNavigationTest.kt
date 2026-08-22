package com.example.mytracker.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mytracker.fitness.FitnessRoute
import com.example.mytracker.fluid.FluidRoute
import com.example.mytracker.goals.DayGoalsRoute
import com.example.mytracker.goals.GoalsRoute
import com.example.mytracker.nutrition.diary.DiaryRoute
import com.example.mytracker.nutrition.food.FoodEditRoute
import com.example.mytracker.nutrition.library.LibraryRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bottom bar and the drawer against a real NavController, with the app's own routes but stand-in
 * screens — the back stack is what is under test, not what the screens draw.
 *
 * The case that broke: the Tagebuch's "Bibliothek" button pushed a top-level destination with a
 * plain `navigate`, after which tapping the Tagebuch tab popped the Bibliothek and restored it in
 * the same step — the tab looked dead and only the system back button worked. Both in-screen
 * shortcuts into a drawer destination are pinned here.
 */
@RunWith(AndroidJUnit4::class)
class TopLevelNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController

    /**
     * The entry each stand-in screen was composed with, by label. A screen's "Zurück" button closes
     * over its own entry (see [popBackStackOnce]), and the point of several tests below is to press
     * the button of a screen that has already been popped — which is what the second tap of a real
     * double tap hits, since the outgoing screen stays on display while it animates away.
     */
    private val entries = mutableMapOf<String, NavBackStackEntry>()

    @Composable
    private fun Stub(label: String, entry: NavBackStackEntry) {
        SideEffect { entries[label] = entry }
        Text(label)
    }

    private fun startNavHost() {
        entries.clear()
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = DiaryRoute) {
                composable<DiaryRoute> { entry -> Stub("Tagebuch", entry) }
                composable<LibraryRoute> { entry -> Stub("Bibliothek", entry) }
                composable<FluidRoute> { entry -> Stub("Flüssigkeiten", entry) }
                composable<FitnessRoute> { entry -> Stub("Fitness", entry) }
                composable<DayGoalsRoute> { entry -> Stub("Tagesziele", entry) }
                composable<GoalsRoute> { entry -> Stub("Ziele", entry) }
                composable<FoodEditRoute> { entry -> Stub("Lebensmittel bearbeiten", entry) }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * A graph that does *not* start on a top-level destination. The app's own graph starts on the
     * Tagebuch, which is top-level itself, so [navigateToTopLevel]'s pop loop always has a floor to
     * stop on there and its guard can never be exercised.
     */
    private fun startDetailOnlyNavHost() {
        entries.clear()
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = FoodEditRoute()) {
                composable<FoodEditRoute> { entry -> Stub("Lebensmittel bearbeiten", entry) }
                composable<DiaryRoute> { entry -> Stub("Tagebuch", entry) }
            }
        }
        composeRule.waitForIdle()
    }

    private fun onNav(block: NavHostController.() -> Unit) {
        composeRule.runOnUiThread { navController.block() }
        composeRule.waitForIdle()
    }

    /** The entries actually on the stack — the NavGraph itself sits in there and never draws. */
    private fun stackDepth(): Int =
        navController.currentBackStack.value.count { it.destination !is NavGraph }

    private fun assertOn(route: Any) =
        assertEquals(route::class.qualifiedName, navController.currentDestination?.route)

    /** The Tagebuch's "Bibliothek" button, as [AppNavHost] wires it. */
    @Test
    fun theDiaryTabComesBackFromALibraryOpenedByTheDiaryButton() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        assertOn(LibraryRoute)

        onNav { navigateToTopLevel(DiaryRoute) }
        assertOn(DiaryRoute)
    }

    /** The Tagesziele screen's "Ziele bearbeiten" button — the same shortcut into the drawer. */
    @Test
    fun theDayGoalsTabComesBackFromTheGoalsEditor() {
        startNavHost()

        onNav { navigateToTopLevel(DayGoalsRoute) }
        onNav { navigateToTopLevel(GoalsRoute) }
        assertOn(GoalsRoute)

        onNav { navigateToTopLevel(DayGoalsRoute) }
        assertOn(DayGoalsRoute)
    }

    @Test
    fun aDetailScreenInsideTheLibraryIsDroppedWhenSwitchingTabs() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigate(FoodEditRoute()) }
        assertTrue(navController.currentDestination?.hasRoute(FoodEditRoute::class) == true)

        onNav { navigateToTopLevel(DiaryRoute) }
        assertOn(DiaryRoute)
    }

    @Test
    fun switchingTabsBackAndForthKeepsLandingOnTheTabsOwnScreen() {
        startNavHost()

        onNav { navigateToTopLevel(FluidRoute) }
        assertOn(FluidRoute)
        onNav { navigateToTopLevel(DiaryRoute) }
        assertOn(DiaryRoute)

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigateToTopLevel(FitnessRoute) }
        assertOn(FitnessRoute)
        onNav { navigateToTopLevel(DiaryRoute) }
        assertOn(DiaryRoute)
    }

    /**
     * The blank-page bug: two taps on a screen's own "Zurück" popped twice, the second one taking
     * the start destination off the stack, after which the NavHost had nothing left to draw and the
     * app showed the bottom bar over an empty background.
     *
     * Both presses go through in one UI-thread block, which is the fastest a double tap can be —
     * the second arrives before the first pop has settled.
     */
    @Test
    fun aDoubleTapOnBackDoesNotSkipThePageBehindIt() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigate(FoodEditRoute()) }
        val foodEntry = entries.getValue("Lebensmittel bearbeiten")

        composeRule.runOnUiThread {
            navController.popBackStackOnce(foodEntry)
            navController.popBackStackOnce(foodEntry)
        }
        composeRule.waitForIdle()

        assertOn(LibraryRoute)
    }

    /**
     * The same double tap, but slow enough that the first pop has fully settled in between — a
     * device with animations turned off is in this state immediately. The old guard was the
     * incoming screen's lifecycle state, so it stopped protecting anything the moment the
     * transition was over and the second tap skipped a page.
     */
    @Test
    fun aSecondBackTapAfterTheTransitionSettledDoesNotSkipAScreen() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigate(FoodEditRoute()) }
        val foodEntry = entries.getValue("Lebensmittel bearbeiten")

        onNav { popBackStackOnce(foodEntry) }
        assertOn(LibraryRoute)

        // The outgoing screen's own button, pressed a second time.
        onNav { popBackStackOnce(foodEntry) }
        assertOn(LibraryRoute)
    }

    /**
     * "Speichern" and "Zurück" in quick succession: both run [popBackStackOnce] for the same
     * screen — the editors call `onDone()` from a `LaunchedEffect` as well as from the arrow.
     * One screen closes, not two.
     */
    @Test
    fun savingAndThenTappingBackClosesOneScreenOnly() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigate(FoodEditRoute()) }
        val foodEntry = entries.getValue("Lebensmittel bearbeiten")

        onNav { popBackStackOnce(foodEntry) }
        onNav { popBackStackOnce(foodEntry) }

        assertOn(LibraryRoute)
        assertEquals(2, stackDepth())
    }

    /**
     * The other direction: a press that arrives while the screen is still animating *in* has to go
     * through. The editors reach this via `LaunchedEffect(state.isSaved) { onDone() }` when the save
     * has already completed by the time the screen enters composition; the old lifecycle guard
     * swallowed that press and left the editor open with no way out but the system back button.
     */
    @Test
    fun aBackPressDuringTheEnterTransitionStillCloses() {
        startNavHost()
        onNav { navigateToTopLevel(LibraryRoute) }

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread { navController.navigate(FoodEditRoute()) }
        // One frame in: the entry is current, but nowhere near RESUMED.
        composeRule.mainClock.advanceTimeBy(16L)
        composeRule.runOnUiThread {
            navController.popBackStackOnce(navController.currentBackStackEntry!!)
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertOn(LibraryRoute)
    }

    /** Leaving the app is the system back button's job — a screen's own button must not empty it. */
    @Test
    fun aScreensBackButtonNeverPopsTheLastEntry() {
        startNavHost()

        onNav { popBackStackOnce(currentBackStackEntry!!) }

        assertOn(DiaryRoute)
        assertTrue(navController.currentDestination != null)
    }

    /** Switching tabs out of a detail screen pops it — but never past the tab underneath. */
    @Test
    fun switchingTabsNeverLeavesTheHostWithNothingToDraw() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigate(FoodEditRoute()) }
        onNav { navigateToTopLevel(FluidRoute) }

        assertOn(FluidRoute)
        assertTrue(stackDepth() > 0)
    }

    /**
     * [navigateToTopLevel]'s pop loop on a graph with no top-level destination to stop on. Without
     * its `previousBackStackEntry` guard the loop pops the only entry there is — `popBackStack()`
     * carries that out and *then* returns false, so the loop's own break comes one pop too late and
     * the NavHost is left with nothing to draw.
     */
    @Test
    fun theTopLevelLoopCannotEmptyTheGraph() {
        startDetailOnlyNavHost()

        onNav { navigateToTopLevel(DiaryRoute) }

        assertOn(DiaryRoute)
        assertTrue(stackDepth() > 0)
    }

    @Test
    fun theDiaryTabIsNotStackedOnItselfWhenAlreadyThere() {
        startNavHost()

        onNav { navigateToTopLevel(DiaryRoute) }
        onNav { navigateToTopLevel(DiaryRoute) }
        assertOn(DiaryRoute)
        // The start destination, once, and nothing stacked behind it.
        assertEquals(1, stackDepth())
    }
}
