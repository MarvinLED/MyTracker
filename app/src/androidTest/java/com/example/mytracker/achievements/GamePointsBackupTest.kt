package com.example.mytracker.achievements

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupRepository
import com.example.mytracker.core.backup.BackupScope
import com.example.mytracker.core.backup.ImportMode
import com.example.mytracker.core.database.AppDatabase
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The points ledger through a backup and back.
 *
 * It has to survive the trip, and that is the whole reason it is in the backup at all: the ledger is
 * deliberately *not* reproducible from the entries, because Flüssigkeit, Schlaf and Habits keep no
 * goal history. A restore that lost it would silently reshape the figure and drop records that had
 * already been shown.
 */
@RunWith(AndroidJUnit4::class)
class GamePointsBackupTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private val bookedAt = Instant.ofEpochMilli(1_700_000_000_000)

    @Before
    fun setUp() {
        source = freshDatabase()
        target = freshDatabase()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun aSettledDayComesBackWithEveryAttribute() = runBlocking {
        source.gameDayPointsDao().upsertAll(dayRows(epochDay = 20_000, kraft = 25.0))

        restore()

        val rows = target.gameDayPointsDao().getAllOnce()
        assertEquals(AvatarAttribute.entries.size, rows.size)
        assertEquals(25.0, rows.first { it.attribute == AvatarAttribute.KRAFT }.points, 0.0001)
        // The zeroes travel too: the days present in the table are exactly the settled ones, so a
        // day that earned nothing still has to arrive as a settled day.
        assertEquals(0.0, rows.first { it.attribute == AvatarAttribute.FORM }.points, 0.0001)
        assertEquals(listOf(20_000L), target.gameDayPointsDao().bookedDays())
    }

    @Test
    fun aDayAlreadySettledHereKeepsItsOwnBooking() = runBlocking {
        source.gameDayPointsDao().upsertAll(dayRows(epochDay = 20_000, kraft = 25.0))
        target.gameDayPointsDao().upsertAll(dayRows(epochDay = 20_000, kraft = 90.0))

        restore()

        // A booking was final when it was made. Letting a backup overwrite it would re-decide a day
        // that has already been paid out — and the ledger's one rule is that it never changes.
        val rows = target.gameDayPointsDao().getAllOnce()
        assertEquals(90.0, rows.first { it.attribute == AvatarAttribute.KRAFT }.points, 0.0001)
    }

    @Test
    fun replacingLeavesExactlyWhatTheFileHeld() = runBlocking {
        source.gameDayPointsDao().upsertAll(dayRows(epochDay = 20_000, kraft = 25.0))
        target.gameDayPointsDao().upsertAll(dayRows(epochDay = 19_000, kraft = 10.0))

        restore(mode = ImportMode.REPLACE)

        assertEquals(listOf(20_000L), target.gameDayPointsDao().bookedDays())
    }

    private suspend fun restore(mode: ImportMode = ImportMode.MERGE) {
        val selected = setOf(BackupScope.DAILY_ENTRIES)
        val json = BackupRepository(providersFor(source)).exportToJson(selected)
        BackupRepository(providersFor(target)).importFromJson(json, selected, mode)
    }

    private fun providersFor(db: AppDatabase): Set<BackupExportProvider> =
        setOf(GamePointsExportProvider(db.gameDayPointsDao()))

    private fun dayRows(epochDay: Long, kraft: Double) = AvatarAttribute.entries.map { attribute ->
        GameDayPoints(
            epochDay = epochDay,
            attribute = attribute,
            points = if (attribute == AvatarAttribute.KRAFT) kraft else 0.0,
            bookedAt = bookedAt,
        )
    }

    private fun freshDatabase(): AppDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }
}
