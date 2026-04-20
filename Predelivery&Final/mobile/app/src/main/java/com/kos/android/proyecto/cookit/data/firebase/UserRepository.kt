package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.UserData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor() : IUserRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    override suspend fun createUserDocument(userId: String, userData: UserData): Result<Unit> {
        return try {

            usersCollection.document(userId).set(userData.toFirestoreMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error creando documento de usuario: ${e.message}"))
        }
    }

    override suspend fun getUserData(userId: String): UserData? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                document.toObject(UserData::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}