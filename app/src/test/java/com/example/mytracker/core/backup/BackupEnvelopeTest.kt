package com.example.prokject2_tracker.core.backup

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The envelope itself, against stand-in providers: what a category selection actually does to the
 * file that gets written and to the data that gets touched on the way back in.
 */
class BackupEnvelopeTest {
    private val log = mutableListOf<String>()

    private val settings = FakeProvider("settings", BackupScope.SETTINGS, log = log)
    private val foods = FakeProvider("foods", BackupScope.LIBRARY, importPriority = 5, log = log)
    private val muscleGroups = FakeProvider("muscleGroups", BackupScope.LIBRARY, log = log)
    private val diary = FakeProvider("diaryEntries", BackupScope.DAILY_ENTRIES, importPriority = 100, log = log)

    private val repository = BackupRepository(setOf(settings, foods, muscleGroups, diary))

    @Test
    fun onlyTheSelectedCategoriesEndUpInTheFile() = runTest {
        val json = Json.parseToJsonElement(
            repository.exportToJson(setOf(BackupScope.LIBRARY)),
        ).jsonObject

        assertEquals(setOf("LIBRARY"), json["scopes"]!!.jsonArrayOfStrings())
        assertTrue(json.containsKey("foods"))
        assertTrue(json.containsKey("muscleGroups"))
        assertFalse(json.containsKey("diaryEntries"))
        assertFalse(json.containsKey("settings"))
        assertFalse(diary.exported)
    }

    @Test
    fun everythingSelectedWritesEveryProvider() = runTest {
        val json = Json.parseToJsonElement(
            repository.exportToJson(BackupScope.entries.toSet()),
        ).jsonObject

        assertEquals(
            setOf("settings", "foods", "muscleGroups", "diaryEntries"),
            json.keys - setOf("schemaVersion", "exportedAt", "scopes"),
        )
    }

    @Test
    fun aFileReportsWhatItHolds() = runTest {
        val raw = repository.exportToJson(setOf(BackupScope.LIBRARY, BackupScope.DAILY_ENTRIES))

        val info = repository.readInfo(raw)

        assertEquals(2, info.schemaVersion)
        assertEquals(setOf(BackupScope.LIBRARY, BackupScope.DAILY_ENTRIES), info.scopes)
    }

    /** The old library-only format had no `scopes` key at all, and has to keep importing. */
    @Test
    fun aSchemaOneFileIsReadAsALibraryBackup() = runTest {
        val v1 = """{"schemaVersion":1,"exportedAt":1700000000000,"foods":["x"],"muscleGroups":["y"]}"""

        val info = repository.readInfo(v1)

        assertEquals(1, info.schemaVersion)
        assertEquals(setOf(BackupScope.LIBRARY), info.scopes)
    }

    @Test
    fun aSchemaOneFileStillImports() = runTest {
        val v1 = """{"schemaVersion":1,"exportedAt":1700000000000,"foods":["x"]}"""

        repository.importFromJson(v1, setOf(BackupScope.LIBRARY), ImportMode.MERGE)

        assertEquals(1, foods.imported.size)
    }

