package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
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
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChefViewModel,
    onNavigateToGenerator: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToExplore: () -> Unit, // 1. AGREGAMOS EL NUEVO PARÁMETRO AQUÍ
    onLogout: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        topBar = {
            TopAppBar(
                title = { Text("Main Menu", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = {
                        viewModel.signOut()
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).build()
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut()

                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", tint = Color.Black)
                    }
                }
            )
        },
        bottomBar = {
            CookitBottomNavigation(
                currentScreen = CookitScreen.HOME,
                onNavigateToHome = { },
                onNavigateToExplore = onNavigateToExplore,
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToProfile = onNavigateToProfile
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = Color(0xFFA6A6A6),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text("🐭", fontSize = 28.sp)
            }
        }


    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)),
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFE0E0E0),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp), // Espacio lateral para que la sombra no se corte
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),

                userScrollEnabled = true
            ) {
                items(items = recipes, key = { it.id }) { recipe ->
                    val isFavorite = userData?.favorites?.contains(recipe.id) == true
                    val isSaved = userData?.myRecipes?.contains(recipe.id) == true

                    RecipeCard(
                        recipe = recipe,
                        isFavorite = isFavorite,
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
