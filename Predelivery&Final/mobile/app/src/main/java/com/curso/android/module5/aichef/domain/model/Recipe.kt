package com.curso.android.module5.aichef.domain.model

data class Recipe(
    val id: String = "",
    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val imageUrl: String = "",

) {

    fun toMap(): Map<String, Any> = mapOf(
        "ingredients" to ingredients,
        "steps" to steps,
        "imageUrl" to imageUrl,
        "id" to id,
        "name" to name
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(id: String, data: Map<String, Any?>): Recipe {
            return Recipe(
                id = id,
                name = data["name"] as? String ?: "",
                ingredients = (data["ingredients"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                steps = (data["steps"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                imageUrl = data["imageUrl"] as? String ?: "",
            )
        }
    }
}

data class GeneratedRecipe(
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>
)