    @Test
    fun textThatIsNotABackupIsRejectedBeforeAnythingIsWritten() = runTest {
        val notABackup = """{"hello":"world"}"""

        val failure = runCatching { repository.readInfo(notABackup) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(log.isEmpty())
    }

    @Test
    fun importingOneCategoryLeavesTheOthersUntouched() = runTest {
        val raw = repository.exportToJson(BackupScope.entries.toSet())

        repository.importFromJson(raw, setOf(BackupScope.LIBRARY), ImportMode.MERGE)

        assertEquals(1, foods.imported.size)
        assertEquals(1, muscleGroups.imported.size)
        assertTrue(diary.imported.isEmpty())
        assertTrue(settings.imported.isEmpty())
    }

    @Test
    fun mergingNeverDeletesAnything() = runTest {
        val raw = repository.exportToJson(BackupScope.entries.toSet())

        repository.importFromJson(raw, BackupScope.entries.toSet(), ImportMode.MERGE)

        assertTrue(log.none { it.startsWith("clear:") })
    }

    @Test
    fun replacingEmptiesEveryChosenCategoryBeforeImportingIt() = runTest {
        val raw = repository.exportToJson(BackupScope.entries.toSet())

        repository.importFromJson(raw, setOf(BackupScope.LIBRARY), ImportMode.REPLACE)

        assertTrue(foods.cleared)
        assertTrue(muscleGroups.cleared)
        assertFalse(diary.cleared)
        assertFalse(settings.cleared)
        // Every wipe happens before the first row is put back, not per provider.
        assertTrue(log.indexOfLast { it.startsWith("clear:") } < log.indexOfFirst { it.startsWith("import:") })
    }

    /** Dependants are wiped before what they depend on, or a foreign key stops the delete. */
    @Test
    fun replacingWipesInReverseImportOrder() = runTest {
        val raw = repository.exportToJson(BackupScope.entries.toSet())

        repository.importFromJson(raw, BackupScope.entries.toSet(), ImportMode.REPLACE)

        val clears = log.filter { it.startsWith("clear:") }
        val imports = log.filter { it.startsWith("import:") }
        assertEquals(listOf("clear:diaryEntries", "clear:foods"), clears.take(2))
        assertEquals("import:diaryEntries", imports.last())
    }

    @Test
    fun aProviderWhoseKeyIsMissingFromTheFileIsSkipped() = runTest {
        val onlyFoods = """{"schemaVersion":2,"scopes":["LIBRARY"],"foods":["x"]}"""

        repository.importFromJson(onlyFoods, setOf(BackupScope.LIBRARY), ImportMode.MERGE)

        assertEquals(1, foods.imported.size)
        assertTrue(muscleGroups.imported.isEmpty())
    }

    /**
     * Ersetzen may only empty what the file can fill again. A table added after the backup was
     * written has no key in it, so wiping it would delete data and put nothing back.
     */
    @Test
    fun replacingLeavesTablesTheFileSaysNothingAboutAlone() = runTest {
        val onlyFoods = """{"schemaVersion":2,"scopes":["LIBRARY"],"foods":["x"]}"""

        repository.importFromJson(onlyFoods, setOf(BackupScope.LIBRARY), ImportMode.REPLACE)

        assertTrue(foods.cleared)
        assertFalse(muscleGroups.cleared)
        assertTrue(muscleGroups.imported.isEmpty())
    }

    /** An empty list is still an answer: the category was exported, it just held nothing. */
    @Test
    fun replacingWipesACategoryThatWasExportedEmpty() = runTest {
        val emptyFoods = """{"schemaVersion":2,"scopes":["LIBRARY"],"foods":[],"muscleGroups":[]}"""

        repository.importFromJson(emptyFoods, setOf(BackupScope.LIBRARY), ImportMode.REPLACE)

        assertTrue(foods.cleared)
        assertTrue(muscleGroups.cleared)
    }

    @Test
    fun anEnvelopeWithoutATimestampStillReads() = runTest {
        val info = repository.readInfo("""{"schemaVersion":2,"scopes":["SETTINGS"]}""")

        assertNull(info.exportedAt)
        assertEquals(setOf(BackupScope.SETTINGS), info.scopes)
    }

    private fun JsonElement.jsonArrayOfStrings(): Set<String> =
        (this as kotlinx.serialization.json.JsonArray).map { it.jsonPrimitive.content }.toSet()
}

private class FakeProvider(
    override val key: String,
    override val scope: BackupScope,
    override val importPriority: Int = 0,
    private val log: MutableList<String>,
) : BackupExportProvider {
    var exported = false
    var cleared = false
    val imported = mutableListOf<JsonElement>()

    override suspend fun export(): JsonElement {
        exported = true
        return JsonPrimitive("data-$key")
    }

    override suspend fun import(json: JsonElement) {
        imported += json
        log += "import:$key"
    }

    override suspend fun clear() {
        cleared = true
        log += "clear:$key"
    }
}
