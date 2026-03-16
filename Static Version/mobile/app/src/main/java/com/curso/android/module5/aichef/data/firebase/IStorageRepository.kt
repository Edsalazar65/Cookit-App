package com.curso.android.module5.aichef.data.firebase

import android.graphics.Bitmap

/**
 * =============================================================================
 * IStorageRepository - Interface para Firebase Cloud Storage
 * =============================================================================
 *
 * CONCEPTO: Abstracción de Almacenamiento
 * Esta interface abstrae las operaciones de almacenamiento de archivos.
 * La implementación real usa Firebase Storage, pero podría reemplazarse
 * por S3, Azure Blob Storage, o almacenamiento local.
 *
 * =============================================================================
 */
interface IStorageRepository {

    /**
     * Sube una imagen a Storage
     * @param recipeId ID único de la receta (usado como nombre del archivo)
     * @param bitmap Imagen a subir
     * @return URL de descarga de la imagen subida
     */
    suspend fun uploadRecipeImage(recipeId: String, bitmap: Bitmap): Result<String>

    /**
     * Verifica si existe una imagen para la receta
     * @param recipeId ID de la receta
     * @return true si existe la imagen
     */
    suspend fun imageExists(recipeId: String): Boolean

    /**
     * Obtiene la URL de descarga de una imagen
     * @param recipeId ID de la receta
     * @return URL o null si no existe
     */
    suspend fun getImageUrl(recipeId: String): String?

    /**
     * Elimina la imagen de una receta
     * @param recipeId ID de la receta
     * @return Result indicando éxito o error
     */
    suspend fun deleteRecipeImage(recipeId: String): Result<Unit>
}
