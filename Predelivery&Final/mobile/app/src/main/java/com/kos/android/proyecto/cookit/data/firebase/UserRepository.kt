package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.UserData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor() : IUserRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    override suspend fun createUserDocument(userId: String, userData: UserData): Result<Unit> {
        return try {

            usersCollection.document(userId).set(userData.toFirestoreMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error creando documento de usuario: ${e.message}"))
        }
    }

    override suspend fun getUserData(userId: String): UserData? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                document.toObject(UserData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun observeUserData(userId: String): Flow<UserData?> = callbackFlow {
        val listenerRegistration = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }

            val userData = snapshot?.toObject(UserData::class.java)
            trySend(userData)
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun toggleFavorite(userId: String, recipeId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val favorites = userDoc.get("favorites") as? List<String> ?: emptyList()

            if (favorites.contains(recipeId)) {
                usersCollection.document(userId).update("favorites", FieldValue.arrayRemove(recipeId)).await()
            } else {
                usersCollection.document(userId).update("favorites", FieldValue.arrayUnion(recipeId)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleSaveRecipe(userId: String, recipeId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val myRecipes = userDoc.get("myRecipes") as? List<String> ?: emptyList()

            if (myRecipes.contains(recipeId)) {
                usersCollection.document(userId).update("myRecipes", FieldValue.arrayRemove(recipeId)).await()
            } else {
                usersCollection.document(userId).update("myRecipes", FieldValue.arrayUnion(recipeId)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addRecipeToMyRecipes(userId: String, recipeId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("myRecipes", FieldValue.arrayUnion(recipeId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePicture(userId: String, photoURL: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("photoURL", photoURL).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addIngredient(userId: String, ingredient: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("inventory", FieldValue.arrayUnion(ingredient)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeIngredient(userId: String, ingredient: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("inventory", FieldValue.arrayRemove(ingredient)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}