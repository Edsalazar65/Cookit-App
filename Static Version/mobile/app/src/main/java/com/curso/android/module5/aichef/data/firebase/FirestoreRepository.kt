package com.curso.android.module5.aichef.data.firebase

import com.curso.android.module5.aichef.domain.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * =============================================================================
 * FirestoreRepository - Wrapper para Cloud Firestore
 * =============================================================================
 *
 * CONCEPTO: Cloud Firestore
 * Firestore es una base de datos NoSQL en la nube que ofrece:
 * - Sincronización en tiempo real
 * - Soporte offline automático
 * - Escalabilidad automática
 * - Seguridad basada en reglas
 *
 * ESTRUCTURA DE DATOS:
 * - Collections: Contenedores de documentos (como tablas)
 * - Documents: Objetos JSON con campos (como filas)
 * - Subcollections: Collections anidadas dentro de documentos
 *
 * En este proyecto:
 * recipes/
 *   └── {recipeId}/
 *         ├── userId: "abc123"
 *         ├── title: "Pasta Carbonara"
 *         ├── ingredients: ["pasta", "huevo", "queso"]
 *         ├── steps: ["Hervir pasta", "Mezclar..."]
 *         └── createdAt: 1701907200000
 *
 * SEGURIDAD:
 * Las reglas de Firestore deben configurarse para que:
 * - Solo usuarios autenticados puedan leer/escribir
 * - Cada usuario solo pueda ver sus propias recetas
 *
 * Ejemplo de reglas:
 * ```
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     match /recipes/{recipeId} {
 *       allow read, write: if request.auth != null
 *                          && request.auth.uid == resource.data.userId;
 *       allow create: if request.auth != null
 *                     && request.auth.uid == request.resource.data.userId;
 *     }
 *   }
 * }
 * ```
 *
 * =============================================================================
 */
class FirestoreRepository @javax.inject.Inject constructor() : IFirestoreRepository {

    // Instancia de Firestore
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Referencia a la colección de recetas
    private val recipesCollection = firestore.collection("recipes")

    /**
     * Observa las recetas de un usuario específico en tiempo real
     *
     * CONCEPTO: Snapshot Listeners -> Flow
     * Firestore usa listeners para notificar cambios en tiempo real.
     * callbackFlow convierte este patrón en un Flow de Kotlin.
     *
     * El Flow emite cada vez que:
     * - Se agrega una nueva receta
     * - Se modifica una receta existente
     * - Se elimina una receta
     * - Hay cambios en la conectividad (caché local)
     *
     * @param userId ID del usuario autenticado
     * @return Flow que emite la lista actualizada de recetas
     */
    override fun observeUserRecipes(userId: String): Flow<List<Recipe>> = callbackFlow {
        // Crear query para obtener solo las recetas del usuario
        // Ordenadas por fecha de creación (más recientes primero)
        val query = recipesCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        // Registrar listener para cambios en tiempo real
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // En caso de error, emitir lista vacía
                // Podrías también cerrar el flow con una excepción
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Convertir documentos a objetos Recipe
            val recipes = snapshot?.documents?.mapNotNull { document ->
                try {
                    val data = document.data ?: return@mapNotNull null
                    Recipe.fromFirestore(document.id, data)
                } catch (e: Exception) {
                    null // Ignorar documentos malformados
                }
            } ?: emptyList()

            // Emitir la lista actualizada
            trySend(recipes)
        }

        // Remover el listener cuando el Flow se cancela
        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Guarda una nueva receta en Firestore
     *
     * CONCEPTO: add() vs set()
     * - add(): Firestore genera un ID único automáticamente
     * - set(): Tú especificas el ID del documento
     *
     * Usamos add() para que cada receta tenga un ID único generado.
     *
     * @param recipe Receta a guardar (sin ID)
     * @return Result con el ID del documento creado o un error
     */
    override suspend fun saveRecipe(recipe: Recipe): Result<String> {
        return try {
            // add() retorna una referencia al documento creado
            val documentRef = recipesCollection.add(recipe.toMap()).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(Exception("Error guardando receta: ${e.message}"))
        }
    }

    /**
     * Obtiene una receta específica por su ID
     *
     * @param recipeId ID del documento
     * @return Recipe o null si no existe
     */
    override suspend fun getRecipe(recipeId: String): Recipe? {
        return try {
            val document = recipesCollection.document(recipeId).get().await()
            if (document.exists()) {
                val data = document.data ?: return null
                Recipe.fromFirestore(document.id, data)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina una receta por su ID
     *
     * @param recipeId ID del documento a eliminar
     * @return Result indicando éxito o error
     */
    override suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            recipesCollection.document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error eliminando receta: ${e.message}"))
        }
    }

    /**
     * Actualiza la URL de la imagen generada de una receta
     *
     * CONCEPTO: update() para campos específicos
     * update() permite modificar campos específicos sin reescribir
     * todo el documento. Más eficiente que set() para cambios parciales.
     *
     * @param recipeId ID del documento
     * @param imageUrl URL de la imagen en Firebase Storage
     * @return Result indicando éxito o error
     */
    override suspend fun updateGeneratedImageUrl(recipeId: String, imageUrl: String): Result<Unit> {
        return try {
            recipesCollection.document(recipeId)
                .update("generatedImageUrl", imageUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error actualizando imagen: ${e.message}"))
        }
    }
}
