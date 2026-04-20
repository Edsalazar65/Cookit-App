package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.UserData

interface IUserRepository {

    suspend fun createUserDocument(userId: String, userData: UserData): Result<Unit>

    suspend fun getUserData(userId: String): UserData?
}