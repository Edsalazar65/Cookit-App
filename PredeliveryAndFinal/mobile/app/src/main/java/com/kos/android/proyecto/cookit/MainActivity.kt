package com.kos.android.proyecto.cookit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kos.android.proyecto.cookit.domain.model.AuthState
import com.kos.android.proyecto.cookit.ui.screens.AddRecipeScreen
import com.kos.android.proyecto.cookit.ui.screens.AuthScreen
import com.kos.android.proyecto.cookit.ui.screens.ExploreScreen
import com.kos.android.proyecto.cookit.ui.screens.FavoritesScreen
import com.kos.android.proyecto.cookit.ui.screens.HomeScreen
import com.kos.android.proyecto.cookit.ui.screens.ProfileScreen
import com.kos.android.proyecto.cookit.ui.screens.RecipeDetailScreen
import com.kos.android.proyecto.cookit.ui.screens.TrashScreen
import com.kos.android.proyecto.cookit.ui.theme.AiChefTheme
import com.kos.android.proyecto.cookit.ui.viewmodel.ChefViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiChefTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiChefNavigation()
                }
            }
        }
    }
}

object NavRoutes {
    const val AUTH = "auth"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val GENERATOR = "generator"
    const val ADD_RECIPE = "add_recipe"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val PROFILE = "profile"
    const val TRASH = "trash"

    fun recipeDetail(recipeId: String) = "recipe_detail/$recipeId"
}

@Composable
fun AiChefNavigation() {
    val navController = rememberNavController()
    val viewModel: ChefViewModel = hiltViewModel()

    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Este bloque asegura que el destino inicial sea dinámico pero estable
    val startDestination = remember(authState) {
        when (authState) {
            is AuthState.Authenticated -> NavRoutes.HOME
            is AuthState.Unauthenticated -> NavRoutes.AUTH
            else -> NavRoutes.AUTH
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.AUTH) {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGenerator = { navController.navigate(NavRoutes.GENERATOR) },
                onNavigateToDetail = { recipeId -> navController.navigate(NavRoutes.recipeDetail(recipeId)) },
                onNavigateToFavorites = { navController.navigate(NavRoutes.FAVORITES) },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                onNavigateToExplore = { navController.navigate(NavRoutes.EXPLORE) },
                onNavigateToAddRecipe = { navController.navigate(NavRoutes.ADD_RECIPE) },
                onLogout = {
                    viewModel.signOut()
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavRoutes.EXPLORE) {
            ExploreScreen(
                viewModel = viewModel,
                onNavigateToRecipeDetail = { recipeId -> navController.navigate(NavRoutes.recipeDetail(recipeId)) },
                onNavigateToFavorites = { navController.navigate(NavRoutes.FAVORITES) },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                onNavigateToHome = { navController.navigate(NavRoutes.HOME) },
                onNavigateToAddRecipe = { navController.navigate(NavRoutes.ADD_RECIPE) }
            )
        }

        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                onNavigateToExplore = { navController.navigate(NavRoutes.EXPLORE) },
                onNavigateToAddRecipe = { navController.navigate(NavRoutes.ADD_RECIPE) },
                onNavigateToDetail = { recipeId -> navController.navigate(NavRoutes.recipeDetail(recipeId)) }
            )
        }

        composable(NavRoutes.RECIPE_DETAIL) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            RecipeDetailScreen(
                viewModel = viewModel,
                recipeId = recipeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ADD_RECIPE) {
            AddRecipeScreen(
                viewModel = viewModel,
                onRecipeAdded = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToHome = { navController.navigate(NavRoutes.HOME) },
                onNavigateToExplore = { navController.navigate(NavRoutes.EXPLORE) },
                onNavigateToFavorites = { navController.navigate(NavRoutes.FAVORITES) },
                onNavigateToAddRecipe = { navController.navigate(NavRoutes.ADD_RECIPE) },
                onNavigateToTrash = { navController.navigate(NavRoutes.TRASH) },
                onLogout = {
                    viewModel.signOut()
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.TRASH) {
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
