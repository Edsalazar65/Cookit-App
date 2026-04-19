package com.curso.android.module5.aichef.data.firebase

import com.curso.android.module5.aichef.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * =============================================================================
 * IFirestoreRepository - Interface para Cloud Firestore
 * =============================================================================
 *
 * CONCEPTO: Repository Interface Pattern
 * Esta interface define el contrato para operaciones con la base de datos.
 * La implementación real usa Firebase Firestore, pero en tests se puede
 * usar una implementación en memoria.
 *
 * BENEFICIOS:
 * 1. Los ViewModels no dependen de Firebase directamente
 * 2. Los tests no necesitan conexión a Firebase
 * 3. Se puede cambiar a otra BD sin modificar los ViewModels
 *
 * =============================================================================
 */
interface IFirestoreRepository {

    /**
     * Observa las recetas de un usuario en tiempo real
     * @param userId ID del usuario autenticado
     * @return Flow que emite la lista actualizada de recetas
     */
    fun observeUserRecipes(userId: String): Flow<List<Recipe>>

    /**
     * Guarda una nueva receta
     * @param recipe Receta a guardar
     * @return Result con el ID del documento creado
     */
    suspend fun saveRecipe(recipe: Recipe): Result<String>

    /**
     * Obtiene una receta por su ID
     * @param recipeId ID del documento
     * @return Recipe o null si no existe
     */
    suspend fun getRecipe(recipeId: String): Recipe?

    /**
     * Elimina una receta
     * @param recipeId ID del documento a eliminar
     * @return Result indicando éxito o error
     */
    suspend fun deleteRecipe(recipeId: String): Result<Unit>

    /**
     * Actualiza la URL de la imagen generada
     * @param recipeId ID del documento
     * @param imageUrl URL de la imagen en Storage
     * @return Result indicando éxito o error
     */
    suspend fun updateGeneratedImageUrl(recipeId: String, imageUrl: String): Result<Unit>
}
