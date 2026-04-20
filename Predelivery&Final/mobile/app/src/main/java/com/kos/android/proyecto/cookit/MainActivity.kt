package com.kos.android.proyecto.cookit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kos.android.proyecto.cookit.domain.model.AuthState
import com.kos.android.proyecto.cookit.ui.screens.AuthScreen
import com.kos.android.proyecto.cookit.ui.screens.GeneratorScreen
import com.kos.android.proyecto.cookit.ui.screens.HomeScreen
import com.kos.android.proyecto.cookit.ui.screens.ProfileScreen
import com.kos.android.proyecto.cookit.ui.screens.RecipeDetailScreen
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

/**
 * Rutas de navegación
 */
object NavRoutes {
    const val AUTH = "auth"
    const val HOME = "home"

    const val FAVORITES = "favorites"
    const val GENERATOR = "generator"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"
    const val PROFILE = "profile"

    fun recipeDetail(recipeId: String) = "recipe_detail/$recipeId"
}

@Composable
fun AiChefNavigation() {
    val navController = rememberNavController()

    val viewModel: ChefViewModel = hiltViewModel()

    // Observar estado de autenticación
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Determinar destino inicial basado en estado de auth
    val startDestination = when (authState) {
        is AuthState.Authenticated -> NavRoutes.HOME
        is AuthState.Unauthenticated -> NavRoutes.AUTH
        else -> NavRoutes.AUTH
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Pantalla de autenticación
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

        // Pantalla principal con lista de recetas
        composable(NavRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGenerator = {
                    navController.navigate(NavRoutes.GENERATOR)
                },
                onNavigateToDetail = { recipeId ->
                    navController.navigate(NavRoutes.recipeDetail(recipeId))
                },
                onNavigateToFavorites = {
                    navController.navigate("favorites")
                },
                onNavigateToProfile = {
                    navController.navigate(NavRoutes.PROFILE)
                },
                onLogout = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.FAVORITES) {
            val recipes by viewModel.recipes.collectAsStateWithLifecycle()
//            val favoriteRecipes = recipes.filter { it.isFavorite }

//            FavoritesScreen(
//                onNavigateBack = { navController.navigate(NavRoutes.HOME) },
//                onNavigateToProfile = {
//                    navController.navigate(NavRoutes.PROFILE)
//                },
////                favoriteRecipes = favoriteRecipes
//            )
        }

        // Pantalla de generación de recetas con IA
        composable(NavRoutes.GENERATOR) {
            GeneratorScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate(NavRoutes.HOME)
                },
                onRecipeGenerated = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla de detalle de receta
        composable(NavRoutes.RECIPE_DETAIL) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            RecipeDetailScreen(
                viewModel = viewModel,
                recipeId = recipeId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        //Pantalla de perfil
        composable(NavRoutes.PROFILE) {

            ProfileScreen(
                onNavigateBack = {
                    navController.navigate(NavRoutes.HOME)
                },
                onLogout = {

                    viewModel.signOut()
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                },
                onNavigateToFavorites = {
                    navController.navigate(NavRoutes.FAVORITES)
                }
            )
        }
    }
}
