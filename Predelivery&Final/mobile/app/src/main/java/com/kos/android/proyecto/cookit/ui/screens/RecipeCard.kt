package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.R

@Composable
fun RecipeCard(
    recipe: Recipe,
    isFavorite: Boolean = false,
    isSaved: Boolean = false,
    onRecipeClick: (String) -> Unit,
    onFavoriteClick: (Recipe) -> Unit,
    onSaveClick: (Recipe) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onRecipeClick(recipe.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Imagen de la receta
            AsyncImage(
                model = recipe.imageURL,
                contentDescription = "Imagen de ${recipe.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Image),
                error = rememberVectorPainter(Icons.Default.Warning)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Título
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mostrar hasta 5 ingredientes
                Text(
                    text = "Ingredientes principales:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                val topIngredients = recipe.ingredients.take(5)
                topIngredients.forEach { ingredient ->
                    Text(
                        text = "• $ingredient",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onFavoriteClick(recipe) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar de Favoritos" else "Agregar a Favoritos",
                            tint = if (isFavorite) Color(0xFFD32F2F) else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { onSaveClick(recipe) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Quitar de Guardados" else "Guardar Receta",
                            tint = if (isSaved) Color(0xFF388E3C) else LocalContentColor.current
                        )
                    }
                }
            }
        }
    }
}