package com.example.prokject2_tracker.core.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.prokject2_tracker.fitness.FitnessRoute
import com.example.prokject2_tracker.fluid.FluidRoute
import com.example.prokject2_tracker.goals.DayGoalsRoute
import com.example.prokject2_tracker.goals.GoalsRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryRoute
import com.example.prokject2_tracker.nutrition.food.FoodEditRoute
import com.example.prokject2_tracker.nutrition.library.LibraryRoute
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

    private fun startNavHost() {
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = DiaryRoute) {
                composable<DiaryRoute> { Text("Tagebuch") }
                composable<LibraryRoute> { Text("Bibliothek") }
                composable<FluidRoute> { Text("Flüssigkeiten") }
                composable<FitnessRoute> { Text("Fitness") }
                composable<DayGoalsRoute> { Text("Tagesziele") }
                composable<GoalsRoute> { Text("Ziele") }
                composable<FoodEditRoute> { Text("Lebensmittel bearbeiten") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun onNav(block: NavHostController.() -> Unit) {
        composeRule.runOnUiThread { navController.block() }
        composeRule.waitForIdle()
    }

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
     * Both presses go through in one UI-thread block, which is what a double tap amounts to — the
     * second arrives before the first pop has settled.
     */
    @Test
    fun aDoubleTapOnBackDoesNotSkipThePageBehindIt() {
        startNavHost()

        onNav { navigateToTopLevel(LibraryRoute) }
        onNav { navigate(FoodEditRoute()) }

        composeRule.runOnUiThread {
            navController.popBackStackOnce()
            navController.popBackStackOnce()
        }
        composeRule.waitForIdle()

        assertOn(LibraryRoute)
    }

    /** Leaving the app is the system back button's job — a screen's own button must not empty it. */
    @Test
    fun aScreensBackButtonNeverPopsTheLastEntry() {
        startNavHost()

        onNav { popBackStackOnce() }

        assertOn(DiaryRoute)
        assertTrue(navController.currentDestination != null)
    }

    @Test
    fun theDiaryTabIsNotStackedOnItselfWhenAlreadyThere() {
        startNavHost()

        onNav { navigateToTopLevel(DiaryRoute) }
        onNav { navigateToTopLevel(DiaryRoute) }
        assertOn(DiaryRoute)
        // Back from the start destination ends the flow: nothing left to pop.
        composeRule.runOnUiThread { assertEquals(false, navController.popBackStack()) }
    }
}
