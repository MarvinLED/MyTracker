package com.example.mytracker.nutrition.library

import com.example.mytracker.nutrition.diary.DiaryPickerSort
import com.example.mytracker.nutrition.diary.next
import com.example.mytracker.nutrition.food.Tag
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The two filter buttons show no words, so a short press has to land somewhere predictable: one
 * state on, and eventually back where it started.
 */
class LibraryFilterCycleTest {
    private fun tag(id: String, name: String) = Tag(id = id, name = name, createdAt = Instant.EPOCH)

    @Test
    fun sortCycleGoesRoundAndComesBack() {
        assertEquals(DiaryPickerSort.MOST_EATEN, DiaryPickerSort.LAST_EATEN.next())
        assertEquals(DiaryPickerSort.NAME, DiaryPickerSort.MOST_EATEN.next())
        assertEquals(DiaryPickerSort.LAST_EATEN, DiaryPickerSort.NAME.next())
    }

    @Test
    fun everySortIsReachableByTapping() {
        val visited = generateSequence(DiaryPickerSort.LAST_EATEN) { it.next() }
            .take(DiaryPickerSort.entries.size)
            .toSet()

        assertEquals(DiaryPickerSort.entries.toSet(), visited)
    }

    @Test
    fun tagCycleStartsAtAllAndReturnsToIt() {
        val tags = listOf(tag("a", "Obst"), tag("b", "Snack"))

        val first = nextTagId(null, tags)
        val second = nextTagId(first, tags)
        val third = nextTagId(second, tags)

        assertEquals("a", first)
        assertEquals("b", second)
        // Round the corner: "Alle" is part of the cycle, not something one has to reach by menu.
        assertNull(third)
    }

    @Test
    fun withoutTagsTheButtonStaysOnAll() {
        assertNull(nextTagId(null, emptyList()))
        assertNull(nextTagId("gone", emptyList()))
    }

    @Test
    fun aTagThatNoLongerExistsStartsTheCycleOver() {
        val tags = listOf(tag("a", "Obst"))

        // Deleting the filtered tag must not strand the button on a state it can never leave.
        assertNull(nextTagId("deleted", tags))
    }
}
