package com.example.mytracker.core.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.ConfirmDeleteDialog
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun Instant.formatted(): String = TIMESTAMP.format(atZone(ZoneId.systemDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportTo { jsonText ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(jsonText.toByteArray()) }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.prepareImport(source = uri.toString(), label = uri.lastPathSegment ?: "Datei") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
            }
        }
    }

    var pendingFileDelete by remember { mutableStateOf<BackupFile?>(null) }

    val openTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? -> if (uri != null) viewModel.setFolder(uri) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.BACKUP.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Backup") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                ScopeCard(
                    title = "Umfang",
                    explanation = "Was gesichert und exportiert wird.",
                    selected = state.exportScopes,
                    onToggle = viewModel::toggleExportScope,
                )
            }
            item {
                ManualCard(
                    hasFolder = state.settings.folderUri != null,
                    onExport = { createDocumentLauncher.launch(defaultExportName()) },
                    onImport = { openDocumentLauncher.launch(arrayOf("application/json")) },
                    onBackupNow = viewModel::backupNow,
                )
            }
            item {
                FolderCard(
                    settings = state.settings,
                    onPick = { openTreeLauncher.launch(null) },
                    onClear = viewModel::clearFolder,
                )
            }
            item {
                ScheduleCard(
                    settings = state.settings,
                    onIntervalChange = viewModel::setInterval,
                    onAutoScopeToggle = viewModel::toggleAutoScope,
                )
            }
            item {
                RetentionCard(
                    settings = state.settings,
                    onRetentionChange = viewModel::setRetention,
                    onKeepCountChange = viewModel::setKeepCount,
                )
            }
            if (state.settings.folderUri != null) {
                item { FilesHeader(count = state.files.size, onRefresh = viewModel::refreshFiles) }
                items(state.files, key = { it.uri }) { file ->
                    BackupFileRow(
                        file = file,
                        onRestore = {
                            viewModel.prepareImport(source = file.uri, label = file.name) {
                                viewModel.readBackupFile(file.uri)
                            }
                        },
                        onDelete = { pendingFileDelete = file },
                    )
                }
            }
            item { StatusLine(status = state.status) }
        }
    }

    pendingFileDelete?.let { file ->
        ConfirmDeleteDialog(
            title = "Sicherung löschen?",
            // A backup is the last copy of everything else this app can delete, so this one says
            // plainly that there is no way back.
            text = "\"${file.name}\" wird endgültig aus dem Sicherungsordner entfernt.",
            onConfirm = { viewModel.deleteFile(file) },
            onDismiss = { pendingFileDelete = null },
        )
    }

    state.pendingImport?.let { pending ->
        ImportDialog(
            pending = pending,
            onToggleScope = viewModel::togglePendingScope,
            onModeChange = viewModel::setPendingMode,
            onDismiss = viewModel::cancelImport,
            onConfirm = {
                viewModel.confirmImport { source ->
                    // A file out of the backup folder is read through the store, which holds the
                    // folder's persisted grant; one the user just picked is read straight off the
                    // one-shot grant that came with the picker.
                    viewModel.readBackupFile(source)
                        ?: context.contentResolver.openInputStream(Uri.parse(source))?.use { stream ->
                            BufferedReader(InputStreamReader(stream)).readText()
                        }
                }
            },
        )
    }
}

