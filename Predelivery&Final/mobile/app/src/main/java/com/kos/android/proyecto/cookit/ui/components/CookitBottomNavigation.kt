package com.kos.android.proyecto.cookit.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class CookitScreen {
    HOME, EXPLORE, FAVORITES, PROFILE
}

@Composable
fun CookitBottomNavigation(
    currentScreen: CookitScreen,
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            selected = currentScreen == CookitScreen.HOME,
            onClick = onNavigateToHome,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, null) },
            selected = currentScreen == CookitScreen.EXPLORE,
            onClick = onNavigateToExplore,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, null) },
            selected = currentScreen == CookitScreen.FAVORITES,
            onClick = onNavigateToFavorites,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, null) },
            selected = currentScreen == CookitScreen.PROFILE,
            onClick = onNavigateToProfile,
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
        )
    }
}
