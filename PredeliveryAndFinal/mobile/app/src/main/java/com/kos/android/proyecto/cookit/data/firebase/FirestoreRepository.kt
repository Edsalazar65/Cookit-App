package com.kos.android.proyecto.cookit.data.firebase

import android.util.Log
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
    private val trashedRecipesCollection = firestore.collection("trashed_recipes")

    override fun observePublicRecipes(): Flow<List<Recipe>> = callbackFlow {
        val listenerRegistration = publicRecipesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val recipes = snapshot?.documents?.mapNotNull { document ->
                try {
                    Recipe.fromFirestore(document.id, document.data ?: return@mapNotNull null)
                } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(recipes)
        }
        awaitClose { listenerRegistration.remove() }
    }

    override fun observeTrashedRecipes(): Flow<List<Recipe>> = callbackFlow {
        Log.d("FirestoreRepository", "Starting trash bin snapshot listener...")
        val listenerRegistration = trashedRecipesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreRepository", "Trash Bin listener error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val recipes = snapshot?.documents?.mapNotNull { document ->
                try {
                    Recipe.fromFirestore(document.id, document.data ?: return@mapNotNull null)
                } catch (e: Exception) { 
                    Log.e("FirestoreRepository", "Error parsing trashed recipe: ${e.message}")
                    null 
                }
            } ?: emptyList()
            Log.d("FirestoreRepository", "Trash snapshot received: ${recipes.size} recipes")
            trySend(recipes)
        }
        awaitClose { 
            Log.d("FirestoreRepository", "Closing trash bin snapshot listener")
            listenerRegistration.remove() 
        }
    }

    override suspend fun moveToTrash(recipe: Recipe): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.set(trashedRecipesCollection.document(recipe.id), recipe.toMap())
            batch.delete(publicRecipesCollection.document(recipe.id))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun restoreFromTrash(recipe: Recipe): Result<Unit> {
        return try {
            val batch = firestore.batch()
            batch.set(publicRecipesCollection.document(recipe.id), recipe.toMap())
            batch.delete(trashedRecipesCollection.document(recipe.id))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun permanentDelete(recipeId: String): Result<Unit> {
        return try {
            trashedRecipesCollection.document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun saveRecipe(recipe: Recipe): Result<String> {
        return try {
            val docRef = if (recipe.id.isNotEmpty()) publicRecipesCollection.document(recipe.id) else publicRecipesCollection.document()
            val finalRecipe = if (recipe.id.isEmpty()) recipe.copy(id = docRef.id) else recipe
            docRef.set(finalRecipe.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateRecipeImageUrl(recipeId: String, imageUrl: String): Result<Unit> {
        return try {
            publicRecipesCollection.document(recipeId).update("imageURL", imageUrl).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            publicRecipesCollection.document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
