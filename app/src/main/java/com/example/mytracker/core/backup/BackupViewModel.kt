package com.example.mytracker.core.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The last thing that happened, shown under the buttons. */
sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Running : BackupStatus
    data class Succeeded(val message: String) : BackupStatus
    data class Failed(val message: String) : BackupStatus
}

/**
 * A file the user picked or tapped, waiting for them to say what to restore from it. Held rather
 * than acted on immediately because the answer — which categories, merge or replace — changes what
 * happens to data that cannot be got back.
 */
data class PendingImport(
    /** Where to read it from again once confirmed: a SAF document uri, as a string. */
    val source: String,
    val label: String,
    /** What the file turned out to hold — the only categories worth offering. */
    val availableScopes: Set<BackupScope>,
    val exportedAt: Instant?,
    val selectedScopes: Set<BackupScope>,
    val mode: ImportMode = ImportMode.MERGE,
)

data class BackupUiState(
    val settings: BackupSettings = BackupSettings(),
    /** What a manual export writes. Starts as everything; independent of the automatic selection. */
    val exportScopes: Set<BackupScope> = BackupScope.entries.toSet(),
    val files: List<BackupFile> = emptyList(),
    val pendingImport: PendingImport? = null,
    val status: BackupStatus = BackupStatus.Idle,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val settingsRepository: BackupSettingsRepository,
    private val backupRepository: BackupRepository,
    private val fileStore: BackupFileStore,
    private val autoBackupRunner: AutoBackupRunner,
) : ViewModel() {
    private val localState = MutableStateFlow(BackupUiState())

    val uiState: StateFlow<BackupUiState> = combine(
        settingsRepository.settings,
        localState,
    ) { settings, local -> local.copy(settings = settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupUiState())

    init {
        refreshFiles()
    }

    // --- Umfang -------------------------------------------------------------------------------

    fun toggleExportScope(scope: BackupScope, selected: Boolean) {
        localState.update { state ->
            state.copy(exportScopes = state.exportScopes.toggled(scope, selected))
        }
    }

    fun toggleAutoScope(scope: BackupScope, selected: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.current().autoScopes
            settingsRepository.setAutoScopes(current.toggled(scope, selected))
        }
    }

    // --- Speicherort, Automatik, Aufbewahrung -------------------------------------------------

    fun setFolder(folder: Uri) {
        viewModelScope.launch {
            runCatching {
                val previous = settingsRepository.current().folderUri
                withContext(Dispatchers.IO) { fileStore.persistFolderPermission(folder, previous) }
                val label = withContext(Dispatchers.IO) { fileStore.folderLabel(folder) }
                settingsRepository.setFolder(folder.toString(), label)
            }.onSuccess {
                setStatus(BackupStatus.Succeeded("Speicherort gesetzt."))
                refreshFiles()
            }.onFailure {
                setStatus(BackupStatus.Failed(it.message ?: "Ordner konnte nicht übernommen werden"))
            }
        }
    }

    fun clearFolder() {
        viewModelScope.launch {
            settingsRepository.setFolder(null, null)
            localState.update { it.copy(files = emptyList()) }
            setStatus(BackupStatus.Succeeded("Speicherort entfernt. Automatische Backups sind damit aus."))
        }
    }

    fun setInterval(interval: BackupInterval) {
        viewModelScope.launch { settingsRepository.setInterval(interval) }
    }

    fun setRetention(retention: BackupRetention) {
        viewModelScope.launch { settingsRepository.setRetention(retention) }
    }

    fun setKeepCount(count: Int) {
        viewModelScope.launch { settingsRepository.setKeepCount(count) }
    }

    // --- Sichern ------------------------------------------------------------------------------

    /**
     * "Jetzt sichern": into the configured folder, under the configured retention rule.
     *
     * The selection comes off [localState] rather than off [uiState], here and below: [uiState] only
     * tracks the flows while the screen is subscribed, so its `.value` falls back to the initial
     * state at exactly the moments a button press might outlive the screen.
     */
    fun backupNow() {
        val scopes = localState.value.exportScopes
        viewModelScope.launch {
            setStatus(BackupStatus.Running)
            when (val outcome = autoBackupRunner.backupNow(scopes)) {
                is AutoBackupOutcome.Written -> {
                    val pruned = outcome.prunedCount
                    setStatus(
                        BackupStatus.Succeeded(
                            "Gesichert als ${outcome.fileName}." +
                                if (pruned > 0) " $pruned ältere gelöscht." else "",
                        ),
                    )
                    refreshFiles()
                }
                is AutoBackupOutcome.Failed -> setStatus(BackupStatus.Failed(outcome.message))
                AutoBackupOutcome.NotConfigured -> setStatus(
                    BackupStatus.Failed("Kein Speicherort gewählt oder nichts ausgewählt."),
                )
                AutoBackupOutcome.NotDue -> Unit
            }
        }
    }

    /** "Exportieren…": into a file the user names themselves, outside the backup folder. */
    fun exportTo(write: (String) -> Unit) {
        val scopes = localState.value.exportScopes
        viewModelScope.launch {
            setStatus(BackupStatus.Running)
            runCatching {
                require(scopes.isNotEmpty()) { "Nichts ausgewählt" }
                val jsonText = backupRepository.exportToJson(scopes)
                withContext(Dispatchers.IO) { write(jsonText) }
            }.onSuccess {
                setStatus(BackupStatus.Succeeded("Export erfolgreich."))
            }.onFailure {
                setStatus(BackupStatus.Failed(it.message ?: "Export fehlgeschlagen"))
            }
        }
    }

    // --- Wiederherstellen ---------------------------------------------------------------------

    /**
     * Reads a file's envelope and parks it in [BackupUiState.pendingImport]. Nothing is written yet:
     * what the file holds decides which categories are even worth offering, and the choice of merge
     * or replace is the user's.
     */
    fun prepareImport(source: String, label: String, read: () -> String?) {
        viewModelScope.launch {
            runCatching {
                val text = requireNotNull(withContext(Dispatchers.IO) { read() }) {
                    "Datei konnte nicht gelesen werden"
                }
                val info = backupRepository.readInfo(text)
                require(info.scopes.isNotEmpty()) { "Die Datei enthält keine bekannten Daten" }
                info
            }.onSuccess { info ->
                localState.update {
                    it.copy(
                        pendingImport = PendingImport(
                            source = source,
                            label = label,
                            availableScopes = info.scopes,
                            exportedAt = info.exportedAt,
                            selectedScopes = info.scopes,
                        ),
                        status = BackupStatus.Idle,
                    )
                }
            }.onFailure {
                setStatus(BackupStatus.Failed(it.message ?: "Datei konnte nicht gelesen werden"))
            }
        }
    }

    fun togglePendingScope(scope: BackupScope, selected: Boolean) {
        localState.update { state ->
            val pending = state.pendingImport ?: return@update state
            state.copy(pendingImport = pending.copy(selectedScopes = pending.selectedScopes.toggled(scope, selected)))
        }
    }

    fun setPendingMode(mode: ImportMode) {
        localState.update { state ->
            state.copy(pendingImport = state.pendingImport?.copy(mode = mode))
        }
    }

    fun cancelImport() {
        localState.update { it.copy(pendingImport = null) }
    }

    /**
     * Runs the parked import. The file is read a second time rather than kept in memory since the
     * dialog was opened: it is the version on disk at the moment of confirming that should land,
     * and a whole database's worth of JSON is not worth holding on to across a dialog.
     */
    fun confirmImport(readFile: (String) -> String?) {
        val pending = localState.value.pendingImport ?: return
        localState.update { it.copy(pendingImport = null) }
        viewModelScope.launch {
            setStatus(BackupStatus.Running)
            runCatching {
                require(pending.selectedScopes.isNotEmpty()) { "Nichts ausgewählt" }
                val text = requireNotNull(withContext(Dispatchers.IO) { readFile(pending.source) }) {
                    "Datei konnte nicht gelesen werden"
                }
                backupRepository.importFromJson(text, pending.selectedScopes, pending.mode)
            }.onSuccess {
                val what = pending.selectedScopes.joinToString { it.label }
                setStatus(BackupStatus.Succeeded("Wiederhergestellt: $what."))
            }.onFailure {
                setStatus(BackupStatus.Failed(it.message ?: "Import fehlgeschlagen"))
            }
        }
    }

    /** Reads one of the files listed from the backup folder — the restore path for a tapped row. */
    fun readBackupFile(uri: String): String? = runCatching { fileStore.read(uri) }.getOrNull()

    // --- Dateien im Ordner --------------------------------------------------------------------

    fun refreshFiles() {
        viewModelScope.launch {
            val folder = settingsRepository.current().folderUri ?: return@launch
            val files = withContext(Dispatchers.IO) {
                runCatching { fileStore.list(folder) }.getOrDefault(emptyList())
            }
            localState.update { it.copy(files = files) }
        }
    }

    fun deleteFile(file: BackupFile) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { fileStore.delete(file.uri) } }
                .onSuccess {
                    setStatus(BackupStatus.Succeeded("${file.name} gelöscht."))
                    refreshFiles()
                }
                .onFailure { setStatus(BackupStatus.Failed(it.message ?: "Löschen fehlgeschlagen")) }
        }
    }

    fun dismissStatus() = setStatus(BackupStatus.Idle)

    private fun setStatus(status: BackupStatus) {
        localState.update { it.copy(status = status) }
    }
}

private fun <T> Set<T>.toggled(value: T, selected: Boolean): Set<T> =
    if (selected) this + value else this - value