private fun defaultExportName(): String =
    backupFileName(BackupRetention.KEEP_LAST, Instant.now())

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ScopeCard(
    title: String,
    explanation: String,
    selected: Set<BackupScope>,
    onToggle: (BackupScope, Boolean) -> Unit,
) {
    SectionCard(title) {
        Text(explanation, style = MaterialTheme.typography.bodySmall)
        BackupScope.entries.forEach { scope ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = scope in selected,
                    onCheckedChange = { checked -> onToggle(scope, checked) },
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(scope.label, style = MaterialTheme.typography.bodyLarge)
                    Text(scope.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ManualCard(
    hasFolder: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBackupNow: () -> Unit,
) {
    SectionCard("Manuell") {
        Button(onClick = onBackupNow, enabled = hasFolder, modifier = Modifier.fillMaxWidth()) {
            Text("Jetzt sichern")
        }
        if (!hasFolder) {
            Text(
                "Für „Jetzt sichern“ fehlt noch ein Speicherort.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Exportieren…")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Importieren…")
        }
    }
}

@Composable
private fun FolderCard(settings: BackupSettings, onPick: () -> Unit, onClear: () -> Unit) {
    SectionCard("Speicherort") {
        Text(
            settings.folderLabel ?: settings.folderUri ?: "Noch kein Ordner gewählt.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) { Text("Ordner wählen") }
            if (settings.folderUri != null) {
                TextButton(onClick = onClear) { Text("Entfernen") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCard(
    settings: BackupSettings,
    onIntervalChange: (BackupInterval) -> Unit,
    onAutoScopeToggle: (BackupScope, Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard("Automatisch sichern") {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                readOnly = true,
                value = settings.interval.label,
                onValueChange = {},
                label = { Text("Intervall") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BackupInterval.entries.forEach { interval ->
                    DropdownMenuItem(
                        text = { Text(interval.label) },
                        onClick = {
                            expanded = false
                            onIntervalChange(interval)
                        },
                    )
                }
            }
        }
        Text(
            "Geprüft wird beim Öffnen der App — wer sie eine Woche nicht startet, bekommt in " +
                "dieser Woche auch kein automatisches Backup.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Automatisch gesichert wird:", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackupScope.entries.forEach { scope ->
                FilterChip(
                    selected = scope in settings.autoScopes,
                    onClick = { onAutoScopeToggle(scope, scope !in settings.autoScopes) },
                    label = { Text(scope.label.substringBefore(",")) },
                )
            }
        }
        Text(
            settings.lastBackupAtEpochMillis
                ?.let { "Zuletzt gesichert: ${Instant.ofEpochMilli(it).formatted()}" }
                ?: "Noch nie automatisch gesichert.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RetentionCard(
    settings: BackupSettings,
    onRetentionChange: (BackupRetention) -> Unit,
    onKeepCountChange: (Int) -> Unit,
) {
    SectionCard("Aufbewahrung") {
        BackupRetention.entries.forEach { retention ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = settings.retention == retention,
                    onClick = { onRetentionChange(retention) },
                )
                Text(retention.label, modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (settings.retention == BackupRetention.KEEP_LAST) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Anzahl:")
                OutlinedButton(
                    onClick = { onKeepCountChange(settings.keepCount - 1) },
                    enabled = settings.keepCount > BackupSettings.MIN_KEEP_COUNT,
                ) { Text("−") }
                Text(
                    settings.keepCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                OutlinedButton(
                    onClick = { onKeepCountChange(settings.keepCount + 1) },
                    enabled = settings.keepCount < BackupSettings.MAX_KEEP_COUNT,
                ) { Text("+") }
            }
            Text(
                "Ältere Backups im Ordner werden nach jeder Sicherung gelöscht.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text(
                "Es gibt immer genau eine Datei, die jedes Mal neu geschrieben wird.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FilesHeader(count: Int, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Vorhandene Backups ($count)", style = MaterialTheme.typography.titleMedium)
        AssistChip(onClick = onRefresh, label = { Text("Aktualisieren") })
    }
}

@Composable
private fun BackupFileRow(file: BackupFile, onRestore: () -> Unit, onDelete: () -> Unit) {
    Column {
        ListItem(
            headlineContent = { Text(file.name) },
            supportingContent = {
                Text(
                    "${Instant.ofEpochMilli(file.lastModifiedEpochMillis).formatted()} · " +
                        "${(file.sizeBytes / 1024.0).roundToInt()} KB",
                )
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                }
            },
            modifier = Modifier.fillMaxWidth().clickable(onClick = onRestore),
        )
        HorizontalDivider()
    }
}

@Composable
private fun StatusLine(status: BackupStatus) {
    when (status) {
        BackupStatus.Idle -> Unit
        BackupStatus.Running -> Text("Läuft…", style = MaterialTheme.typography.bodyMedium)
        is BackupStatus.Succeeded -> Text(status.message, style = MaterialTheme.typography.bodyMedium)
        is BackupStatus.Failed -> Text(
            "Fehler: ${status.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ImportDialog(
    pending: PendingImport,
    onToggleScope: (BackupScope, Boolean) -> Unit,
    onModeChange: (ImportMode) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var confirmingReplace by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wiederherstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pending.label, style = MaterialTheme.typography.bodyMedium)
                pending.exportedAt?.let {
                    Text("Gesichert am ${it.formatted()}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Text("Diese Kategorien sind in der Datei:", style = MaterialTheme.typography.bodyMedium)
                pending.availableScopes.sorted().forEach { scope ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = scope in pending.selectedScopes,
                            onCheckedChange = { checked -> onToggleScope(scope, checked) },
                        )
                        Text(scope.label, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                HorizontalDivider()
                ImportMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = pending.mode == mode,
                            onClick = { onModeChange(mode) },
                        )
                        Text(mode.label, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(
                    when (pending.mode) {
                        ImportMode.MERGE ->
                            "Vorhandene Einträge bleiben, wie sie sind. Es kommt nur dazu, was fehlt."
                        ImportMode.REPLACE ->
                            "Die gewählten Kategorien werden vorher gelöscht. Was seit dieser " +
                                "Sicherung dazukam, ist danach weg."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pending.selectedScopes.isNotEmpty(),
                onClick = {
                    if (pending.mode == ImportMode.REPLACE) confirmingReplace = true else onConfirm()
                },
            ) {
                Text("Wiederherstellen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )

    if (confirmingReplace) {
        AlertDialog(
            onDismissRequest = { confirmingReplace = false },
            title = { Text("Wirklich ersetzen?") },
            text = {
                Text(
                    "Gelöscht wird: ${pending.selectedScopes.sorted().joinToString { it.label }}. " +
                        "Danach steht dort genau das, was in der Datei ist.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingReplace = false
                        onConfirm()
                    },
                ) {
                    Text("Löschen und ersetzen")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReplace = false }) { Text("Abbrechen") }
            },
        )
    }
}
