package com.example.prokject2_tracker.nutrition.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.backup.LibraryBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object ExportSucceeded : BackupStatus
    data object ImportSucceeded : BackupStatus
    data class Failed(val message: String) : BackupStatus
}

@HiltViewModel
class LibraryBackupViewModel @Inject constructor(
    private val libraryBackupRepository: LibraryBackupRepository,
) : ViewModel() {
    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    fun exportTo(write: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val jsonText = libraryBackupRepository.exportToJson()
                withContext(Dispatchers.IO) { write(jsonText) }
            }.onSuccess {
                _status.value = BackupStatus.ExportSucceeded
            }.onFailure {
                _status.value = BackupStatus.Failed(it.message ?: "Export fehlgeschlagen")
            }
        }
    }

    fun importFrom(read: () -> String?) {
        viewModelScope.launch {
            runCatching {
                val jsonText = requireNotNull(withContext(Dispatchers.IO) { read() }) {
                    "Datei konnte nicht gelesen werden"
                }
                libraryBackupRepository.importFromJson(jsonText)
            }.onSuccess {
                _status.value = BackupStatus.ImportSucceeded
            }.onFailure {
                _status.value = BackupStatus.Failed(it.message ?: "Import fehlgeschlagen")
            }
        }
    }

    fun resetStatus() {
        _status.value = BackupStatus.Idle
    }
}
