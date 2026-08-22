package com.example.mytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.mytracker.core.backup.AutoBackupRunner
import com.example.mytracker.core.ui.AppScaffold
import com.example.mytracker.ui.theme.MyTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var autoBackupRunner: AutoBackupRunner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyTrackerTheme {
                AppScaffold()
            }
        }
    }

    /**
     * The automatic backup's only trigger. With no background scheduler in the app, "täglich" can
     * only mean "the first time the app is opened on a new day" — [AutoBackupRunner.runIfDue] does
     * nothing when nothing is due, so running it on every return to the foreground costs a settings
     * read and is what keeps the schedule from drifting a whole period on a late start.
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { autoBackupRunner.runIfDue() }
    }
}
