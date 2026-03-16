package com.curso.android.module5.aichef

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
import com.curso.android.module5.aichef.domain.model.AuthState
import com.curso.android.module5.aichef.ui.screens.AuthScreen
import com.curso.android.module5.aichef.ui.screens.GeneratorScreen
import com.curso.android.module5.aichef.ui.screens.HomeScreen
import com.curso.android.module5.aichef.ui.screens.RecipeDetailScreen
import com.curso.android.module5.aichef.ui.theme.AiChefTheme
import com.curso.android.module5.aichef.ui.viewmodel.ChefViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * =============================================================================
 * MainActivity - Activity principal con navegación protegida
 * =============================================================================
 *
 * CONCEPTO: @AndroidEntryPoint
 * Esta anotación es OBLIGATORIA para Activities/Fragments que usen Hilt.
 * Permite que Hilt inyecte dependencias en la Activity y sus Composables.
 *
 * CONCEPTO: AuthGuard en Navigation Compose
 * La navegación debe ser "protegida" - si el usuario no está autenticado,
 * debe ser redirigido a la pantalla de login.
 *
 * Hay dos enfoques principales:
 *
 * 1. Decisión en la navegación inicial (usado aquí):
 *    - Observar el estado de auth
 *    - Decidir startDestination basado en si hay sesión
 *
 * 2. LaunchedEffect en cada pantalla:
 *    - Cada pantalla verifica auth y redirige si es necesario
 *    - Más código pero más control
 *
 * CONCEPTO: ViewModel compartido
 * El ChefViewModel se crea a nivel de Navigation para ser compartido
 * entre todas las pantallas. Esto permite:
 * - Mantener estado consistente (auth, recetas)
 * - Evitar recrear el ViewModel en cada navegación
 * - Compartir datos entre pantallas sin argumentos
 *
 * =============================================================================
 */
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
    const val GENERATOR = "generator"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}"

    fun recipeDetail(recipeId: String) = "recipe_detail/$recipeId"
}

/**
 * Configuración de navegación con AuthGuard
 *
 * FLUJO DE NAVEGACIÓN:
 *
 * ┌─────────────┐
 * │  AuthState  │
 * └──────┬──────┘
 *        │
 *        ▼
 * ┌──────────────────┐     ┌──────────────────┐
 * │  Unauthenticated │────▶│    AuthScreen    │
 * └──────────────────┘     └────────┬─────────┘
 *                                   │ Login/Signup success
 *                                   ▼
 * ┌──────────────────┐     ┌──────────────────┐
 * │  Authenticated   │────▶│    HomeScreen    │
 * └──────────────────┘     └────────┬─────────┘
 *                                   │ FAB click
 *                                   ▼
 *                          ┌──────────────────┐
 *                          │ GeneratorScreen  │
 *                          └────────┬─────────┘
 *                                   │ Recipe generated
 *                                   ▼
 *                          ┌──────────────────┐
 *                          │    HomeScreen    │
 *                          └──────────────────┘
 */
@Composable
fun AiChefNavigation() {
    val navController = rememberNavController()

    // ViewModel compartido entre todas las pantallas
    // hiltViewModel() obtiene el ViewModel del grafo de Hilt
    val viewModel: ChefViewModel = hiltViewModel()

    // Observar estado de autenticación
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Determinar destino inicial basado en estado de auth
    val startDestination = when (authState) {
        is AuthState.Authenticated -> NavRoutes.HOME
        is AuthState.Unauthenticated -> NavRoutes.AUTH
        else -> NavRoutes.AUTH // Loading o Error -> mostrar auth
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
                    // Navegar a Home y limpiar el back stack
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
                onLogout = {
                    // Navegar a Auth y limpiar el back stack
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de generación de recetas con IA
        composable(NavRoutes.GENERATOR) {
            GeneratorScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRecipeGenerated = {
                    // Volver al Home después de generar
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
    }
}
