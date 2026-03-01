package com.afhara.mywebsocket.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: Screen,
    val label: String,
    val icon: ImageVector,
) {
    companion object {
        val items = listOf(
            BottomNavItem(
                route = Screen.Dashboard,
                label = "Dashboard",
                icon = Icons.Default.Home,
            ),
            BottomNavItem(
                route = Screen.CryptoList,
                label = "Market",
                icon = Icons.Default.List,
            ),
        )
    }
}