package com.curso.android.module5.aichef.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.curso.android.module5.aichef.domain.model.Recipe
import com.curso.android.module5.aichef.ui.viewmodel.ChefViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChefViewModel,
    onNavigateToGenerator: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onLogout: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        topBar = {
            TopAppBar(
                title = { Text("Main Menu", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { viewModel.signOut(); onLogout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", tint = Color.Black)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF388E3C))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, null) },
                    selected = false,
                    onClick = onNavigateToFavorites
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) },
                    selected = false,
                    onClick = {onNavigateToProfile() }
                )
            }
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
                    RecipeCardItem(
                        recipe = recipe,
                        onClick = { onNavigateToDetail(recipe.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeCardItem(
    recipe: Recipe,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {

        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(end = 12.dp), // Margen para que el ratón no tape el borde
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 20.dp),
                    color = Color(0xFFFFD180).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                ) {
                    Text(
                        text = recipe.title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(54.dp)
                .offset(x = (0).dp, y = (8).dp),
            shape = CircleShape,
            color = Color(0xFF000000),
            shadowElevation = 8.dp,
            onClick = { /* Acción chat */ }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🐭", fontSize = 28.sp)
            }
        }
    }
}