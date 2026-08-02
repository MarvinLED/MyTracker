package com.example.prokject2_tracker.sleep

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prokject2_tracker.core.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sleep log against a real database: that a night is keyed by the morning it ended, that tags
 * come and go with it, and that deleting a tag takes the label off the nights without taking the
 * nights.
 */
@RunWith(AndroidJUnit4::class)
class SleepRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: SleepRepository

    private val day = 20_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = SleepRepository(db.sleepDao(), db.sleepTagDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun loggingTheSameNightTwiceCorrectsItInsteadOfAddingASecond() = runBlocking {
        repository.logNight(day, startMinuteOfDay = 23 * 60, endMinuteOfDay = 7 * 60, morningFitness = 6, lastMealMinuteOfDay = 20 * 60, tagIds = emptyList())
        val first = repository.getForDay(day)!!

        repository.logNight(day, startMinuteOfDay = 22 * 60 + 30, endMinuteOfDay = 6 * 60 + 15, morningFitness = 8, lastMealMinuteOfDay = null, tagIds = emptyList())

        assertEquals(1, repository.observeAll().first().size)
        val corrected = repository.getForDay(day)!!
        assertEquals(first.id, corrected.id)
        // Correcting the times keeps the night where it was in the log rather than re-dating it.
        assertEquals(first.createdAt, corrected.createdAt)
        assertEquals(465, corrected.durationMinutes)
        assertEquals(8, corrected.morningFitness)
        assertNull(corrected.lastMealMinuteOfDay)
    }

    @Test
    fun aNightCarriesItsTagsAndGivesThemUpAgain() = runBlocking {
        val hot = repository.createTag("heiß")!!
        val dreams = repository.createTag("viel geträumt")!!

        repository.logNight(day, 23 * 60, 7 * 60, morningFitness = null, lastMealMinuteOfDay = null, tagIds = listOf(hot.id, dreams.id))
        assertEquals(setOf(hot.id, dreams.id), repository.getTagIdsForEntry("sleep-$day").toSet())

        // Saving again replaces the whole set, so unticking a tag actually removes it.
        repository.logNight(day, 23 * 60, 7 * 60, morningFitness = null, lastMealMinuteOfDay = null, tagIds = listOf(dreams.id))
        assertEquals(listOf(dreams.id), repository.getTagIdsForEntry("sleep-$day"))
    }

    @Test
    fun aTagNameIsReusedRatherThanDuplicated() = runBlocking {
        val first = repository.createTag("Snooze")!!
        val again = repository.createTag("  snooze ")!!

        assertEquals(first.id, again.id)
        assertEquals(1, repository.observeTags().first().size)
        // Blank input creates nothing at all.
        assertNull(repository.createTag("   "))
    }

    @Test
    fun deletingATagKeepsTheNightsItWasOn() = runBlocking {
        val hot = repository.createTag("heiß")!!
        repository.logNight(day, 23 * 60, 7 * 60, morningFitness = 5, lastMealMinuteOfDay = null, tagIds = listOf(hot.id))
        assertEquals(1, repository.tagUsageCount(hot.id))

        repository.deleteTag(hot)

        assertTrue(repository.observeTags().first().isEmpty())
        val night = repository.getForDay(day)
        assertEquals(5, night?.morningFitness)
        assertTrue(repository.getTagIdsForEntry("sleep-$day").isEmpty())
    }

    @Test
    fun deletingANightTakesItsTagLinksWithIt() = runBlocking {
        val hot = repository.createTag("heiß")!!
        repository.logNight(day, 23 * 60, 7 * 60, morningFitness = null, lastMealMinuteOfDay = null, tagIds = listOf(hot.id))

        repository.delete(repository.getForDay(day)!!)

        assertNull(repository.getForDay(day))
        assertEquals(0, repository.tagUsageCount(hot.id))
        // The tag itself is library data and stays.
        assertEquals(1, repository.observeTags().first().size)
    }

    @Test
    fun theFormPrefillsFromTheNightBefore() = runBlocking {
        repository.logNight(day - 1, 22 * 60 + 45, 6 * 60 + 30, morningFitness = 7, lastMealMinuteOfDay = null, tagIds = emptyList())

        val previous = repository.getMostRecentBefore(day)!!

        assertEquals(day - 1, previous.epochDay)
        assertEquals(22 * 60 + 45, previous.startMinuteOfDay)
        assertNull(repository.getMostRecentBefore(day - 1))
    }

    /** The Analyse series computes the wrap-around in SQL — it has to agree with the Kotlin side. */
    @Test
    fun theDurationSeriesCountsAcrossMidnightLikeTheEntityDoes() = runBlocking {
        repository.logNight(day, 23 * 60 + 10, 6 * 60 + 45, morningFitness = null, lastMealMinuteOfDay = null, tagIds = emptyList())
        repository.logNight(day + 1, 14 * 60, 15 * 60 + 30, morningFitness = null, lastMealMinuteOfDay = null, tagIds = emptyList())

        val points = repository.observeDailyDurationMinutes(day, day + 1).first()

        assertEquals(listOf(day, day + 1), points.map { it.epochDay })
        assertEquals(455.0, points[0].value, 0.001)
        assertEquals(90.0, points[1].value, 0.001)
        assertEquals(
            repository.getForDay(day)!!.durationMinutes.toDouble(),
            points[0].value,
            0.001,
        )
    }
}
