package com.example.prokject2_tracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.prokject2_tracker.R
import kotlinx.serialization.Serializable

@Serializable
data object PlaceholderRoute

/**
 * Bottom-nav destinations. Kept to a max of 4 (Material3 guidance); each future stage appends its
 * own entry/entries here without touching [com.example.prokject2_tracker.core.ui.AppScaffold].
 */
enum class TopLevelDestination(
    val route: Any,
    val routeQualifiedName: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(
        route = PlaceholderRoute,
        routeQualifiedName = PlaceholderRoute::class.qualifiedName!!,
        labelRes = R.string.nav_home,
        icon = Icons.Filled.Home,
    ),
}
