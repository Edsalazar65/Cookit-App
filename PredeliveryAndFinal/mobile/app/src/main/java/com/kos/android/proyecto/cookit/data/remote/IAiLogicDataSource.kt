package com.kos.android.proyecto.cookit.data.remote

import android.graphics.Bitmap
import com.kos.android.proyecto.cookit.domain.model.GeneratedRecipe

/**
 * =============================================================================
 * IAiLogicDataSource - Interface para Firebase AI Logic (Gemini)
 * =============================================================================
 *
 * CONCEPTO: Abstracción de Servicios de IA
 * Esta interface define las operaciones de generación con IA.
 * Permite:
 * 1. Testear el ViewModel sin llamar a la API real de Gemini
 * 2. Implementar caching de respuestas
 * 3. Cambiar a otro modelo de IA sin afectar el código cliente
 *
 * EJEMPLO DE MOCK PARA TESTS:
 * ```kotlin
 * class FakeAiLogicDataSource : IAiLogicDataSource {
 *     override suspend fun generateRecipeFromImage(imageBitmap: Bitmap) =
 *         GeneratedRecipe(
 *             title = "Test Recipe",
 *             ingredients = listOf("Ingredient 1", "Ingredient 2"),
 *             steps = listOf("Step 1", "Step 2")
 *         )
 *
 *     override suspend fun generateRecipeImage(title: String, ingredients: List<String>) =
 *         Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
 * }
 * ```
 *
 * =============================================================================
 */
interface IAiLogicDataSource {

    /**
     * Genera una receta a partir de una imagen de ingredientes
     * @param imageBitmap Imagen de los ingredientes
     * @return GeneratedRecipe con título, ingredientes y pasos
     * @throws Exception si la generación falla
     */
    suspend fun generateRecipeFromImage(imageBitmap: Bitmap): GeneratedRecipe

    /**
     * Genera una imagen del plato terminado
     * @param recipeTitle Título de la receta
     * @param ingredients Lista de ingredientes principales
     * @return Bitmap de la imagen generada
     * @throws Exception si la generación falla
     */
    suspend fun generateRecipeImage(recipeTitle: String, ingredients: List<String>): Bitmap
}
