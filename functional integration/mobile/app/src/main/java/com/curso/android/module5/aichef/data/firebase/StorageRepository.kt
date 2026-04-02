package com.curso.android.module5.aichef.data.firebase

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * =============================================================================
 * StorageRepository - Repositorio para Firebase Cloud Storage
 * =============================================================================
 *
 * CONCEPTO: Firebase Cloud Storage
 * Firebase Storage permite almacenar y servir archivos (imágenes, videos, etc.)
 * de forma segura y escalable. Se integra con Firebase Auth para control de acceso.
 *
 * USO EN ESTE PROYECTO:
 * Almacenamos las imágenes generadas por Gemini para:
 * 1. Evitar regenerar la imagen cada vez (ahorro de cuota API)
 * 2. Carga más rápida en visitas posteriores
 * 3. Persistencia de las imágenes generadas
 *
 * ESTRUCTURA DE ALMACENAMIENTO:
 * ```
 * gs://[bucket]/
 * └── recipe_images/
 *     └── {recipeId}.jpg
 * ```
 *
 * SEGURIDAD:
 * Las reglas de Storage deben configurarse para permitir:
 * - Lectura: usuarios autenticados
 * - Escritura: usuarios autenticados (en su propia carpeta idealmente)
 *
 * =============================================================================
 */
class StorageRepository @javax.inject.Inject constructor() : IStorageRepository {

    // Referencia a Firebase Storage
    private val storage = Firebase.storage

    // Referencia a la carpeta de imágenes de recetas
    private val recipeImagesRef = storage.reference.child("recipe_images")

    /**
     * Sube una imagen generada a Firebase Storage
     *
     * CONCEPTO: Compresión de Bitmap
     * Antes de subir, comprimimos el Bitmap a JPEG para reducir tamaño.
     * Usamos ByteArrayOutputStream para convertir a bytes.
     *
     * CONCEPTO: putBytes vs putFile
     * - putBytes: Sube bytes directamente (usado aquí para Bitmap)
     * - putFile: Sube desde un Uri local (para archivos del dispositivo)
     *
     * @param recipeId ID único de la receta (usado como nombre del archivo)
     * @param bitmap Imagen generada por Gemini
     * @return URL de descarga de la imagen subida
     */
    override suspend fun uploadRecipeImage(recipeId: String, bitmap: Bitmap): Result<String> {
        return try {
            // Crear referencia al archivo: recipe_images/{recipeId}.jpg
            val imageRef = recipeImagesRef.child("$recipeId.jpg")

            // Comprimir Bitmap a JPEG
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val imageData = baos.toByteArray()

            // Subir bytes a Storage
            imageRef.putBytes(imageData).await()

            // Obtener URL de descarga pública
            val downloadUrl = imageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifica si existe una imagen para la receta
     *
     * CONCEPTO: Metadata de Storage
     * Podemos obtener metadata de un archivo para verificar si existe
     * sin necesidad de descargar el archivo completo.
     *
     * @param recipeId ID de la receta
     * @return true si existe la imagen, false si no
     */
    override suspend fun imageExists(recipeId: String): Boolean {
        return try {
            val imageRef = recipeImagesRef.child("$recipeId.jpg")
            imageRef.metadata.await()
            true
        } catch (e: Exception) {
            // Si hay excepción, el archivo no existe
            false
        }
    }

    /**
     * Obtiene la URL de descarga de una imagen existente
     *
     * @param recipeId ID de la receta
     * @return URL de descarga o null si no existe
     */
    override suspend fun getImageUrl(recipeId: String): String? {
        return try {
            val imageRef = recipeImagesRef.child("$recipeId.jpg")
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina la imagen de una receta
     *
     * Útil cuando se elimina una receta para limpiar Storage.
     *
     * @param recipeId ID de la receta
     */
    override suspend fun deleteRecipeImage(recipeId: String): Result<Unit> {
        return try {
            val imageRef = recipeImagesRef.child("$recipeId.jpg")
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Si no existe, no es un error crítico
            Result.success(Unit)
        }
    }
}
