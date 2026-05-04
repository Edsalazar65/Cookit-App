package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface IFirestoreRepository {

    fun observePublicRecipes(): Flow<List<Recipe>>

    suspend fun saveRecipe(recipe: Recipe): Result<String>

    suspend fun deleteRecipe(recipeId: String): Result<Unit>
}
