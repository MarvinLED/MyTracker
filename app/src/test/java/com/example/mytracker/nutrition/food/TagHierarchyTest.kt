package com.example.mytracker.nutrition.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagHierarchyTest {
    // vegan ⇒ vegetarisch ⇒ pflanzlich, and vegan ⇒ laktosefrei on the side.
    private val chain = listOf(
        TagImplication(childTagId = "vegan", parentTagId = "vegetarisch"),
        TagImplication(childTagId = "vegetarisch", parentTagId = "pflanzlich"),
        TagImplication(childTagId = "vegan", parentTagId = "laktosefrei"),
    )

    @Test
    fun filterClosure_withoutImplications_isJustTheTagItself() {
        assertEquals(setOf("vegan"), tagFilterClosure("vegan", emptyList()))
    }

    @Test
    fun filterClosure_pullsInEveryTagThatImpliesIt_transitively() {
        // Filtering "pflanzlich" has to reach vegan through vegetarisch, two hops down.
        assertEquals(setOf("pflanzlich", "vegetarisch", "vegan"), tagFilterClosure("pflanzlich", chain))
    }

    @Test
    fun filterClosure_doesNotWidenUpwards() {
        // The narrower tag stays narrow: "vegan" must not start matching vegetarian-only items.
        assertEquals(setOf("vegan"), tagFilterClosure("vegan", chain))
    }

    @Test
    fun filterClosure_diamond_countsSharedAncestorOnce() {
        val diamond = listOf(
            TagImplication(childTagId = "vegan", parentTagId = "vegetarisch"),
            TagImplication(childTagId = "vegan", parentTagId = "milchfrei"),
            TagImplication(childTagId = "vegetarisch", parentTagId = "pflanzlich"),
            TagImplication(childTagId = "milchfrei", parentTagId = "pflanzlich"),
        )

        assertEquals(
            setOf("pflanzlich", "vegetarisch", "milchfrei", "vegan"),
            tagFilterClosure("pflanzlich", diamond),
        )
    }

    @Test
    fun impliedTagsClosure_walksUpwardsAndIncludesTheStart() {
        assertEquals(
            setOf("vegan", "vegetarisch", "laktosefrei", "pflanzlich"),
            impliedTagsClosure(setOf("vegan"), chain),
        )
    }

    @Test
    fun wouldCreateCycle_rejectsSelfImplication() {
        assertTrue(wouldCreateCycle("vegan", "vegan", emptyList()))
    }

    @Test
    fun wouldCreateCycle_rejectsReversingAnExistingChain() {
        // "pflanzlich implies vegan" would close the loop vegan → vegetarisch → pflanzlich → vegan.
        assertTrue(wouldCreateCycle("pflanzlich", "vegan", chain))
    }

    @Test
    fun wouldCreateCycle_allowsAnUnrelatedPair() {
        assertFalse(wouldCreateCycle("glutenfrei", "vegan", chain))
    }

    @Test
    fun closures_terminateOnAlreadyCyclicData() {
        // Not reachable through addImplication, but an imported backup or a hand-edited database
        // could carry it — the walk has to answer rather than hang.
        val cyclic = listOf(
            TagImplication(childTagId = "a", parentTagId = "b"),
            TagImplication(childTagId = "b", parentTagId = "a"),
        )

        assertEquals(setOf("a", "b"), tagFilterClosure("a", cyclic))
        assertEquals(setOf("a", "b"), impliedTagsClosure(setOf("a"), cyclic))
    }
}
