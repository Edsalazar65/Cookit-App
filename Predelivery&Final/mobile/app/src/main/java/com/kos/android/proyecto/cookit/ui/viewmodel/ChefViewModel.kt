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
import com.kos.android.proyecto.cookit.data.firebase.IUserRepository
import com.kos.android.proyecto.cookit.domain.model.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    private val _filterByInventory = MutableStateFlow(false)
    val filterByInventory: StateFlow<Boolean> = _filterByInventory.asStateFlow()

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
        _exploreSearchQuery
    ) { publicList, query ->
        if (query.isBlank()) publicList
        else publicList.filter { it.name.contains(query, ignoreCase = true) }
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

    private var imageGenerationJob: Job? = null

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
                    val existingData = userRepository.getUserData(userId)
                    if (existingData == null) {
                        val newUserData = UserData(email = "google_user@example.com", name = "Google User")
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

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch { firestoreRepository.deleteRecipe(recipeId) }
    }

    fun toggleFavorite(recipeId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { userRepository.toggleFavorite(userId, recipeId) }
    }

    fun toggleSaveRecipe(recipeId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { userRepository.toggleSaveRecipe(userId, recipeId) }
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
    fun addIngredient(ingredient: String) {
        val userId = authRepository.currentUserId ?: return
        if (ingredient.isBlank()) return
        viewModelScope.launch { userRepository.addIngredient(userId, ingredient) }
    }
    fun removeIngredient(ingredient: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { userRepository.removeIngredient(userId, ingredient) }
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
