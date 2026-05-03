package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreRepository @Inject constructor() : IFirestoreRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val publicRecipesCollection = firestore.collection("public_recipes")

    override fun observePublicRecipes(): Flow<List<Recipe>> = callbackFlow {
        val listenerRegistration = publicRecipesCollection.addSnapshotListener { snapshot, error ->
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

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            publicRecipesCollection.document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error eliminando receta: ${e.message}"))
        }
    }
}
