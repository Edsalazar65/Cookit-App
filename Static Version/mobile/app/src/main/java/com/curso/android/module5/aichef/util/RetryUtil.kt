package com.curso.android.module5.aichef.util

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * =============================================================================
 * RETRY UTIL - Utilidades para reintentos con Exponential Backoff
 * =============================================================================
 *
 * CONCEPTO: Exponential Backoff
 * Cuando una operación falla (ej: llamada a API), en lugar de reintentar
 * inmediatamente, esperamos un tiempo que aumenta exponencialmente:
 *
 * Intento 1: Espera 1 segundo
 * Intento 2: Espera 2 segundos
 * Intento 3: Espera 4 segundos
 * Intento 4: Espera 8 segundos (capped a maxDelay)
 *
 * ¿POR QUÉ EXPONENTIAL BACKOFF?
 * 1. Evita sobrecargar un servidor caído con requests constantes
 * 2. Da tiempo al servidor para recuperarse
 * 3. Reduce consumo de batería/datos en el cliente
 * 4. Es el patrón estándar en sistemas distribuidos
 *
 * FÓRMULA:
 * delay = min(initialDelay * (factor ^ attempt), maxDelay)
 *
 * EJEMPLO CON VALORES DEFAULT:
 * - initialDelay = 1000ms (1 segundo)
 * - factor = 2.0 (duplica cada intento)
 * - maxDelay = 10000ms (máximo 10 segundos)
 *
 * Intento 0: 1000ms
 * Intento 1: 2000ms
 * Intento 2: 4000ms
 * Intento 3: 8000ms
 * Intento 4: 10000ms (capped)
 *
 * =============================================================================
 */

/**
 * Ejecuta un bloque con reintentos usando exponential backoff.
 *
 * @param maxRetries Número máximo de reintentos (default: 3)
 * @param initialDelayMs Delay inicial en milisegundos (default: 1000)
 * @param factor Multiplicador del delay por cada intento (default: 2.0)
 * @param maxDelayMs Delay máximo en milisegundos (default: 10000)
 * @param shouldRetry Lambda que determina si se debe reintentar basado en la excepción
 * @param block Bloque de código a ejecutar
 * @return Resultado del bloque si tiene éxito
 * @throws Exception La última excepción si todos los reintentos fallan
 *
 * EJEMPLO DE USO:
 * ```kotlin
 * val result = retryWithExponentialBackoff(
 *     maxRetries = 3,
 *     shouldRetry = { e -> e is IOException }
 * ) {
 *     apiService.fetchData()
 * }
 * ```
 */
suspend fun <T> retryWithExponentialBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000L,
    factor: Double = 2.0,
    maxDelayMs: Long = 10000L,
    shouldRetry: (Exception) -> Boolean = { true },
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e

            // Verificar si debemos reintentar
            if (attempt >= maxRetries || !shouldRetry(e)) {
                throw e
            }

            // Esperar antes del próximo intento
            delay(currentDelay)

            // Calcular próximo delay (exponential)
            currentDelay = min(
                (currentDelay * factor).toLong(),
                maxDelayMs
            )
        }
    }

    // Nunca debería llegar aquí, pero por seguridad
    throw lastException ?: IllegalStateException("Retry failed unexpectedly")
}

/**
 * Versión simplificada para casos comunes.
 * Solo reintenta en excepciones de red o temporales.
 *
 * @param maxRetries Número máximo de reintentos
 * @param block Bloque de código a ejecutar
 * @return Resultado del bloque
 */
suspend fun <T> retryOnNetworkError(
    maxRetries: Int = 3,
    block: suspend () -> T
): T = retryWithExponentialBackoff(
    maxRetries = maxRetries,
    shouldRetry = { e ->
        // Reintentar solo en errores que podrían ser temporales
        e.message?.contains("network", ignoreCase = true) == true ||
        e.message?.contains("timeout", ignoreCase = true) == true ||
        e.message?.contains("connection", ignoreCase = true) == true ||
        e.message?.contains("unavailable", ignoreCase = true) == true
    },
    block = block
)

/**
 * =============================================================================
 * NOTAS ADICIONALES SOBRE RETRY PATTERNS
 * =============================================================================
 *
 * 1. JITTER (VARIACIÓN ALEATORIA):
 *    Para evitar que múltiples clientes reintentos al mismo tiempo,
 *    se puede agregar variación aleatoria al delay:
 *    ```kotlin
 *    val jitter = Random.nextLong(0, currentDelay / 2)
 *    delay(currentDelay + jitter)
 *    ```
 *
 * 2. CIRCUIT BREAKER:
 *    Si hay muchos fallos consecutivos, dejar de intentar por un tiempo:
 *    ```kotlin
 *    class CircuitBreaker(
 *        private val failureThreshold: Int = 5,
 *        private val resetTimeout: Long = 60000
 *    ) {
 *        private var failures = 0
 *        private var lastFailure: Long = 0
 *
 *        fun shouldAllow(): Boolean {
 *            if (failures >= failureThreshold) {
 *                if (System.currentTimeMillis() - lastFailure > resetTimeout) {
 *                    failures = 0
 *                    return true
 *                }
 *                return false
 *            }
 *            return true
 *        }
 *    }
 *    ```
 *
 * 3. RETRY CON FALLBACK:
 *    Si todos los reintentos fallan, usar un valor por defecto:
 *    ```kotlin
 *    val result = runCatching {
 *        retryWithExponentialBackoff { api.fetch() }
 *    }.getOrElse { defaultValue }
 *    ```
 *
 * =============================================================================
 */
