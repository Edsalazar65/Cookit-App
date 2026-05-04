package com.kos.android.proyecto.cookit.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kos.android.proyecto.cookit.data.firebase.IAuthRepository
import com.kos.android.proyecto.cookit.data.firebase.IFirestoreRepository
import com.kos.android.proyecto.cookit.data.firebase.IStorageRepository
import com.kos.android.proyecto.cookit.data.firebase.IUserRepository
import com.kos.android.proyecto.cookit.data.remote.IAiLogicDataSource
import com.kos.android.proyecto.cookit.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiLogicDataSource: IAiLogicDataSource,
    private val firestoreRepository: IFirestoreRepository,
    private val storageRepository: IStorageRepository,
    private val userRepository: IUserRepository,
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("¡Bonjour! Soy Remy. ¿En qué puedo ayudarte hoy en la cocina? 🐭🍳", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(message: String, inventory: List<String>) {
        if (message.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage(message, true)
            _isLoading.value = true

            try {
                val response = aiLogicDataSource.chatWithRemy(message, inventory)
                
                val cleanResponse = response.trim()
                if (cleanResponse.contains("{") && cleanResponse.contains("}")) {
                    val startIndex = cleanResponse.indexOf("{")
                    val endIndex = cleanResponse.lastIndexOf("}")
                    val jsonPart = cleanResponse.substring(startIndex, endIndex + 1)
                    handleRecipeResponse(jsonPart)
                } else {
                    _messages.value = _messages.value + ChatMessage(response, false)
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("🐭 Ups, mon ami, tuve un pequeño problema en la cocina: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun handleRecipeResponse(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val name = json.getString("name")
            val ingredients = mutableListOf<String>()
            val ingredientsArray = json.getJSONArray("ingredients")
            for (i in 0 until ingredientsArray.length()) {
                ingredients.add(ingredientsArray.getString(i))
            }
            val steps = mutableListOf<String>()
            val stepsArray = json.getJSONArray("steps")
            for (i in 0 until stepsArray.length()) {
                steps.add(stepsArray.getString(i))
            }
            val difficulty = json.optString("difficulty", "Media")
            val imagePrompt = json.optString("imagePrompt", "A professional photo of $name")

            _messages.value = _messages.value + ChatMessage("¡Voilà! He creado una receta especial para ti: **$name**. Dame un momento para prepararla... 🐭✨", false)

            // 1. Crear la receta en Firestore para obtener el ID
            val initialRecipe = Recipe(
                name = name,
                ingredients = ingredients,
                steps = steps,
                difficulty = difficulty
            )
            val saveResult = firestoreRepository.saveRecipe(initialRecipe)
            
            saveResult.fold(
                onSuccess = { recipeId ->
                    // 2. Generar imagen
                    try {
                        Log.d("ChatViewModel", "Generando imagen para la receta: $name")
                        val bitmap = aiLogicDataSource.generateRecipeImage(name, ingredients, imagePrompt)
                        
                        Log.d("ChatViewModel", "Subiendo imagen a storage...")
                        // 3. Subir a Storage
                        val uploadResult = storageRepository.uploadRecipeImage(recipeId, bitmap)
                        
                        if (uploadResult.isSuccess) {
                            val imageUrl = uploadResult.getOrThrow()
                            Log.d("ChatViewModel", "Imagen subida con éxito: $imageUrl")
                            
                            // 4. Actualizar el campo imageURL del documento existente
                            val updateResult = firestoreRepository.updateRecipeImageUrl(recipeId, imageUrl)
                            
                            if (updateResult.isSuccess) {
                                Log.d("ChatViewModel", "Base de datos actualizada con ImageURL para: $recipeId")
                            } else {
                                Log.e("ChatViewModel", "Error al actualizar ImageURL: ${updateResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            Log.e("ChatViewModel", "Error al subir la imagen: ${uploadResult.exceptionOrNull()?.message}")
                            // Fallback: Si no se pudo subir la imagen propia, usamos una genérica para no dejar el campo vacío
                            firestoreRepository.updateRecipeImageUrl(recipeId, "https://images.unsplash.com/photo-1504674900247-0877df9cc836?q=80&w=1000&auto=format&fit=crop")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Fallo en el proceso de imagen: ${e.message}")
                        // Fallback: Si falla la generación por cuota, usamos imagen genérica
                        firestoreRepository.updateRecipeImageUrl(recipeId, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=1000&auto=format&fit=crop")
                    }

                    // 5. Agregar a las recetas del usuario
                    authRepository.currentUserId?.let { userId ->
                        userRepository.addRecipeToMyRecipes(userId, recipeId)
                    }

                    _messages.value = _messages.value + ChatMessage("¡Listo! La receta de **$name** ha sido guardada en tu colección personal. ¡Buen provecho! 🍳", false)
                },
                onFailure = { error ->
                    _messages.value = _messages.value + ChatMessage("🐭 Lo siento, mon ami, no pude guardar la receta: ${error.message}", false)
                }
            )

        } catch (e: Exception) {
            _messages.value = _messages.value + ChatMessage("🐭 Intenté crear una receta pero algo salió mal con los ingredientes... ¿Lo intentamos de nuevo?", false)
        }
    }
}
