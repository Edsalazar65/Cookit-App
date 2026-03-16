package com.curso.android.module5.aichef.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curso.android.module5.aichef.data.firebase.IAuthRepository
import com.curso.android.module5.aichef.data.firebase.IFirestoreRepository
import com.curso.android.module5.aichef.data.firebase.IStorageRepository
import com.curso.android.module5.aichef.data.remote.IAiLogicDataSource
import com.curso.android.module5.aichef.domain.model.AuthState
import com.curso.android.module5.aichef.domain.model.Recipe
import com.curso.android.module5.aichef.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * =============================================================================
 * ChefViewModel - ViewModel principal de la aplicación
 * =============================================================================
 *
 * CONCEPTO: ViewModel en MVVM
 * El ViewModel actúa como intermediario entre la UI y los datos:
 * - Expone estados observables (StateFlow)
 * - Ejecuta acciones (suspend functions)
 * - Sobrevive a cambios de configuración
 *
 * Este ViewModel maneja:
 * - Estado de autenticación (login/logout)
 * - Lista de recetas del usuario
 * - Generación de recetas con IA
 *
 * ARQUITECTURA:
 * UI (Compose) ──observa──> ChefViewModel ──usa──> Repositories
 *                                              ├── IAuthRepository
 *                                              ├── IFirestoreRepository
 *                                              ├── IStorageRepository
 *                                              └── IAiLogicDataSource
 *
 * CONCEPTO: @HiltViewModel
 * Esta anotación permite que Hilt inyecte dependencias en el ViewModel.
 * Combinado con @Inject constructor, Hilt sabe cómo crear el ViewModel.
 *
 * BENEFICIO DE USAR INTERFACES:
 * El ViewModel depende de interfaces (IAuthRepository, etc.) en lugar de
 * implementaciones concretas. Esto permite:
 * 1. Testear con mocks/fakes sin tocar Firebase real
 * 2. Cambiar implementaciones sin modificar el ViewModel
 * 3. Seguir el principio de Inversión de Dependencias (DIP)
 *
 * =============================================================================
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChefViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val firestoreRepository: IFirestoreRepository,
    private val storageRepository: IStorageRepository,
    private val aiLogicDataSource: IAiLogicDataSource
) : ViewModel() {

    // =========================================================================
    // ESTADO DE AUTENTICACIÓN
    // =========================================================================

    /**
     * Estado de autenticación observable
     *
     * CONCEPTO: stateIn
     * Convierte el Flow de AuthRepository en StateFlow para Compose.
     * - SharingStarted.WhileSubscribed: Activo mientras hay observers
     * - 5000ms: Mantiene activo 5s después del último observer
     */
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    // =========================================================================
    // ESTADO DE UI PARA AUTH
    // =========================================================================

    private val _authUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authUiState: StateFlow<UiState<Unit>> = _authUiState.asStateFlow()

    // =========================================================================
    // LISTA DE RECETAS
    // =========================================================================

    /**
     * Lista de recetas del usuario autenticado
     *
     * CONCEPTO: flatMapLatest
     * Cada vez que cambia el authState, cancelamos el Flow anterior
     * y creamos uno nuevo basado en el nuevo userId.
     *
     * Si el usuario no está autenticado, emitimos lista vacía.
     */
    val recipes: StateFlow<List<Recipe>> = authState
        .flatMapLatest { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    firestoreRepository.observeUserRecipes(state.userId)
                }
                else -> flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // =========================================================================
    // ESTADO DE GENERACIÓN DE RECETAS
    // =========================================================================

    private val _generationState = MutableStateFlow<UiState<Recipe>>(UiState.Idle)
    val generationState: StateFlow<UiState<Recipe>> = _generationState.asStateFlow()

    // =========================================================================
    // ESTADO DE GENERACIÓN DE IMÁGENES
    // =========================================================================
    // El estado ahora es String (URL) en lugar de Bitmap
    // Esto permite usar Coil para cargar y cachear la imagen eficientemente

    private val _imageGenerationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val imageGenerationState: StateFlow<UiState<String>> = _imageGenerationState.asStateFlow()

    private var imageGenerationJob: Job? = null

    // =========================================================================
    // ACCIONES DE AUTENTICACIÓN
    // =========================================================================

    /**
     * Inicia sesión con email y password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Iniciando sesión...")

            val result = authRepository.signIn(email, password)

            _authUiState.value = result.fold(
                onSuccess = { UiState.Success(Unit) },
                onFailure = { UiState.Error(it.message ?: "Error desconocido") }
            )
        }
    }

    /**
     * Registra un nuevo usuario
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Creando cuenta...")

            val result = authRepository.signUp(email, password)

            _authUiState.value = result.fold(
                onSuccess = { UiState.Success(Unit) },
                onFailure = { UiState.Error(it.message ?: "Error desconocido") }
            )
        }
    }

    /**
     * Cierra la sesión actual
     */
    fun signOut() {
        authRepository.signOut()
        _authUiState.value = UiState.Idle
    }

    /**
     * Limpia el estado de UI de autenticación
     */
    fun clearAuthUiState() {
        _authUiState.value = UiState.Idle
    }

    // =========================================================================
    // ACCIONES DE GENERACIÓN DE RECETAS
    // =========================================================================

    /**
     * Genera una receta a partir de una imagen usando Firebase AI Logic
     *
     * FLUJO:
     * 1. Validar que hay un usuario autenticado
     * 2. Llamar a Firebase AI Logic con la imagen
     * 3. Guardar la receta generada en Firestore
     * 4. Retornar la receta guardada
     *
     * MANEJO DE ERRORES:
     * - Cuota excedida: Mensaje específico al usuario
     * - Error de red: Sugerir reintentar
     * - Error de IA: Mostrar mensaje genérico
     *
     * @param imageBitmap Imagen de los ingredientes
     */
    fun generateRecipe(imageBitmap: Bitmap) {
        val userId = authRepository.currentUserId
        if (userId == null) {
            _generationState.value = UiState.Error("Debes iniciar sesión")
            return
        }

        viewModelScope.launch {
            _generationState.value = UiState.Loading("Analizando imagen con IA...")

            try {
                // 1. Generar receta con Firebase AI Logic
                val generatedRecipe = aiLogicDataSource.generateRecipeFromImage(imageBitmap)

                // 2. Crear objeto Recipe para guardar
                val recipe = Recipe(
                    userId = userId,
                    title = generatedRecipe.title,
                    ingredients = generatedRecipe.ingredients,
                    steps = generatedRecipe.steps
                )

                // 3. Guardar en Firestore
                val saveResult = firestoreRepository.saveRecipe(recipe)

                saveResult.fold(
                    onSuccess = { recipeId ->
                        // Retornar la receta con el ID generado
                        _generationState.value = UiState.Success(recipe.copy(id = recipeId))
                    },
                    onFailure = { error ->
                        _generationState.value = UiState.Error(
                            "Receta generada pero no se pudo guardar: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                // Manejar errores específicos de Firebase AI Logic
                val errorMessage = when {
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "Cuota de API excedida. Intenta más tarde."
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                        "Error de permisos. Verifica la configuración de Firebase."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Error de conexión. Verifica tu internet."
                    else ->
                        "Error al generar receta: ${e.message}"
                }
                _generationState.value = UiState.Error(errorMessage)
            }
        }
    }

    /**
     * Limpia el estado de generación
     */
    fun clearGenerationState() {
        _generationState.value = UiState.Idle
    }

    // =========================================================================
    // ACCIONES DE RECETAS
    // =========================================================================

    /**
     * Elimina una receta
     */
    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteRecipe(recipeId)
        }
    }

    // =========================================================================
    // ACCIONES DE GENERACIÓN DE IMÁGENES CON CACHE
    // =========================================================================

    /**
     * Obtiene o genera la imagen del plato terminado
     *
     * CONCEPTO: Cache de Imágenes Generadas
     * Para evitar consumir cuota de API en cada vista:
     * 1. Primero verificamos si ya existe una URL en el modelo (Firestore)
     * 2. Si existe, la usamos directamente
     * 3. Si no existe, generamos con Gemini, subimos a Storage, y guardamos URL
     *
     * FLUJO:
     * ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
     * │ ¿Tiene URL?  │─Sí─▶│  Usar URL    │─────▶│   Success    │
     * └──────┬───────┘     └──────────────┘     └──────────────┘
     *        │No
     *        ▼
     * ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
     * │Generar imagen│────▶│Subir Storage │────▶│Guardar URL   │
     * │  (Gemini)    │     │              │     │ (Firestore)  │
     * └──────────────┘     └──────────────┘     └──────────────┘
     *
     * @param recipeId ID de la receta para cache
     * @param existingImageUrl URL existente (si hay)
     * @param recipeTitle Título de la receta
     * @param ingredients Lista de ingredientes
     */
    fun generateRecipeImage(
        recipeId: String,
        existingImageUrl: String,
        recipeTitle: String,
        ingredients: List<String>
    ) {
        // Cancelar generación anterior si existe
        imageGenerationJob?.cancel()

        imageGenerationJob = viewModelScope.launch {
            // ================================================================
            // PASO 1: Verificar si ya existe imagen cacheada
            // ================================================================
            if (existingImageUrl.isNotBlank()) {
                // Ya tenemos la imagen, usar directamente
                _imageGenerationState.value = UiState.Success(existingImageUrl)
                return@launch
            }

            // ================================================================
            // PASO 2: No hay cache, generar nueva imagen
            // ================================================================
            _imageGenerationState.value = UiState.Loading("Generando imagen del plato...")

            try {
                // Generar imagen con Gemini
                val bitmap = aiLogicDataSource.generateRecipeImage(recipeTitle, ingredients)

                // ============================================================
                // PASO 3: Subir imagen a Firebase Storage
                // ============================================================
                _imageGenerationState.value = UiState.Loading("Guardando imagen...")

                val uploadResult = storageRepository.uploadRecipeImage(recipeId, bitmap)

                uploadResult.fold(
                    onSuccess = { imageUrl ->
                        // ====================================================
                        // PASO 4: Guardar URL en Firestore para futuro cache
                        // ====================================================
                        firestoreRepository.updateGeneratedImageUrl(recipeId, imageUrl)

                        // Éxito - devolver la URL
                        _imageGenerationState.value = UiState.Success(imageUrl)
                    },
                    onFailure = { error ->
                        // Error al subir, pero tenemos el bitmap
                        // Podríamos mostrar el bitmap directamente como fallback
                        _imageGenerationState.value = UiState.Error(
                            "Imagen generada pero no se pudo guardar: ${error.message}"
                        )
                    }
                )

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "Cuota de API excedida. Intenta más tarde."
                    e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                        "Error de permisos. Verifica la configuración."
                    e.message?.contains("not supported", ignoreCase = true) == true ->
                        "Generación de imágenes no disponible."
                    else ->
                        "Error al generar imagen: ${e.message}"
                }
                _imageGenerationState.value = UiState.Error(errorMessage)
            }
        }
    }

    /**
     * Limpia el estado de generación de imagen
     */
    fun clearImageState() {
        imageGenerationJob?.cancel()
        _imageGenerationState.value = UiState.Idle
    }
}
