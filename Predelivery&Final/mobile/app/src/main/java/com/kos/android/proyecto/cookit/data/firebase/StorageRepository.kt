package com.kos.android.proyecto.cookit.data.firebase

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class StorageRepository @Inject constructor() : IStorageRepository {

    private val storage = Firebase.storage

    private val recipeImagesRef = storage.reference.child("recipe_images")
    private val profileImagesRef = storage.reference.child("avatars")

    override suspend fun uploadRecipeImage(recipeId: String, bitmap: Bitmap): Result<String> {
        return try {
            val imageRef = recipeImagesRef.child("$recipeId.png")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val imageData = baos.toByteArray()
            imageRef.putBytes(imageData).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecipeImage(recipeId: String): Result<Unit> {
        return try {
            val imageRef = recipeImagesRef.child("$recipeId.png")
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun uploadProfilePicture(userId: String, bitmap: Bitmap): Result<String> {
        return try {
            val imageRef = profileImagesRef.child("$userId.jpg")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val imageData = baos.toByteArray()
            imageRef.putBytes(imageData).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
