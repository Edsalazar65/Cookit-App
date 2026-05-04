package com.kos.android.proyecto.cookit.data.firebase

import android.graphics.Bitmap

interface IStorageRepository {

    suspend fun uploadRecipeImage(recipeId: String, bitmap: Bitmap): Result<String>

    suspend fun deleteRecipeImage(recipeId: String): Result<Unit>

    suspend fun uploadProfilePicture(userId: String, bitmap: Bitmap): Result<String>
}
