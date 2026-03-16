package com.curso.android.module5.aichef.domain.model

/**
 * =============================================================================
 * UiState - Estados de la interfaz de usuario
 * =============================================================================
 *
 * CONCEPTO: Sealed Classes para Estado de UI
 * Usar sealed classes para representar estados permite:
 * 1. Exhaustividad: El compilador verifica que manejamos todos los casos
 * 2. Type-safety: Cada estado puede tener datos específicos
 * 3. Claridad: El código de UI es más legible
 *
 * PATRÓN: Loading/Success/Error
 * Este patrón es estándar para operaciones asíncronas:
 * - Idle: Estado inicial, sin operación en curso
 * - Loading: Operación en progreso, mostrar indicador
 * - Success: Operación exitosa, mostrar resultado
 * - Error: Operación fallida, mostrar mensaje de error
 *
 * =============================================================================
 */
sealed class UiState<out T> {
    /**
     * Estado inicial - Sin operación en curso
     */
    data object Idle : UiState<Nothing>()

    /**
     * Cargando - Operación en progreso
     * @param message Mensaje opcional para mostrar durante la carga
     */
    data class Loading(val message: String? = null) : UiState<Nothing>()

    /**
     * Éxito - Operación completada con resultado
     * @param data Datos resultantes de la operación
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Error - Operación fallida
     * @param message Mensaje de error para mostrar al usuario
     * @param exception Excepción original (opcional, para logging)
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : UiState<Nothing>()
}

/**
 * =============================================================================
 * AuthState - Estado específico de autenticación
 * =============================================================================
 */
sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}
