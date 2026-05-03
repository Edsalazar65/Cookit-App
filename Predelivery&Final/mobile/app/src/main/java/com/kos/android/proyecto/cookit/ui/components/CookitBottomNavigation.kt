package com.kos.android.proyecto.cookit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class CookitScreen {
    HOME, EXPLORE, FAVORITES, PROFILE, ADD_RECIPE
}

@Composable
fun CookitBottomNavigation(
    currentScreen: CookitScreen,
    onNavigateToHome: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddRecipe: () -> Unit
) {
    Box {
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
            
            // Spacer for the center button
            NavigationBarItem(
                icon = { },
                selected = false,
                onClick = { },
                enabled = false
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

        // Central Add Button
        FloatingActionButton(
            onClick = onNavigateToAddRecipe,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = Color(0xFF388E3C),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Recipe", modifier = Modifier.size(32.dp))
        }
    }
}
