package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    onNavigateToExplore: () -> Unit,
    onLogout: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterByInventory by viewModel.filterByInventory.collectAsStateWithLifecycle()

    var showAddIngredientDialog by remember { mutableStateOf(false) }
    var newIngredientName by remember { mutableStateOf("") }

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
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)),
                placeholder = { Text("Search my recipes") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    FilterChip(
                        selected = filterByInventory,
                        onClick = { viewModel.onToggleInventoryFilter() },
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

            Spacer(modifier = Modifier.height(16.dp))

            // CRUD Inventario
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Ingredients", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                TextButton(onClick = { showAddIngredientDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (userData?.inventory?.isEmpty() == true) {
                    item {
                        Text("Add ingredients to filter your recipes", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                items(userData?.inventory ?: emptyList()) { ingredient ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.removeIngredient(ingredient) },
                        label = { Text(ingredient, fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = Color(0xFFFFD180).copy(alpha = 0.3f),
                            selectedLabelColor = Color(0xFF5D4037)
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No saved recipes found.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToExplore,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                        ) {
                            Text("Explore Recipes")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
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

    if (showAddIngredientDialog) {
        AlertDialog(
            onDismissRequest = { showAddIngredientDialog = false },
            title = { Text("Add Ingredient") },
            text = {
                TextField(
                    value = newIngredientName,
                    onValueChange = { newIngredientName = it },
                    placeholder = { Text("e.g. Tomato") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addIngredient(newIngredientName)
                    newIngredientName = ""
                    showAddIngredientDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIngredientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
