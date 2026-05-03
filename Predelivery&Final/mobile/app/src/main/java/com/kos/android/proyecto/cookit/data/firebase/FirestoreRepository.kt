package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


class FirestoreRepository @javax.inject.Inject constructor() : IFirestoreRepository {

    // Instancia de Firestore
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Referencia a la colección de recetas
    private val recipesCollection = firestore.collection("recipes")

    private val publicRecipesCollection = firestore.collection("public_recipes")

    override fun observeUserRecipes(userId: String): Flow<List<Recipe>> = callbackFlow {
        // Crear query para obtener solo las recetas del usuario
        // Ordenadas por fecha de creación (más recientes primero)
        val query = recipesCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        // Registrar listener para cambios en tiempo real
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val recipes = snapshot?.documents?.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    Recipe.fromFirestore(document.id, data)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(recipes)
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun observePublicRecipes(): Flow<List<Recipe>> = callbackFlow {
        val listenerRegistration = publicRecipesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val recipes = snapshot?.documents?.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    // Asumiendo que usas Recipe.fromFirestore o mapeas los datos manualmente como en tu otra función
                    Recipe.fromFirestore(document.id, data)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(recipes)
        }

        awaitClose { listenerRegistration.remove() }
    }


    override suspend fun saveRecipe(recipe: Recipe): Result<String> {
        return try {
            val documentRef = recipesCollection.add(recipe.toMap()).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(Exception("Error guardando receta: ${e.message}"))
        }
    }


    override suspend fun getRecipe(recipeId: String): Recipe? {
        return try {
            val document = recipesCollection.document(recipeId).get().await()
            if (document.exists()) {
                val data = document.data ?: return null
                Recipe.fromFirestore(document.id, data)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            recipesCollection.document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error eliminando receta: ${e.message}"))
        }
    }



    override suspend fun updateGeneratedImageUrl(recipeId: String, imageUrl: String): Result<Unit> {
        return try {
            recipesCollection.document(recipeId)
                .update("generatedImageUrl", imageUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error actualizando imagen: ${e.message}"))
        }
    }
}
