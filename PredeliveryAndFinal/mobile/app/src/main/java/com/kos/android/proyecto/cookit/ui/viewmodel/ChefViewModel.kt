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

    val authState: StateFlow<AuthState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    private val _authUiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authUiState: StateFlow<UiState<Unit>> = _authUiState.asStateFlow()

    private val _publicRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val publicRecipes: StateFlow<List<Recipe>> = _publicRecipes.asStateFlow()

    private val _trashedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val trashedRecipes: StateFlow<List<Recipe>> = _trashedRecipes.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    private val _filterByInventory = MutableStateFlow(false)
    val filterByInventory: StateFlow<Boolean> = _filterByInventory.asStateFlow()

    private val _filterExploreByInventory = MutableStateFlow(false)
    val filterExploreByInventory: StateFlow<Boolean> = _filterExploreByInventory.asStateFlow()

    private val _notifications = MutableStateFlow<List<CookitNotification>>(emptyList())
    val notifications: StateFlow<List<CookitNotification>> = _notifications.asStateFlow()

    val userData: StateFlow<UserData?> = authState
        .flatMapLatest { state ->
            when (state) {
                is AuthState.Authenticated -> userRepository.observeUserData(state.userId)
                else -> flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isAdmin: StateFlow<Boolean> = combine(authState, userData) { auth, user ->
        val email = (auth as? AuthState.Authenticated)?.email ?: user?.email
        email == "edgargameplay2@gmail.com"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val favoriteRecipes: StateFlow<List<Recipe>> = combine(
        publicRecipes,
        userData
    ) { publicList, user ->
        val favoriteIds = user?.favorites ?: emptyList()
        publicList.filter { it.id in favoriteIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredPublicRecipes: StateFlow<List<Recipe>> = combine(
        publicRecipes,
        _exploreSearchQuery,
        _filterExploreByInventory,
        userData
    ) { publicList, query, onlyInventory, user ->
        val inventory = user?.inventory ?: emptyList()
        publicList.filter { recipe ->
            val matchesQuery = recipe.name.contains(query, ignoreCase = true)
            val matchesInventory = if (onlyInventory) {
                recipe.ingredients.any { ingredient -> 
                    inventory.any { it.contains(ingredient, ignoreCase = true) } 
                }
            } else true
            matchesQuery && matchesInventory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recipes: StateFlow<List<Recipe>> = combine(
        publicRecipes,
        userData,
        _searchQuery,
        _filterByInventory
    ) { publicList, user, query, onlyInventory ->
        val myRecipeIds = user?.myRecipes ?: emptyList()
        val inventory = user?.inventory ?: emptyList()
        publicList.filter { recipe ->
            val matchesId = recipe.id in myRecipeIds
            val matchesQuery = recipe.name.contains(query, ignoreCase = true)
            val matchesInventory = if (onlyInventory) {
                recipe.ingredients.any { ingredient -> 
                    inventory.any { it.contains(ingredient, ignoreCase = true) } 
                }
            } else true
            matchesId && matchesQuery && matchesInventory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _generationState = MutableStateFlow<UiState<Recipe>>(UiState.Idle)
    val generationState: StateFlow<UiState<Recipe>> = _generationState.asStateFlow()

    private val _imageGenerationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val imageGenerationState: StateFlow<UiState<String>> = _imageGenerationState.asStateFlow()

    private val _addRecipeState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addRecipeState: StateFlow<UiState<Unit>> = _addRecipeState.asStateFlow()

    private var imageGenerationJob: Job? = null

    init {
        observePublicRecipes()
        observeTrashedRecipes()
    }

    private fun observePublicRecipes() {
        viewModelScope.launch {
            firestoreRepository.observePublicRecipes().collect { recipes ->
                _publicRecipes.value = recipes
            }
        }
    }

    private fun observeTrashedRecipes() {
        viewModelScope.launch {
            firestoreRepository.observeTrashedRecipes().collect { recipes ->
                _trashedRecipes.value = recipes
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Logging in...")
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = { _authUiState.value = UiState.Success(Unit) },
                onFailure = { error -> _authUiState.value = UiState.Error(error.message ?: "Unknown error") }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Connecting with Google...")
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { userId ->
                    // Intentamos obtener el email real de la sesión de Firebase
                    val email = authRepository.observeAuthState().first().let { 
                        (it as? AuthState.Authenticated)?.email ?: "google_user@example.com"
                    }
                    val existingData = userRepository.getUserData(userId)
                    if (existingData == null) {
                        val newUserData = UserData(email = email, name = "Google User")
                        userRepository.createUserDocument(userId, newUserData)
                    }
                    _authUiState.value = UiState.Success(Unit)
                },
                onFailure = { error -> _authUiState.value = UiState.Error(error.message ?: "Google error") }
            )
        }
    }

    fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = UiState.Loading("Creating account...")
            val authResult = authRepository.signUp(email, password)
            authResult.fold(
                onSuccess = { userId ->
                    val newUserData = UserData(email = email, name = name)
                    userRepository.createUserDocument(userId, newUserData)
                    _authUiState.value = UiState.Success(Unit)
                },
                onFailure = { error -> _authUiState.value = UiState.Error(error.message ?: "Signup error") }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authUiState.value = UiState.Idle
    }

    fun clearAuthUiState() { _authUiState.value = UiState.Idle }
    fun clearGenerationState() { _generationState.value = UiState.Idle }
    fun clearImageState() { 
        imageGenerationJob?.cancel()
        _imageGenerationState.value = UiState.Idle 
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch { 
            val result = firestoreRepository.moveToTrash(recipe)
            result.fold(
                onSuccess = { showNotification("Recipe moved to trash bin", Color(0xFF5D4037)) },
                onFailure = { showNotification("Error: ${it.message}", Color.Red) }
            )
        }
    }

    fun restoreFromTrash(recipe: Recipe) {
        viewModelScope.launch { 
            val result = firestoreRepository.restoreFromTrash(recipe)
            result.fold(
                onSuccess = { showNotification("Recipe restored", Color(0xFF388E3C)) },
                onFailure = { showNotification("Error: ${it.message}", Color.Red) }
            )
        }
    }

    fun permanentDelete(recipeId: String) {
        viewModelScope.launch { 
            val result = firestoreRepository.permanentDelete(recipeId)
            result.fold(
                onSuccess = { showNotification("Recipe deleted permanently", Color.Black) },
                onFailure = { showNotification("Error: ${it.message}", Color.Red) }
            )
        }
    }

    fun toggleFavorite(recipeId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { 
            val user = userRepository.getUserData(userId)
            val isFavorite = user?.favorites?.contains(recipeId) == true
            val result = userRepository.toggleFavorite(userId, recipeId)
            result.onSuccess {
                val msg = if (isFavorite) "Removed from favorites" else "Added to favorites"
                showNotification(msg, if (isFavorite) Color.Gray else Color(0xFFD32F2F))
            }
        }
    }

    fun toggleSaveRecipe(recipeId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { 
            val user = userRepository.getUserData(userId)
            val isSaved = user?.myRecipes?.contains(recipeId) == true
            val result = userRepository.toggleSaveRecipe(userId, recipeId)
            result.onSuccess {
                val msg = if (isSaved) "Removed from saved" else "Recipe saved"
                showNotification(msg, if (isSaved) Color.Gray else Color(0xFF388E3C))
            }
        }
    }
    
    private fun showNotification(message: String, color: Color) {
        viewModelScope.launch {
            val notification = CookitNotification(message = message, color = color)
            _notifications.update { it + notification }
            delay(2500)
            _notifications.update { currentList -> currentList.filter { it.id != notification.id } }
        }
    }

    fun uploadProfilePicture(bitmap: Bitmap) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
            val result = storageRepository.uploadProfilePicture(userId, croppedBitmap)
            result.onSuccess { url -> userRepository.updateProfilePicture(userId, url) }
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

    fun saveManualRecipe(
        name: String,
        ingredients: List<String>,
        steps: List<String>,
        difficulty: String,
        imageBitmap: Bitmap?
    ) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _addRecipeState.value = UiState.Loading("Saving recipe...")
            try {
                val initialRecipe = Recipe(
                    name = name,
                    ingredients = ingredients,
                    steps = steps,
                    difficulty = difficulty
                )
                val saveResult = firestoreRepository.saveRecipe(initialRecipe)
                saveResult.fold(
                    onSuccess = { recipeId ->
                        var finalImageUrl = ""
                        if (imageBitmap != null) {
                            val uploadResult = storageRepository.uploadRecipeImage(recipeId, imageBitmap)
                            if (uploadResult.isSuccess) {
                                finalImageUrl = uploadResult.getOrThrow()
                                val updatedRecipe = initialRecipe.copy(id = recipeId, imageURL = finalImageUrl)
                                firestoreRepository.saveRecipe(updatedRecipe)
                            }
                        } else {
                             val updatedRecipe = initialRecipe.copy(id = recipeId)
                             firestoreRepository.saveRecipe(updatedRecipe)
                        }
                        userRepository.addRecipeToMyRecipes(userId, recipeId)
                        _addRecipeState.value = UiState.Success(Unit)
                    },
                    onFailure = { error ->
                        _addRecipeState.value = UiState.Error(error.message ?: "Failed to save recipe")
                    }
                )
            } catch (e: Exception) {
                _addRecipeState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearAddRecipeState() {
        _addRecipeState.value = UiState.Idle
    }

    fun generateRecipeImage(recipeId: String, existingImageUrl: String, recipeTitle: String, ingredients: List<String>) {
        imageGenerationJob?.cancel()
        imageGenerationJob = viewModelScope.launch {
            if (existingImageUrl.isNotBlank()) {
                _imageGenerationState.value = UiState.Success(existingImageUrl)
                return@launch
            }
            _imageGenerationState.value = UiState.Loading("Generating image...")
            try {
                val bitmap = aiLogicDataSource.generateRecipeImage(recipeTitle, ingredients)
                val uploadResult = storageRepository.uploadRecipeImage(recipeId, bitmap)
                uploadResult.onSuccess { _imageGenerationState.value = UiState.Success(it) }
            } catch (e: Exception) {
                _imageGenerationState.value = UiState.Error("Error: ${e.message}")
            }
        }
    }
}
