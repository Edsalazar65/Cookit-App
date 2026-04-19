package com.curso.android.module5.aichef.data.firebase

import com.curso.android.module5.aichef.domain.model.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * =============================================================================
 * AuthRepository - Wrapper para Firebase Authentication
 * =============================================================================
 *
 * CONCEPTO: Firebase Authentication
 * Firebase Auth maneja la autenticación de usuarios de forma segura:
 * - Email/Password: El método más común y simple
 * - OAuth: Google, Facebook, Twitter, GitHub, etc.
 * - Phone: Autenticación por SMS
 * - Anonymous: Usuarios temporales
 *
 * PERSISTENCIA DE SESIÓN:
 * Firebase Auth persiste la sesión automáticamente en el dispositivo.
 * El usuario permanece logueado hasta que llames a signOut().
 *
 * CONCEPTO: Repository Pattern para Auth
 * Encapsulamos Firebase Auth en un Repository para:
 * 1. Abstraer la implementación de Firebase
 * 2. Facilitar testing (mock del repository)
 * 3. Centralizar manejo de errores
 * 4. Convertir callbacks a coroutines/Flow
 *
 * =============================================================================
 */
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

    /**
     * Observa cambios en el estado de autenticación como Flow
     *
     * CONCEPTO: AuthStateListener -> Flow
     * Firebase usa listeners para notificar cambios de estado.
     * callbackFlow convierte este patrón en un Flow de Kotlin.
     *
     * El Flow emite cada vez que:
     * - El usuario inicia sesión
     * - El usuario cierra sesión
     * - El token se refresca
     *
     * @return Flow de AuthState
     */
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

    /**
     * Inicia sesión con email y password
     *
     * CONCEPTO: Tasks -> Coroutines
     * Firebase usa Task<T> para operaciones asíncronas.
     * .await() de kotlinx-coroutines-play-services convierte
     * el Task en una suspend function.
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @return Result con el userId o un error
     */
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

    /**
     * Registra un nuevo usuario con email y password
     *
     * @param email Correo electrónico del nuevo usuario
     * @param password Contraseña (mínimo 6 caracteres)
     * @return Result con el userId o un error
     */
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

    /**
     * Cierra la sesión del usuario actual
     *
     * Esta operación es síncrona y siempre exitosa.
     * Después de llamar signOut(), currentUser será null.
     */
    override fun signOut() {
        auth.signOut()
    }
}
