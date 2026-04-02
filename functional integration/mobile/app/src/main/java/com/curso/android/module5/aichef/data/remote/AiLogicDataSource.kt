package com.curso.android.module5.aichef.data.remote

import android.graphics.Bitmap
import com.curso.android.module5.aichef.domain.model.GeneratedRecipe
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

/**
 * =============================================================================
 * AiLogicDataSource - Fuente de datos para Firebase AI Logic
 * =============================================================================
 *
 * CONCEPTO: Firebase AI Logic (2025)
 * Firebase AI Logic es el SDK unificado que reemplaza a:
 * - firebase-vertexai (renombrado)
 * - google-ai-client SDK (deprecado para producción)
 *
 * VENTAJAS DE FIREBASE AI LOGIC:
 * 1. Seguridad sin API Keys: Las credenciales se manejan server-side
 * 2. Firebase App Check: Protección contra uso no autorizado
 * 3. Integración nativa: Funciona con Auth, Firestore, y Storage
 * 4. Billing unificado: Se cobra a través de Firebase/GCP
 *
 * BACKENDS DISPONIBLES:
 * - GenerativeBackend.googleAI(): Gemini Developer API (recomendado para empezar)
 * - GenerativeBackend.vertexAI(): Vertex AI (más control, enterprise)
 *
 * MODELOS DISPONIBLES (Diciembre 2025):
 * - gemini-3-pro-preview: El más avanzado (preview)
 * - gemini-3-flash-preview: Rápido y eficiente, ideal para producción (preview)
 * - gemini-3-pro-image-preview: Generación de imágenes (preview)
 *
 * =============================================================================
 */
class AiLogicDataSource @javax.inject.Inject constructor() : IAiLogicDataSource {

    /**
     * CONCEPTO: Inicialización del modelo generativo
     *
     * Firebase.ai es el punto de entrada al SDK.
     * - backend: Especifica qué API usar (googleAI o vertexAI)
     * - generativeModel: Crea una instancia del modelo específico
     *
     * La instancia del modelo se puede reutilizar para múltiples requests.
     */
    private val generativeModel = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel("gemini-3-flash-preview")

