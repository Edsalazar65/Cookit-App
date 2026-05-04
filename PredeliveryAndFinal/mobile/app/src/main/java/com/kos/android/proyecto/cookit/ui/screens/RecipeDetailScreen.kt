package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    viewModel: ChefViewModel,
    recipeId: String,
    onNavigateBack: () -> Unit
) {
    val publicRecipes by viewModel.publicRecipes.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    val recipe = publicRecipes.find { it.id == recipeId }

    Scaffold(
        containerColor = Color(0xFFFDF5E6)
    ) { paddingValues ->
        if (recipe == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF388E3C))
            }
        } else {
            val isFavorite = userData?.favorites?.contains(recipe.id) == true
            val isSaved = userData?.myRecipes?.contains(recipe.id) == true

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                // Header con Imagen y Botón Volver
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AsyncImage(
                        model = recipe.imageURL,
                        contentDescription = recipe.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Botón Volver (Circular y con contraste)
                    Surface(
                        modifier = Modifier.padding(16.dp).size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.8f),
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(8.dp),
                            tint = Color.Black
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Título y Dificultad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recipe.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = Color(0xFF5D4037)
                            )
                            Surface(
                                color = Color(0xFFFFD180),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = recipe.difficulty.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }

                        // Botones de acción
                        Row {
                            IconButton(onClick = { viewModel.toggleFavorite(recipe.id) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFD32F2F) else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.toggleSaveRecipe(recipe.id) }) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Save",
                                    tint = if (isSaved) Color(0xFF388E3C) else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Sección Ingredientes
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF388E3C),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = ingredient, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Sección Pasos
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    recipe.steps.forEachIndexed { index, step ->
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = CircleShape,
                                color = Color(0xFFFFD180)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF5D4037)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = step,
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
