package com.kos.android.proyecto.cookit.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kos.android.proyecto.cookit.data.firebase.IAuthRepository
import com.kos.android.proyecto.cookit.data.firebase.IFirestoreRepository
import com.kos.android.proyecto.cookit.data.firebase.IStorageRepository
import com.kos.android.proyecto.cookit.data.remote.IAiLogicDataSource
import com.kos.android.proyecto.cookit.domain.model.AuthState
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.domain.model.UiState
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
import com.kos.android.proyecto.cookit.data.firebase.IUserRepository
import com.kos.android.proyecto.cookit.domain.model.UserData
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChefViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val firestoreRepository: IFirestoreRepository,
    private val storageRepository: IStorageRepository,
    private val aiLogicDataSource: IAiLogicDataSource
) : ViewModel() {


    private val _publicRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val publicRecipes: StateFlow<List<Recipe>> = _publicRecipes.asStateFlow()

    init {
        observePublicRecipes()
    }

    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )



    private val _authUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authUiState: StateFlow<UiState<Unit>> = _authUiState.asStateFlow()


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


    private val _generationState = MutableStateFlow<UiState<Recipe>>(UiState.Idle)
    val generationState: StateFlow<UiState<Recipe>> = _generationState.asStateFlow()


    private val _imageGenerationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val imageGenerationState: StateFlow<UiState<String>> = _imageGenerationState.asStateFlow()

    private var imageGenerationJob: Job? = null

    //Autenticacion

    /**
     * Inicia sesión con email y password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Iniciando sesión...")

            val result = authRepository.signIn(email, password)

            result.fold(
                onSuccess = { _authUiState.value = UiState.Success(Unit) },
                onFailure = { error -> _authUiState.value = UiState.Error(error.message ?: "Error desconocido") }
            )
        }
    }
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Conectando con Google...")

            val result = authRepository.signInWithGoogle(idToken)

            result.fold(
                onSuccess = { userId ->

                    val existingData = userRepository.getUserData(userId)

                    if (existingData == null) {

                        val newUserData = UserData(
                            email = "email_obtenido_de_google@ejemplo.com",
                            name = "Usuario de Google"
                        )
                        val firestoreResult = userRepository.createUserDocument(userId, newUserData)

                        firestoreResult.fold(
                            onSuccess = { _authUiState.value = UiState.Success(Unit) },
                            onFailure = { error ->
                                _authUiState.value = UiState.Error(
                                    "Autenticado pero no se pudo crear el perfil: ${error.message}"
                                )
                            }
                        )
                    } else {

                        _authUiState.value = UiState.Success(Unit)
                    }
                },
                onFailure = { error ->
                    _authUiState.value = UiState.Error(error.message ?: "Error de Google")
                }
            )
        }
    }

    /**
     * Registra un nuevo usuario
     */
    fun signUp(name : String,email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Creando cuenta...")

            val authResult = authRepository.signUp(email, password)

            authResult.fold(
                onSuccess = { userId ->
                    val newUserData = UserData(
                        email = email,
                        name = name
                    )

                    val firestoreResult = userRepository.createUserDocument(userId, newUserData)

                    firestoreResult.fold(
                        onSuccess = {
                            _authUiState.value = UiState.Success(Unit)
                        },
                        onFailure = { error ->
                            _authUiState.value = UiState.Error(
                                "Usuario creado pero falló la base de datos: ${error.message}"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _authUiState.value = UiState.Error(error.message ?: "Error de registro")
                }
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

    private fun observePublicRecipes() {
        viewModelScope.launch {
            firestoreRepository.observePublicRecipes().collect { recipes ->
                _publicRecipes.value = recipes
            }
        }
    }


    fun generateRecipe(imageBitmap: Bitmap) {
        val userId = authRepository.currentUserId ?: "usuario_invitado_demo"
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
//                val recipe = Recipe(
//                    userId = userId,
//                    title = generatedRecipe.title,
//                    ingredients = generatedRecipe.ingredients,
//                    steps = generatedRecipe.steps
//                )

                // 3. Guardar en Firestore
//                val saveResult = firestoreRepository.saveRecipe(recipe)

//                saveResult.fold(
//                    onSuccess = { recipeId ->
//                        // Retornar la receta con el ID generado
//                        _generationState.value = UiState.Success(recipe.copy(id = recipeId))
//                    },
//                    onFailure = { error ->
//                        _generationState.value = UiState.Error(
//                            "Receta generada pero no se pudo guardar: ${error.message}"
//                        )
//                    }
//                )

            } catch (e: Exception) {

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

    /**
     * Elimina una receta
     */
    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteRecipe(recipeId)
        }
    }

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


                _imageGenerationState.value = UiState.Loading("Guardando imagen...")

                val uploadResult = storageRepository.uploadRecipeImage(recipeId, bitmap)

                uploadResult.fold(
                    onSuccess = { imageUrl ->

                        //firestoreRepository.updateGeneratedImageUrl(recipeId, imageUrl)


                        _imageGenerationState.value = UiState.Success(imageUrl)
                    },
                    onFailure = { error ->
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
