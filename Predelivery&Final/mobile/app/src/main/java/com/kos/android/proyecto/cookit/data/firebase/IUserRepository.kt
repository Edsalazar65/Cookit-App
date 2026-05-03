package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.UserData
import kotlinx.coroutines.flow.Flow

interface IUserRepository {

    suspend fun createUserDocument(userId: String, userData: UserData): Result<Unit>

    suspend fun getUserData(userId: String): UserData?

    fun observeUserData(userId: String): Flow<UserData?>

    suspend fun toggleFavorite(userId: String, recipeId: String): Result<Unit>

    suspend fun toggleSaveRecipe(userId: String, recipeId: String): Result<Unit>

    suspend fun updateProfilePicture(userId: String, photoURL: String): Result<Unit>

    suspend fun addIngredient(userId: String, ingredient: String): Result<Unit>

    suspend fun removeIngredient(userId: String, ingredient: String): Result<Unit>
}