package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.ui.components.CookitBottomNavigation
import com.kos.android.proyecto.cookit.ui.components.CookitScreen
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ChefViewModel,
    onNavigateToRecipeDetail: (String) -> Unit,
    // Agregamos funciones de navegación para la barra inferior
    onNavigateToHome: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val recipes by viewModel.publicRecipes.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        topBar = {
            TopAppBar(
                title = { Text("Explorar Recetas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            CookitBottomNavigation(
                currentScreen = CookitScreen.EXPLORE,
                onNavigateToHome = onNavigateToHome,
                onNavigateToExplore = { },
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        if (recipes.isEmpty()) {
            // Mensaje que se muestra si no hay recetas para dibujar
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay recetas disponibles.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(recipes) { recipe ->
                    val isFavorite = userData?.favorites?.contains(recipe.id) == true
                    val isSaved = userData?.myRecipes?.contains(recipe.id) == true

                    RecipeCard(
                        recipe = recipe,
                        isFavorite = isFavorite,
                        isSaved = isSaved,
                        onRecipeClick = { id -> onNavigateToRecipeDetail(id) },
                        onFavoriteClick = { r -> viewModel.toggleFavorite(r.id) },
                        onSaveClick = { r -> viewModel.toggleSaveRecipe(r.id) }
                    )
                }
            }
        }
    }
}