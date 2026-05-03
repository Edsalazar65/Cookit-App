package com.kos.android.proyecto.cookit.domain.model

data class Recipe(
    val id: String = "",
    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val imageURL: String = "",
    val difficulty: String= ""

) {

    fun toMap(): Map<String, Any> = mapOf(
        "ingredients" to ingredients,
        "steps" to steps,
        "imageURL" to imageURL,
        "id" to id,
        "name" to name,
        "difficulty" to difficulty
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(id: String, data: Map<String, Any?>): Recipe {
            return Recipe(
                id = id,
                name = data["name"] as? String ?: "",
                ingredients = (data["ingredients"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                steps = (data["steps"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                imageURL = data["imageURL"] as? String ?: "",
            )
        }
    }
}

data class GeneratedRecipe(
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>
)
