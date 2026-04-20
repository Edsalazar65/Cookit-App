package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kos.android.proyecto.cookit.domain.model.Recipe

@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    favoriteRecipes: List<Recipe>
) {
    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    selected = false,
                    onClick = onNavigateBack
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, null) },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) },
                    selected = false,
                    onClick = { onNavigateToProfile()}
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.width(150.dp).height(40.dp),
                color = Color(0xFFFFCDD2),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Favorites", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(favoriteRecipes) { recipe ->
                    FavoriteRecipeCard(recipe)
                }
            }
        }
    }
}

@Composable
private fun FavoriteRecipeCard(recipe: Recipe) {
    Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.85f)
                .padding(bottom = 12.dp),
            color = Color(0xFFFFD180),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = recipe.name, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFD32F2F))
            }
        }
    }
}