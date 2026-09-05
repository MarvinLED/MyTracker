package com.example.mytracker.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the figure gets to wear, and why it never has to give any of it back. */
class AvatarItemTest {
    private fun levels(vararg entries: Pair<AvatarAttribute, Pair<Int, Int>>): List<AttributeLevel> =
        AvatarAttribute.entries.map { attribute ->
            val (level, record) = entries.toMap()[attribute] ?: (0 to 0)
            AttributeLevel(attribute = attribute, level = level, record = record, fraction = 0f)
        }

    @Test
    fun equipmentIsEarnedAtTheRequiredLevel() {
        val unlocked = unlockedItems(levels(AvatarAttribute.KRAFT to (3 to 3)))

        assertTrue(AvatarItem.ARMBAND in unlocked)
        assertFalse(AvatarItem.LAUFSCHUHE in unlocked)
    }

    @Test
    fun aPieceOnceEarnedIsNeverTakenBack() {
        // The form has collapsed to nothing; the record says it was once there.
        val unlocked = unlockedItems(levels(AvatarAttribute.KRAFT to (0 to 4)))

        // Losing the Armband over a quiet fortnight would punish exactly the moment that needs no
        // more punishing.
        assertTrue(AvatarItem.ARMBAND in unlocked)
    }

    @Test
    fun theWholeFigurePiecesAddTheRecordsUp() {
        val unlocked = unlockedItems(
            levels(
                AvatarAttribute.KRAFT to (2 to 3),
                AvatarAttribute.FORM to (2 to 3),
                AvatarAttribute.AUSDAUER to (2 to 3),
                AvatarAttribute.KLARHEIT to (2 to 3),
            ),
        )

        // Twelve across the five records — the cape, but not yet the crown at twenty.
        assertTrue(AvatarItem.UMHANG in unlocked)
        assertFalse(AvatarItem.KRONE in unlocked)
    }

    @Test
    fun theNextUnlockIsTheClosestOne() {
        val next = nextUnlock(levels(AvatarAttribute.AUSDAUER to (1 to 1)))!!

        // Stirnband needs Ausdauer 2 and is one level away; everything else is further.
        assertEquals(AvatarItem.STIRNBAND, next.item)
        assertEquals(1, next.remaining)
        assertEquals(0.5f, next.fraction, 0.0001f)
    }

    @Test
    fun aFullCollectionHasNoNextUnlock() {
        val maxed = AvatarAttribute.entries.map {
            AttributeLevel(attribute = it, level = 20, record = 20, fraction = 1f)
        }

        assertEquals(AvatarItem.entries.size, unlockedItems(maxed).size)
        assertNull(nextUnlock(maxed))
    }

    @Test
    fun nothingIsEarnedOnAnEmptyFigure() {
        val empty = AvatarAttribute.entries.map {
            AttributeLevel(attribute = it, level = 0, record = 0, fraction = 0f)
        }

        assertTrue(unlockedItems(empty).isEmpty())
        // But there is always something to aim at, which is the point of the preview.
        assertEquals(AvatarItem.STIRNBAND, nextUnlock(empty)!!.item)
    }

    @Test
    fun everyRequirementIsSayableInWords() {
        // The locked preview shows this sentence, so none of them may come out empty.
        AvatarItem.entries.forEach { item ->
            assertTrue(item.requirementText().isNotBlank())
        }
    }
}