    /**
     * Modelo para generación de imágenes
     *
     * CONCEPTO: Gemini Image Generation
     * El modelo gemini-3-pro-image-preview puede generar imágenes
     * a partir de descripciones de texto.
     *
     * IMPORTANTE: Se debe configurar responseModalities para incluir TEXT e IMAGE
     */
    private val imageModel = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel(
        modelName = "gemini-3-pro-image-preview",
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
        }
    )

    /**
     * Genera una receta a partir de una imagen de ingredientes
     *
     * CONCEPTO: Contenido Multimodal
     * Gemini puede procesar múltiples tipos de contenido:
     * - Texto: Prompts, instrucciones
     * - Imágenes: Bitmaps, URIs
     * - Audio/Video: En modelos que lo soporten
     *
     * El contenido se construye usando el builder `content { }`:
     * - image(bitmap): Agrega una imagen
     * - text(string): Agrega texto
     *
     * FLUJO:
     * 1. Construir el prompt con imagen + texto
     * 2. Llamar a generateContent (suspend function)
     * 3. Parsear la respuesta de texto
     *
     * @param imageBitmap Imagen de los ingredientes
     * @return GeneratedRecipe con título, ingredientes y pasos
     * @throws Exception si la generación falla
     */
    override suspend fun generateRecipeFromImage(imageBitmap: Bitmap): GeneratedRecipe {
        // Construir el prompt multimodal
        val prompt = content {
            // Primero la imagen
            image(imageBitmap)

            // Luego las instrucciones de texto
            text(
                """
                Analiza esta imagen de ingredientes y genera una receta deliciosa.

                Responde EXACTAMENTE en este formato (sin markdown, sin asteriscos):

                TÍTULO: [nombre creativo de la receta]

                INGREDIENTES:
                - [ingrediente 1 con cantidad]
                - [ingrediente 2 con cantidad]
                - [ingrediente 3 con cantidad]
                (lista todos los ingredientes visibles)

                PASOS:
                1. [primer paso detallado]
                2. [segundo paso detallado]
                3. [tercer paso detallado]
                (incluye todos los pasos necesarios)

                Sé creativo con el nombre y detallado en las instrucciones.
                Si no puedes identificar ingredientes claros, sugiere una receta
                basada en lo que parece ser la imagen.
                """.trimIndent()
            )
        }

        // Llamar al modelo y obtener respuesta
        // generateContent es una suspend function - debe llamarse desde coroutine
        val response = generativeModel.generateContent(prompt)

        // Extraer el texto de la respuesta
        val responseText = response.text ?: throw Exception("Respuesta vacía del modelo")

        // Parsear la respuesta estructurada
        return parseRecipeResponse(responseText)
    }

    /**
     * Parsea la respuesta de texto del modelo a un objeto estructurado
     *
     * NOTA: En producción, considera usar JSON Schema con Gemini
     * para obtener respuestas estructuradas directamente.
     */
    private fun parseRecipeResponse(response: String): GeneratedRecipe {
        // Extraer título
        val titleRegex = Regex("""TÍTULO:\s*(.+)""", RegexOption.IGNORE_CASE)
        val title = titleRegex.find(response)?.groupValues?.get(1)?.trim()
            ?: "Receta Misteriosa"

        // Extraer ingredientes (líneas que empiezan con -)
        val ingredientsSection = response
            .substringAfter("INGREDIENTES:", "")
            .substringBefore("PASOS:", "")

        val ingredients = ingredientsSection
            .lines()
            .map { it.trim() }
            .filter { it.startsWith("-") || it.startsWith("•") }
            .map { it.removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() }

        // Extraer pasos (líneas numeradas)
        val stepsSection = response.substringAfter("PASOS:", "")

        val steps = stepsSection
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.first().isDigit() }
            .map { it.replace(Regex("""^\d+\.\s*"""), "").trim() }
            .filter { it.isNotBlank() }

        return GeneratedRecipe(
            title = title,
            ingredients = ingredients.ifEmpty { listOf("Ingredientes no identificados") },
            steps = steps.ifEmpty { listOf("Sigue tu intuición culinaria") }
        )
    }

    /**
     * =========================================================================
     * generateRecipeImage - Genera imagen del plato terminado con IA
     * =========================================================================
     *
     * CONCEPTO: Generación de Imágenes con Gemini
     * El modelo gemini-3-pro-image-preview puede generar imágenes fotorealistas
     * a partir de descripciones de texto. Esta es una capacidad multimodal
     * donde el modelo "imagina" cómo se vería el plato terminado.
     *
     * CONFIGURACIÓN REQUERIDA:
     * El modelo DEBE configurarse con responseModalities que incluya IMAGE:
     * ```kotlin
     * generationConfig {
     *     responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
     * }
     * ```
     *
     * ESTRUCTURA DE LA RESPUESTA:
     * La respuesta de Gemini contiene múltiples "parts":
     * - TextPart: Texto generado (descripción, explicación)
     * - ImagePart: Imagen generada como Bitmap
     *
     * Usamos filterIsInstance<ImagePart>() para obtener solo las partes
     * que son imágenes, y luego accedemos a .image para obtener el Bitmap.
     *
     * PROMPT ENGINEERING PARA IMÁGENES:
     * Para obtener mejores resultados, el prompt debe ser específico sobre:
     * - Estilo visual (fotografía profesional de comida)
     * - Ángulo de la imagen (cenital, 45 grados)
     * - Iluminación (cálida, natural)
     * - Presentación (elegante, estilo restaurante)
     *
     * @param recipeTitle Título de la receta para contexto
     * @param ingredients Lista de ingredientes principales
     * @return Bitmap de la imagen generada del plato
     * @throws Exception si la generación falla o no se obtiene imagen
     */
    override suspend fun generateRecipeImage(recipeTitle: String, ingredients: List<String>): Bitmap {
        // Limitamos a 5 ingredientes para no sobrecargar el prompt
        val ingredientsList = ingredients.take(5).joinToString(", ")

        // =====================================================================
        // PROMPT PARA GENERACIÓN DE IMAGEN
        // =====================================================================
        // El prompt describe el tipo de imagen que queremos generar.
        // Usamos inglés para mejor compatibilidad con el modelo.
        val prompt = content {
            text(
                """
                Generate a professional food photography image of a finished dish:

                Dish name: $recipeTitle
                Main ingredients: $ingredientsList

                Requirements:
                - Photorealistic, appetizing food photography style
                - Beautiful plating on a nice plate or dish
                - Warm, inviting lighting
                - Top-down or 45-degree angle view
                - Clean, elegant presentation
                - High quality, restaurant-style presentation
                """.trimIndent()
            )
        }

        // Generar contenido con el modelo de imagen
        val response = imageModel.generateContent(prompt)

        // =====================================================================
        // EXTRACCIÓN DE LA IMAGEN
        // =====================================================================
        // La respuesta contiene múltiples "candidates" (alternativas).
        // Cada candidate tiene "content" con "parts".
        // Filtramos para obtener solo ImagePart y extraemos el Bitmap.
        //
        // Cadena de null-safety:
        // - firstOrNull(): Primer candidate o null
        // - ?.content: Contenido del candidate
        // - ?.parts: Lista de partes
        // - ?.filterIsInstance<ImagePart>(): Solo partes de imagen
        // - ?.firstOrNull(): Primera imagen
        // - ?.image: El Bitmap final
        val bitmap = response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
            ?: throw Exception("No se pudo generar la imagen")

        return bitmap
    }
}
