package com.example.mytracker.nutrition.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The combined Alle/Lebensmittel/Rezept button steps through its three states with every tap. */
class DiaryPickerModeCycleTest {
    @Test
    fun tappingWalksAlleToLebensmittelToRezept() {
        assertEquals(DiaryPickerMode.FOOD, DiaryPickerMode.ALL.nextListMode())
        assertEquals(DiaryPickerMode.RECIPE, DiaryPickerMode.FOOD.nextListMode())
    }

    @Test
    fun theCycleWrapsBackToAlle() {
        assertEquals(DiaryPickerMode.ALL, DiaryPickerMode.RECIPE.nextListMode())
    }

    @Test
    fun threeTapsReturnToWhereItStarted() {
        val start = DiaryPickerMode.ALL

        val end = start.nextListMode().nextListMode().nextListMode()

        assertEquals(start, end)
    }

    /** Schnelleintrag has its own button and must never be reachable by cycling into it. */
    @Test
    fun schnelleintragIsNotPartOfTheCycle() {
        assertFalse(DiaryPickerMode.QUICK in diaryPickerListModes)
        assertFalse(diaryPickerListModes.any { it.nextListMode() == DiaryPickerMode.QUICK })
    }

    /** Asking for the next state while on Schnelleintrag starts the cycle over rather than stalling. */
    @Test
    fun aStateOutsideTheCycleStartsItFromTheBeginning() {
        assertEquals(DiaryPickerMode.ALL, DiaryPickerMode.QUICK.nextListMode())
    }

    @Test
    fun everyListModeIsReachable() {
        val visited = generateSequence(DiaryPickerMode.ALL) { it.nextListMode() }
            .take(diaryPickerListModes.size)
            .toSet()

        assertEquals(diaryPickerListModes.toSet(), visited)
    }
}
