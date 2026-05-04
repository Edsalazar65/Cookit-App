package com.kos.android.proyecto.cookit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.ui.components.NotificationHost
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: ChefViewModel,
    onNavigateBack: () -> Unit
) {
    val trashedRecipes by viewModel.trashedRecipes.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    
    var recipeToDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFFFDF5E6),
        topBar = {
            TopAppBar(
                title = { Text("Trash Bin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (trashedRecipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trash is empty", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(trashedRecipes) { recipe ->
                        TrashedRecipeCard(
                            recipe = recipe,
                            onRestore = { viewModel.restoreFromTrash(recipe) },
                            onDeletePermanent = { recipeToDeleteId = recipe.id }
                        )
                    }
                }
            }
            
            NotificationHost(notifications = notifications)
        }
    }

    if (recipeToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { recipeToDeleteId = null },
            title = { Text("Permanent Delete") },
            text = { Text("Are you sure? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = recipeToDeleteId
                    if (id != null) {
                        viewModel.permanentDelete(id)
                    }
                    recipeToDeleteId = null
                }) {
                    Text("Delete Forever", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrashedRecipeCard(
    recipe: Recipe,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF5D4037))
                Text("${recipe.ingredients.size} ingredients", fontSize = 12.sp, color = Color.Gray)
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restaurar", tint = Color(0xFF388E3C))
                }
                IconButton(onClick = onDeletePermanent) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar permanentemente", tint = Color.Red)
                }
            }
        }
    }
}
