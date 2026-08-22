package com.example.mytracker.core.backup

/**
 * The three halves the user thinks of their data in, and the unit both export and import are
 * selected by. Every [BackupExportProvider] belongs to exactly one of them.
 *
 * The split is about what it costs to lose something, not about which table it lives in:
 * [LIBRARY] is what was typed once and reused daily, [DAILY_ENTRIES] is what can never be
 * reconstructed, [SETTINGS] is the handful of preferences that make the app fit its user.
 */
enum class BackupScope(val label: String, val description: String) {
    SETTINGS(
        label = "Einstellungen",
        description = "Einheiten und die Backup-Einstellungen selbst.",
    ),
    DAILY_ENTRIES(
        label = "Tägliche Einträge",
        description = "Tagebuch, Flüssigkeiten, Training, Gewicht, Maße, Blutdruck, Schlaf, " +
            "Habit-Check-ins und erledigte Aufgaben.",
    ),
    LIBRARY(
        label = "Bibliothek, Übungen & Ziele",
        description = "Lebensmittel, Rezepte, Getränkearten, Übungen, Habits, Aufgaben und alle Ziele.",
    ),
}
