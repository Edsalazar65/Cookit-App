package com.kos.android.proyecto.cookit.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kos.android.proyecto.cookit.data.firebase.IAuthRepository
import com.kos.android.proyecto.cookit.data.firebase.IFirestoreRepository
import com.kos.android.proyecto.cookit.data.firebase.IStorageRepository
import com.kos.android.proyecto.cookit.data.firebase.IUserRepository
import com.kos.android.proyecto.cookit.data.remote.IAiLogicDataSource
import com.kos.android.proyecto.cookit.domain.model.AuthState
import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.kos.android.proyecto.cookit.domain.model.UiState
import com.kos.android.proyecto.cookit.domain.model.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CookitNotification(
    val id: Long = System.nanoTime(),
    val message: String,
    val color: Color = Color(0xFF388E3C)
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChefViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val firestoreRepository: IFirestoreRepository,
    private val storageRepository: IStorageRepository,
    private val aiLogicDataSource: IAiLogicDataSource
) : ViewModel() {

    // 1. Autenticación (Eager para persistencia inmediata)
    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _authUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authUiState: StateFlow<UiState<Unit>> = _authUiState.asStateFlow()

    // 2. Datos del Usuario Reactivos
    val userData: StateFlow<UserData?> = authState
        .flatMapLatest { state ->
            if (state is AuthState.Authenticated) {
                Log.d("ChefViewModel", "Session detected for ${state.userId}. Fetching user data...")
                userRepository.observeUserData(state.userId)
            } else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 3. Permisos de Administrador (Dual)
    val isAdmin: StateFlow<Boolean> = combine(authState, userData) { auth, user ->
        val email = (auth as? AuthState.Authenticated)?.email ?: user?.email ?: ""
        val result = email.lowercase() == "edgargameplay2@gmail.com" || email.lowercase() == "nikole1@gmail.com"
        Log.d("ChefViewModel", "ADMIN STATUS FOR $email: $result")
        result
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 4. Papelera Reactiva (Se activa por evento al detectar Admin)
    val trashedRecipes: StateFlow<List<Recipe>> = isAdmin
        .flatMapLatest { isAdm ->
            if (isAdm) {
                Log.d("ChefViewModel", "Admin Mode: Activating Trashed Recipes Listener")
                firestoreRepository.observeTrashedRecipes()
            } else {
                Log.d("ChefViewModel", "Standard Mode: Clearing Trashed Recipes")
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 5. Notificaciones Apilables
    private val _notifications = MutableStateFlow<List<CookitNotification>>(emptyList())
    val notifications: StateFlow<List<CookitNotification>> = _notifications.asStateFlow()

    // 6. Recetas y Búsqueda
    private val _publicRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val publicRecipes: StateFlow<List<Recipe>> = _publicRecipes.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    private val _filterByInventory = MutableStateFlow(false)
    val filterByInventory: StateFlow<Boolean> = _filterByInventory.asStateFlow()

    private val _filterExploreByInventory = MutableStateFlow(false)
    val filterExploreByInventory: StateFlow<Boolean> = _filterExploreByInventory.asStateFlow()

    // 7. Flujos de UI
    val favoriteRecipes: StateFlow<List<Recipe>> = combine(publicRecipes, userData) { list, user ->
        val favIds = user?.favorites ?: emptyList()
        list.filter { it.id in favIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPublicRecipes: StateFlow<List<Recipe>> = combine(
        publicRecipes, _exploreSearchQuery, _filterExploreByInventory, userData
    ) { list, query, onlyInv, user ->
        val inventory = user?.inventory ?: emptyList()
        list.filter { r ->
            val mQuery = r.name.contains(query, ignoreCase = true)
            val mInv = if (onlyInv) r.ingredients.any { ing -> inventory.any { it.contains(ing, ignoreCase = true) } } else true
            mQuery && mInv
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<Recipe>> = combine(
        publicRecipes, userData, _searchQuery, _filterByInventory
    ) { list, user, query, onlyInv ->
        val myIds = user?.myRecipes ?: emptyList()
        val inventory = user?.inventory ?: emptyList()
        list.filter { r ->
            val mId = r.id in myIds
            val mQuery = r.name.contains(query, ignoreCase = true)
            val mInv = if (onlyInv) r.ingredients.any { ing -> inventory.any { it.contains(ing, ignoreCase = true) } } else true
            mId && mQuery && mInv
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 8. Estado de Adición de Receta
    private val _addRecipeState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addRecipeState: StateFlow<UiState<Unit>> = _addRecipeState.asStateFlow()

    init {
        observePublicRecipes()
    }

    private fun observePublicRecipes() {
        viewModelScope.launch {
            firestoreRepository.observePublicRecipes().collect { recipes ->
                _publicRecipes.value = recipes
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Logging in...")
            authRepository.signIn(email, password).fold(
                onSuccess = { _authUiState.value = UiState.Success(Unit) },
                onFailure = { error -> _authUiState.value = UiState.Error(error.message ?: "Error") }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Connecting with Google...")
            authRepository.signInWithGoogle(idToken).onSuccess { userId ->
                viewModelScope.launch {
                    val auth = authRepository.observeAuthState().first { it is AuthState.Authenticated }
                    val realEmail = (auth as AuthState.Authenticated).email ?: "google_user@example.com"
                    val existingData = userRepository.getUserData(userId)
                    if (existingData == null) {
                        userRepository.createUserDocument(userId, UserData(email = realEmail, name = "Google User"))
                    }
                    _authUiState.value = UiState.Success(Unit)
                }
            }.onFailure { _authUiState.value = UiState.Error(it.message ?: "Google Error") }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authUiState.value = UiState.Idle
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch { 
            firestoreRepository.moveToTrash(recipe).onSuccess { 
                showNotification("Recipe moved to trash bin", Color(0xFF5D4037)) 
            }.onFailure { showNotification("Error: ${it.message}", Color.Red) }
        }
    }

    fun restoreFromTrash(recipe: Recipe) {
        viewModelScope.launch { 
            firestoreRepository.restoreFromTrash(recipe).onSuccess { 
                showNotification("Recipe restored", Color(0xFF388E3C)) 
            }.onFailure { showNotification("Error: ${it.message}", Color.Red) }
        }
    }

    fun permanentDelete(recipeId: String) {
        viewModelScope.launch { 
            firestoreRepository.permanentDelete(recipeId).onSuccess { 
                showNotification("Recipe deleted permanently", Color.Black) 
            }.onFailure { showNotification("Error: ${it.message}", Color.Red) }
        }
    }

    fun toggleFavorite(recipeId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { 
            val user = userRepository.getUserData(userId)
            val isFavorite = user?.favorites?.contains(recipeId) == true
            userRepository.toggleFavorite(userId, recipeId).onSuccess {
                showNotification(if (isFavorite) "Removed from favorites" else "Added to favorites", if (isFavorite) Color.Gray else Color(0xFFD32F2F))
            }
        }
    }

    fun toggleSaveRecipe(recipeId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { 
            val user = userRepository.getUserData(userId)
            val isSaved = user?.myRecipes?.contains(recipeId) == true
            userRepository.toggleSaveRecipe(userId, recipeId).onSuccess {
                showNotification(if (isSaved) "Removed from saved" else "Recipe saved", if (isSaved) Color.Gray else Color(0xFF388E3C))
            }
        }
    }
    
    private fun showNotification(message: String, color: Color) {
        viewModelScope.launch {
            val notification = CookitNotification(message = message, color = color)
            _notifications.update { it + notification }
            delay(2500)
            _notifications.update { current -> current.filter { it.id != notification.id } }
        }
    }

    fun uploadProfilePicture(bitmap: Bitmap) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            storageRepository.uploadProfilePicture(userId, bitmap).onSuccess { url ->
                userRepository.updateProfilePicture(userId, url)
            }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onExploreSearchQueryChange(query: String) { _exploreSearchQuery.value = query }
    fun onToggleInventoryFilter() { _filterByInventory.value = !_filterByInventory.value }
    fun onToggleExploreInventoryFilter() { _filterExploreByInventory.value = !_filterExploreByInventory.value }
    
    fun addIngredient(ingredient: String) {
        val userId = authRepository.currentUserId ?: return
        if (ingredient.isBlank()) return
        viewModelScope.launch { userRepository.addIngredient(userId, ingredient) }
    }
    fun removeIngredient(ingredient: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { userRepository.removeIngredient(userId, ingredient) }
    }

    fun saveManualRecipe(name: String, ingredients: List<String>, steps: List<String>, difficulty: String, imageBitmap: Bitmap?) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _addRecipeState.value = UiState.Loading("Saving...")
            firestoreRepository.saveRecipe(Recipe(name = name, ingredients = ingredients, steps = steps, difficulty = difficulty)).fold(
                onSuccess = { id ->
                    if (imageBitmap != null) {
                        storageRepository.uploadRecipeImage(id, imageBitmap).onSuccess { firestoreRepository.updateRecipeImageUrl(id, it) }
                    }
                    userRepository.addRecipeToMyRecipes(userId, id)
                    _addRecipeState.value = UiState.Success(Unit)
                },
                onFailure = { _addRecipeState.value = UiState.Error(it.message ?: "Failed") }
            )
        }
    }

    // IA y Estados de Imagen
    private val _imageGenerationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val imageGenerationState: StateFlow<UiState<String>> = _imageGenerationState.asStateFlow()
    private var imageGenerationJob: Job? = null

    fun generateRecipeImage(recipeId: String, existingImageUrl: String, recipeTitle: String, ingredients: List<String>) {
        imageGenerationJob?.cancel()
        imageGenerationJob = viewModelScope.launch {
            if (existingImageUrl.isNotBlank()) {
                _imageGenerationState.value = UiState.Success(existingImageUrl)
                return@launch
            }
            _imageGenerationState.value = UiState.Loading("Generating...")
            try {
                val bitmap = aiLogicDataSource.generateRecipeImage(recipeTitle, ingredients)
                storageRepository.uploadRecipeImage(recipeId, bitmap).onSuccess {
                    firestoreRepository.updateRecipeImageUrl(recipeId, it)
                    _imageGenerationState.value = UiState.Success(it)
                }
            } catch (e: Exception) { _imageGenerationState.value = UiState.Error(e.message ?: "Error") }
        }
    }

    fun clearAddRecipeState() { _addRecipeState.value = UiState.Idle }
    fun clearAuthUiState() { _authUiState.value = UiState.Idle }
    fun signUp(name: String, email: String, password: String) {}
}
