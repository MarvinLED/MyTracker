package com.example.prokject2_tracker.core.backup

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes backup files in a folder the user picked through the Storage Access Framework.
 *
 * Talks to [DocumentsContract] directly rather than through `androidx.documentfile`: the handful of
 * calls needed here — list a tree's children, create or rewrite one child, delete one — are all
 * framework API, and the app has no other use for the dependency.
 *
 * Every method deals in uri *strings*, because that is the form the folder is remembered in by
 * [BackupSettingsRepository].
 */
@Singleton
class BackupFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    /**
     * Holds on to the grant the folder picker just handed out, so the folder is still writable after
     * a restart, and drops the grant on the folder being replaced — the system caps how many an app
     * may hold, and a folder no longer configured has no claim on one of the slots.
     */
    fun persistFolderPermission(folder: Uri, previous: String?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(folder, flags)
        if (previous != null && previous != folder.toString()) {
            // Best-effort: a grant that was already dropped, or never held, is not a problem here.
            runCatching { resolver.releasePersistableUriPermission(previous.toUri(), flags) }
        }
    }

    /** The folder's name as the picker shows it, for telling the user what is configured. */
    fun folderLabel(folder: Uri): String? {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            folder,
            DocumentsContract.getTreeDocumentId(folder),
        )
        return resolver.query(documentUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }

    /**
     * This app's backups in [folderUri], newest first. Foreign files in the folder are ignored, so
     * pointing the backup at a folder that holds other things is safe — nothing else is ever listed,
     * offered for restore, or pruned.
     */
    fun list(folderUri: String): List<BackupFile> {
        val tree = folderUri.toUri()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val cursor: Cursor = resolver.query(children, projection, null, null, null) ?: return emptyList()
        return cursor.use {
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: continue
                    if (!isBackupFileName(name)) continue
                    add(
                        BackupFile(
                            uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)).toString(),
                            name = name,
                            lastModifiedEpochMillis = cursor.getLong(2),
                            sizeBytes = cursor.getLong(3),
                        ),
                    )
                }
            }
        }.sortedByDescending { it.lastModifiedEpochMillis }
    }

    /**
     * Writes [content] to [name] inside [folderUri], returning the document's uri.
     *
     * An existing file of that name is rewritten in place — that is what makes
     * [BackupRetention.OVERWRITE] overwrite. "wt" truncates first, so a backup that shrank cannot
     * leave the tail of the previous one behind; a provider that refuses the truncating mode gets
     * the file replaced instead, which reaches the same place by a slightly riskier route.
     */
    fun write(folderUri: String, name: String, content: String): String {
        val tree = folderUri.toUri()
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val existing = list(folderUri).firstOrNull { it.name == name }?.uri?.toUri()

        val target = if (existing != null) {
            runCatching { writeTo(existing, content) }.getOrElse {
                DocumentsContract.deleteDocument(resolver, existing)
                createAndWrite(parent, name, content)
            }
            existing
        } else {
            createAndWrite(parent, name, content)
        }
        return target.toString()
    }

    fun read(fileUri: String): String =
        requireNotNull(resolver.openInputStream(fileUri.toUri())) {
            "Datei konnte nicht gelesen werden"
        }.use { stream -> BufferedReader(InputStreamReader(stream)).readText() }

    fun delete(fileUri: String) {
        DocumentsContract.deleteDocument(resolver, fileUri.toUri())
    }

    private fun createAndWrite(parent: Uri, name: String, content: String): Uri {
        val created = requireNotNull(
            DocumentsContract.createDocument(resolver, parent, MIME_TYPE, name),
        ) { "Datei konnte im gewählten Ordner nicht angelegt werden" }
        writeTo(created, content)
        return created
    }

    private fun writeTo(uri: Uri, content: String) {
        requireNotNull(resolver.openOutputStream(uri, "wt")) {
            "Datei konnte nicht geschrieben werden"
        }.use { it.write(content.toByteArray()) }
    }

    private fun String.toUri(): Uri = Uri.parse(this)

    private companion object {
        const val MIME_TYPE = "application/json"
    }
}
