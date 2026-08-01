package com.example.prokject2_tracker.nutrition.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBackupScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryBackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()

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
            viewModel.importFrom {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Export / Import") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Exportiert oder importiert nur Bibliotheksdaten — Lebensmittel, Rezepte, " +
                    "Getränkearten, Maßeinheiten, Schnellauswahl, Übungen und Muskelgruppen. " +
                    "Keine Tagebuch-Einträge und keine geloggten Werte.",
            )
            Button(
                onClick = { createDocumentLauncher.launch("bibliothek-export.json") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Exportieren")
            }
            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Importieren")
            }
            when (val current = status) {
                is BackupStatus.ExportSucceeded -> Text("Export erfolgreich.")
                is BackupStatus.ImportSucceeded -> Text("Import erfolgreich.")
                is BackupStatus.Failed -> Text("Fehler: ${current.message}")
                BackupStatus.Idle -> Unit
            }
        }
    }
}
