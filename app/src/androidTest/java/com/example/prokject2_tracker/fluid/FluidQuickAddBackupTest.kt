package com.example.prokject2_tracker.fluid

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prokject2_tracker.core.database.AppDatabase
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Schnellauswahl travels with the Bibliothek export. What matters on the way back in is what the
 * buttons depend on: their drink type has to exist, and no more of them may arrive than the Tagebuch
 * has room to draw.
 */
@RunWith(AndroidJUnit4::class)
class FluidQuickAddBackupTest {
    private lateinit var db: AppDatabase
    private lateinit var provider: FluidQuickAddLibraryExportProvider

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        provider = FluidQuickAddLibraryExportProvider(db.fluidQuickAddDao(), db.fluidTypeDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportedButtonsComeBackOnADeviceThatHasTheirDrinkType() = runBlocking {
        seedType("type-1", "Wasser")
        db.fluidQuickAddDao().upsert(quickAdd("quick-1", "type-1", FluidQuickAddSymbol.GLASS, 250.0, 0))
        db.fluidQuickAddDao().upsert(quickAdd("quick-2", "type-1", FluidQuickAddSymbol.ML_100, 100.0, 1))
        val exported = provider.export()

        // A fresh device that already has the drink type from the same export.
        val target = freshDatabase()
        val targetProvider = FluidQuickAddLibraryExportProvider(target.fluidQuickAddDao(), target.fluidTypeDao())
        target.fluidTypeDao().upsert(type("type-1", "Wasser"))
        targetProvider.import(exported)

        val imported = target.fluidQuickAddDao().getAllOnce()
        assertEquals(2, imported.size)
        assertEquals(FluidQuickAddSymbol.GLASS, imported[0].symbol)
        assertEquals(250.0, imported[0].amountMl, 0.0001)
        assertEquals(FluidQuickAddSymbol.ML_100, imported[1].symbol)
        target.close()
    }

    @Test
    fun aButtonWhoseDrinkTypeDidNotComeAlongIsSkipped() = runBlocking {
        seedType("type-1", "Wasser")
        db.fluidQuickAddDao().upsert(quickAdd("quick-1", "type-1", FluidQuickAddSymbol.GLASS, 250.0, 0))
        val exported = provider.export()

        val target = freshDatabase()
        val targetProvider = FluidQuickAddLibraryExportProvider(target.fluidQuickAddDao(), target.fluidTypeDao())
        targetProvider.import(exported)

        assertEquals(emptyList<FluidQuickAdd>(), target.fluidQuickAddDao().getAllOnce())
        target.close()
    }

    @Test
    fun importNeverFillsPastTheTwoRowsTheTagebuchDraws() = runBlocking {
        seedType("type-1", "Wasser")
        repeat(FluidQuickAddLimit + 3) { index ->
            db.fluidQuickAddDao().upsert(
                quickAdd("quick-$index", "type-1", FluidQuickAddSymbol.GLASS, 250.0, index),
            )
        }
        val exported = provider.export()

        val target = freshDatabase()
        val targetProvider = FluidQuickAddLibraryExportProvider(target.fluidQuickAddDao(), target.fluidTypeDao())
        target.fluidTypeDao().upsert(type("type-1", "Wasser"))
        targetProvider.import(exported)

        assertEquals(FluidQuickAddLimit, target.fluidQuickAddDao().getAllOnce().size)
        assertNull(target.fluidQuickAddDao().getById("quick-${FluidQuickAddLimit + 2}"))
        target.close()
    }

    private fun freshDatabase(): AppDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    private suspend fun seedType(id: String, name: String) {
        db.fluidTypeDao().upsert(type(id, name))
    }

    private fun type(id: String, name: String) = FluidType(
        id = id,
        name = name,
        defaultQuickAddMl = 250.0,
        sortOrder = 0,
        createdAt = Instant.ofEpochMilli(1_700_000_000_000),
    )

    private fun quickAdd(
        id: String,
        typeId: String,
        symbol: FluidQuickAddSymbol,
        amountMl: Double,
        sortOrder: Int,
    ) = FluidQuickAdd(
        id = id,
        fluidTypeId = typeId,
        symbol = symbol,
        amountMl = amountMl,
        sortOrder = sortOrder,
        createdAt = Instant.ofEpochMilli(1_700_000_000_000),
    )
}
