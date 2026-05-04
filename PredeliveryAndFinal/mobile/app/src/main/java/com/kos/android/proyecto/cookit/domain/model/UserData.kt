package com.kos.android.proyecto.cookit.domain.model

data class UserData(
    val email: String = "",
    val name: String = "",
    val photoURL: String = "",
    val favorites: List<String> = emptyList(),
    val inventory: List<String> = emptyList(),
    val myRecipes: List<String> = emptyList()
) {
    // Convierte el objeto a un mapa para guardarlo en Firestore
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "email" to email,
            "name" to name,
            "photoURL" to photoURL,
            "favorites" to favorites,
            "inventory" to inventory,
            "myRecipes" to myRecipes
        )
    }
}