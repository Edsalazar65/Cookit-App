package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.ui.components.CookitBottomNavigation
import com.kos.android.proyecto.cookit.ui.components.CookitScreen
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@Composable
fun FavoritesScreen(
    viewModel: ChefViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val favoriteRecipes by viewModel.favoriteRecipes.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        bottomBar = {
            CookitBottomNavigation(
                currentScreen = CookitScreen.FAVORITES,
                onNavigateToHome = onNavigateBack,
                onNavigateToExplore = onNavigateToExplore,
                onNavigateToFavorites = { },
                onNavigateToProfile = onNavigateToProfile
            )
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

            if (favoriteRecipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes recetas favoritas aún.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(favoriteRecipes) { recipe ->
                        val isSaved = userData?.myRecipes?.contains(recipe.id) == true
                        RecipeCard(
                            recipe = recipe,
                            isFavorite = true,
                            isSaved = isSaved,
                            onRecipeClick = { id -> onNavigateToDetail(id) },
                            onFavoriteClick = { r -> viewModel.toggleFavorite(r.id) },
                            onSaveClick = { r -> viewModel.toggleSaveRecipe(r.id) }
                        )
                    }
                }
            }
        }
    }
}

