package com.curso.android.module5.aichef.data.firebase

import com.curso.android.module5.aichef.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * =============================================================================
 * IAuthRepository - Interface para Autenticación
 * =============================================================================
 *
 * CONCEPTO: Interfaces para Testabilidad
 * Definir interfaces para los repositorios permite:
 * 1. Inversión de Dependencias (DIP): El ViewModel depende de abstracciones
 * 2. Testabilidad: En tests, se puede usar una implementación mock
 * 3. Flexibilidad: Cambiar implementación sin afectar el resto del código
 *
 * EJEMPLO DE USO EN TESTS:
 * ```kotlin
 * class FakeAuthRepository : IAuthRepository {
 *     override val currentUserId: String? = "test-user-id"
 *     override val isLoggedIn: Boolean = true
 *
 *     override fun observeAuthState(): Flow<AuthState> =
 *         flowOf(AuthState.Authenticated("test-user-id", "test@example.com"))
 *
 *     override suspend fun signIn(email: String, password: String) =
 *         Result.success("test-user-id")
 *     // ...
 * }
 * ```
 *
 * =============================================================================
 */
interface IAuthRepository {

    /**
     * ID del usuario actualmente autenticado
     */
    val currentUserId: String?

    /**
     * Indica si hay un usuario autenticado
     */
    val isLoggedIn: Boolean

    /**
     * Observa cambios en el estado de autenticación
     * @return Flow que emite el estado actual (Authenticated o Unauthenticated)
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Inicia sesión con email y contraseña
     * @return Result con el userId o un error
     */
    suspend fun signIn(email: String, password: String): Result<String>

    /**
     * Registra un nuevo usuario
     * @return Result con el userId o un error
     */
    suspend fun signUp(email: String, password: String): Result<String>

    /**
     * Cierra la sesión del usuario actual
     */
    fun signOut()
}
