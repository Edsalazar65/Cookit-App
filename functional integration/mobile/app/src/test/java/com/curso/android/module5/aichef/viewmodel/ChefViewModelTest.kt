package com.curso.android.module5.aichef.viewmodel

import android.graphics.Bitmap
import app.cash.turbine.test
import com.curso.android.module5.aichef.data.firebase.IAuthRepository
import com.curso.android.module5.aichef.data.firebase.IFirestoreRepository
import com.curso.android.module5.aichef.data.firebase.IStorageRepository
import com.curso.android.module5.aichef.data.remote.GeneratedRecipe
import com.curso.android.module5.aichef.data.remote.IAiLogicDataSource
import com.curso.android.module5.aichef.domain.model.AuthState
import com.curso.android.module5.aichef.domain.model.Recipe
import com.curso.android.module5.aichef.domain.model.UiState
import com.curso.android.module5.aichef.ui.viewmodel.ChefViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * =============================================================================
 * CHEF VIEW MODEL TESTS - Tests unitarios para ChefViewModel
 * =============================================================================
 *
 * CONCEPTO: Testing de ViewModels con Mocks
 * El ViewModel depende de interfaces (IAuthRepository, etc.), lo que permite
 * inyectar mocks en los tests. Esto significa:
 * - No necesitamos Firebase real
 * - Tests rápidos y determinísticos
 * - Podemos simular cualquier escenario
 *
 * CONCEPTO: MockK
 * MockK es la librería de mocking más popular para Kotlin.
 * Ventajas sobre Mockito:
 * - Soporte nativo para coroutines (coEvery, coVerify)
 * - Sintaxis más idiomática de Kotlin
 * - Mejor soporte para data classes y sealed classes
 *
 * CONCEPTO: Turbine
 * Turbine es una librería para testing de Flows.
 * Permite verificar emisiones de manera secuencial:
 * ```kotlin
 * flow.test {
 *     assertEquals(expected1, awaitItem())
 *     assertEquals(expected2, awaitItem())
 *     cancelAndIgnoreRemainingEvents()
 * }
 * ```
 *
 * =============================================================================
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChefViewModelTest {

    // Mocks de dependencias
    private lateinit var authRepository: IAuthRepository
    private lateinit var firestoreRepository: IFirestoreRepository
    private lateinit var storageRepository: IStorageRepository
    private lateinit var aiLogicDataSource: IAiLogicDataSource

    // Subject under test
    private lateinit var viewModel: ChefViewModel

    // Test dispatcher para coroutines
    private val testDispatcher = StandardTestDispatcher()

    /**
     * CONCEPTO: @Before
     * Se ejecuta antes de cada test, configurando el estado inicial.
     * Aquí creamos los mocks y configuramos el Main dispatcher.
     */
    @Before
    fun setup() {
        // Configurar Main dispatcher para tests
        Dispatchers.setMain(testDispatcher)

        // Crear mocks
        authRepository = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)
        storageRepository = mockk(relaxed = true)
        aiLogicDataSource = mockk(relaxed = true)

        // Configurar comportamiento por defecto
        every { authRepository.observeAuthState() } returns flowOf(AuthState.Unauthenticated)
        every { firestoreRepository.observeUserRecipes(any()) } returns flowOf(emptyList())
    }

    /**
     * CONCEPTO: @After
     * Se ejecuta después de cada test, limpiando el estado.
     * Importante para evitar que tests se afecten entre sí.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper para crear el ViewModel con los mocks actuales.
     * Se llama en cada test después de configurar comportamientos específicos.
     */
    private fun createViewModel(): ChefViewModel {
        return ChefViewModel(
            authRepository = authRepository,
            firestoreRepository = firestoreRepository,
            storageRepository = storageRepository,
            aiLogicDataSource = aiLogicDataSource
        )
    }

    // =========================================================================
    // TESTS DE AUTENTICACIÓN
    // =========================================================================

    /**
     * Test: Estado inicial es Unauthenticated
     */
    @Test
    fun `authState emits Unauthenticated when user is not logged in`() = runTest {
        // Given
        every { authRepository.observeAuthState() } returns flowOf(AuthState.Unauthenticated)

        // When
        viewModel = createViewModel()

        // Then
        viewModel.authState.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Estado Authenticated cuando hay usuario
     */
    @Test
    fun `authState emits Authenticated when user is logged in`() = runTest {
        // Given
        val userId = "test-user-123"
        every { authRepository.observeAuthState() } returns flowOf(AuthState.Authenticated(userId))

        // When
        viewModel = createViewModel()

        // Then
        viewModel.authState.test {
            val state = awaitItem()
            assertTrue(state is AuthState.Authenticated)
            assertEquals(userId, (state as AuthState.Authenticated).userId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Sign In exitoso
     */
    @Test
    fun `signIn updates authUiState to Success on successful login`() = runTest {
        // Given
        coEvery { authRepository.signIn(any(), any()) } returns Result.success("user-123")
        viewModel = createViewModel()

        // When
        viewModel.signIn("test@example.com", "password123")
        advanceUntilIdle()

        // Then
        viewModel.authUiState.test {
            val state = awaitItem()
            assertTrue("Expected Success but got $state", state is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { authRepository.signIn("test@example.com", "password123") }
    }

    /**
     * Test: Sign In fallido
     */
    @Test
    fun `signIn updates authUiState to Error on failed login`() = runTest {
        // Given
        val errorMessage = "Invalid credentials"
        coEvery { authRepository.signIn(any(), any()) } returns Result.failure(Exception(errorMessage))
        viewModel = createViewModel()

        // When
        viewModel.signIn("test@example.com", "wrongpassword")
        advanceUntilIdle()

        // Then
        viewModel.authUiState.test {
            val state = awaitItem()
            assertTrue("Expected Error but got $state", state is UiState.Error)
            assertEquals(errorMessage, (state as UiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Sign Up exitoso
     */
    @Test
    fun `signUp updates authUiState to Success on successful registration`() = runTest {
        // Given
        coEvery { authRepository.signUp(any(), any()) } returns Result.success("new-user-123")
        viewModel = createViewModel()

        // When
        viewModel.signUp("new@example.com", "password123")
        advanceUntilIdle()

        // Then
        viewModel.authUiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Sign Out limpia estado
     */
    @Test
    fun `signOut calls repository and resets authUiState`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.signOut()

        // Then
        verify { authRepository.signOut() }
        assertEquals(UiState.Idle, viewModel.authUiState.value)
    }

    // =========================================================================
    // TESTS DE RECETAS
    // =========================================================================

    /**
     * Test: Lista de recetas vacía cuando no hay usuario
     */
    @Test
    fun `recipes emits empty list when user is not authenticated`() = runTest {
        // Given
        every { authRepository.observeAuthState() } returns flowOf(AuthState.Unauthenticated)

        // When
        viewModel = createViewModel()

        // Then
        viewModel.recipes.test {
            assertEquals(emptyList<Recipe>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Lista de recetas del usuario autenticado
     */
    @Test
    fun `recipes emits user recipes when authenticated`() = runTest {
        // Given
        val userId = "test-user"
        val recipes = listOf(
            Recipe(id = "1", userId = userId, title = "Pasta", ingredients = listOf("pasta", "sauce")),
            Recipe(id = "2", userId = userId, title = "Salad", ingredients = listOf("lettuce", "tomato"))
        )
        every { authRepository.observeAuthState() } returns flowOf(AuthState.Authenticated(userId))
        every { firestoreRepository.observeUserRecipes(userId) } returns flowOf(recipes)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        viewModel.recipes.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Pasta", result[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TESTS DE GENERACIÓN DE RECETAS
    // =========================================================================

    /**
     * Test: Generación falla si no hay usuario autenticado
     */
    @Test
    fun `generateRecipe sets error when user is not authenticated`() = runTest {
        // Given
        every { authRepository.currentUserId } returns null
        viewModel = createViewModel()
        val bitmap = mockk<Bitmap>()

        // When
        viewModel.generateRecipe(bitmap)
        advanceUntilIdle()

        // Then
        viewModel.generationState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            assertEquals("Debes iniciar sesión", (state as UiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Generación exitosa guarda en Firestore
     */
    @Test
    fun `generateRecipe saves recipe to Firestore on success`() = runTest {
        // Given
        val userId = "test-user"
        val recipeId = "generated-recipe-123"
        val generatedRecipe = GeneratedRecipe(
            title = "Ensalada Fresca",
            ingredients = listOf("lechuga", "tomate", "pepino"),
            steps = listOf("Lavar", "Cortar", "Mezclar")
        )

        every { authRepository.currentUserId } returns userId
        coEvery { aiLogicDataSource.generateRecipeFromImage(any()) } returns generatedRecipe
        coEvery { firestoreRepository.saveRecipe(any()) } returns Result.success(recipeId)

        viewModel = createViewModel()
        val bitmap = mockk<Bitmap>()

        // When
        viewModel.generateRecipe(bitmap)
        advanceUntilIdle()

        // Then
        viewModel.generationState.test {
            val state = awaitItem()
            assertTrue("Expected Success but got $state", state is UiState.Success)
            val recipe = (state as UiState.Success).data
            assertEquals("Ensalada Fresca", recipe.title)
            assertEquals(recipeId, recipe.id)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { firestoreRepository.saveRecipe(any()) }
    }

    /**
     * Test: Error de cuota de API
     */
    @Test
    fun `generateRecipe shows quota error message`() = runTest {
        // Given
        val userId = "test-user"
        every { authRepository.currentUserId } returns userId
        coEvery { aiLogicDataSource.generateRecipeFromImage(any()) } throws Exception("quota exceeded")

        viewModel = createViewModel()
        val bitmap = mockk<Bitmap>()

        // When
        viewModel.generateRecipe(bitmap)
        advanceUntilIdle()

        // Then
        viewModel.generationState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            assertTrue((state as UiState.Error).message.contains("Cuota"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test: Error de red
     */
    @Test
    fun `generateRecipe shows network error message`() = runTest {
        // Given
        val userId = "test-user"
        every { authRepository.currentUserId } returns userId
        coEvery { aiLogicDataSource.generateRecipeFromImage(any()) } throws Exception("network error")

        viewModel = createViewModel()
        val bitmap = mockk<Bitmap>()

        // When
        viewModel.generateRecipe(bitmap)
        advanceUntilIdle()

        // Then
        viewModel.generationState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            assertTrue((state as UiState.Error).message.contains("conexión"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TESTS DE ELIMINACIÓN
    // =========================================================================

    /**
     * Test: deleteRecipe llama al repositorio
     */
    @Test
    fun `deleteRecipe calls repository`() = runTest {
        // Given
        coEvery { firestoreRepository.deleteRecipe(any()) } returns Result.success(Unit)
        viewModel = createViewModel()

        // When
        viewModel.deleteRecipe("recipe-123")
        advanceUntilIdle()

        // Then
        coVerify { firestoreRepository.deleteRecipe("recipe-123") }
    }

    // =========================================================================
    // TESTS DE LIMPIEZA DE ESTADO
    // =========================================================================

    /**
     * Test: clearAuthUiState resetea a Idle
     */
    @Test
    fun `clearAuthUiState resets state to Idle`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.clearAuthUiState()

        // Then
        assertEquals(UiState.Idle, viewModel.authUiState.value)
    }

    /**
     * Test: clearGenerationState resetea a Idle
     */
    @Test
    fun `clearGenerationState resets state to Idle`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.clearGenerationState()

        // Then
        assertEquals(UiState.Idle, viewModel.generationState.value)
    }
}

/**
 * =============================================================================
 * NOTAS SOBRE TESTING DE VIEWMODELS
 * =============================================================================
 *
 * 1. MOCKING CON MockK:
 *    ```kotlin
 *    // Mock relajado (retorna valores por defecto)
 *    val mock = mockk<MyClass>(relaxed = true)
 *
 *    // Configurar comportamiento
 *    every { mock.someMethod() } returns "value"
 *    coEvery { mock.suspendMethod() } returns result
 *
 *    // Verificar llamadas
 *    verify { mock.someMethod() }
 *    coVerify { mock.suspendMethod() }
 *    ```
 *
 * 2. TURBINE PARA FLOWS:
 *    ```kotlin
 *    flow.test {
 *        awaitItem() // Espera la próxima emisión
 *        awaitEvent() // Espera cualquier evento (item, error, complete)
 *        cancelAndIgnoreRemainingEvents() // Limpieza
 *        cancelAndConsumeRemainingEvents() // Verifica que no hay más
 *    }
 *    ```
 *
 * 3. TEST DISPATCHER:
 *    - StandardTestDispatcher: Control manual del tiempo
 *    - UnconfinedTestDispatcher: Ejecución inmediata
 *
 *    Usar StandardTestDispatcher + advanceUntilIdle() para control preciso.
 *
 * 4. PATRONES COMUNES:
 *    - Given-When-Then: Estructura clara de tests
 *    - Arrange-Act-Assert: Equivalente alternativo
 *    - Un assert por test (cuando sea práctico)
 *
 * =============================================================================
 */
