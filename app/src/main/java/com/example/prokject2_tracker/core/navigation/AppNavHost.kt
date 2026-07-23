package com.example.prokject2_tracker.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = PlaceholderRoute,
        modifier = modifier,
    ) {
        composable<PlaceholderRoute> {
            PlaceholderScreen()
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Text(
        text = "Prokject2 Tracker",
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
    )
}
