package com.kos.android.proyecto.cookit.data.firebase

import com.kos.android.proyecto.cookit.domain.model.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


class AuthRepository @javax.inject.Inject constructor() : IAuthRepository {

    // Instancia de Firebase Auth
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Obtiene el ID del usuario actual
     * @return userId o null si no hay sesión
     */
    override val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Verifica si hay un usuario autenticado
     */
    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val state = if (user != null) {
                AuthState.Authenticated(
                    userId = user.uid,
                    email = user.email
                )
            } else {
                AuthState.Unauthenticated
            }
            trySend(state)
        }

        // Registrar el listener
        auth.addAuthStateListener(authStateListener)

        // Remover el listener cuando el Flow se cancela
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }


    override suspend fun signIn(email: String, password: String): Result<String> {

        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid
                ?: return Result.failure(Exception("Usuario no encontrado"))
            Result.success(userId)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Usuario no encontrado"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Contraseña incorrecta"))
        } catch (e: Exception) {
            Result.failure(Exception("Error de autenticación: ${e.message}"))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)

            val result = auth.signInWithCredential(credential).await()

            val userId = result.user?.uid
                ?: return Result.failure(Exception("No se pudo obtener el ID de usuario"))

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(Exception("Error en Google Auth: ${e.message}"))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid
                ?: return Result.failure(Exception("Error creando usuario"))
            Result.success(userId)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Correo electrónico inválido"))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este correo ya está registrado"))
        } catch (e: Exception) {
            Result.failure(Exception("Error de registro: ${e.message}"))
        }
    }


    override fun signOut() {
        auth.signOut()
    }
}
