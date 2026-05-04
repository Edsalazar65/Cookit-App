package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.ui.components.CookitBottomNavigation
import com.kos.android.proyecto.cookit.ui.components.CookitScreen
import com.kos.android.proyecto.cookit.ui.components.NotificationHost
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ChefViewModel,
    onNavigateToRecipeDetail: (String) -> Unit,
    // Agregamos funciones de navegación para la barra inferior
    onNavigateToHome: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddRecipe: () -> Unit
) {
    val recipes by viewModel.filteredPublicRecipes.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val searchQuery by viewModel.exploreSearchQuery.collectAsStateWithLifecycle()
    val filterByInventory by viewModel.filterExploreByInventory.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Discover Recipes", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            CookitBottomNavigation(
                currentScreen = CookitScreen.EXPLORE,
                onNavigateToHome = onNavigateToHome,
                onNavigateToExplore = { },
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToAddRecipe = onNavigateToAddRecipe
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onExploreSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp)),
                placeholder = { Text("Search all recipes") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    FilterChip(
                        selected = filterByInventory,
                        onClick = { viewModel.onToggleExploreInventoryFilter() },
                        label = { Text("Inventory", fontSize = 10.sp) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            if (recipes.isEmpty()) {
                // Mensaje que se muestra si no hay recetas para dibujar
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay recetas disponibles.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
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
                            onSaveClick = { r -> viewModel.toggleSaveRecipe(r.id) },
                            onDeleteClick = if (isAdmin) { r -> recipeToDelete = r } else null
                        )
                    }
                }
            }
        }
    }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to move this recipe to trash bin?") },
            confirmButton = {
                TextButton(onClick = {
                    val r = recipeToDelete
                    if (r != null) {
                        viewModel.deleteRecipe(r)
                    }
                    recipeToDelete = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    NotificationHost(notifications = notifications)
}
