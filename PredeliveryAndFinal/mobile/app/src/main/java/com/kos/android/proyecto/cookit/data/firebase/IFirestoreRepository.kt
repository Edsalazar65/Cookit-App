package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface IFirestoreRepository {

    fun observePublicRecipes(): Flow<List<Recipe>>

    suspend fun saveRecipe(recipe: Recipe): Result<String>

    suspend fun updateRecipeImageUrl(recipeId: String, imageUrl: String): Result<Unit>

    suspend fun deleteRecipe(recipeId: String): Result<Unit>

    fun observeTrashedRecipes(): Flow<List<Recipe>>

    suspend fun moveToTrash(recipe: Recipe): Result<Unit>

    suspend fun restoreFromTrash(recipe: Recipe): Result<Unit>

    suspend fun permanentDelete(recipeId: String): Result<Unit>
}
