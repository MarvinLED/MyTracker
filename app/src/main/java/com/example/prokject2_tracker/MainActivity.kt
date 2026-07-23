package com.example.prokject2_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.prokject2_tracker.core.ui.AppScaffold
import com.example.prokject2_tracker.ui.theme.Prokject2_TrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Prokject2_TrackerTheme {
                AppScaffold()
            }
        }
    }
}
