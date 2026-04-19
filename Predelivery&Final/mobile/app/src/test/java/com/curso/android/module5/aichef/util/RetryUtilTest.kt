package com.curso.android.module5.aichef.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * =============================================================================
 * RETRY UTIL TESTS - Tests unitarios para RetryUtil
 * =============================================================================
 *
 * CONCEPTO: Testing de Utilidades Puras
 * RetryUtil es una función pura (sin dependencias externas), lo que la hace
 * ideal para unit testing:
 * - No necesita mocks
 * - Comportamiento predecible
 * - Tests rápidos
 *
 * CONCEPTO: runTest
 * Para testear funciones suspend, usamos `runTest` de kotlinx-coroutines-test.
 * Esto ejecuta coroutines en un dispatcher de test que:
 * - Salta delays automáticamente (virtual time)
 * - Asegura que todos los coroutines terminen
 * - Detecta coroutines que no terminan
 *
 * =============================================================================
 */
class RetryUtilTest {

    /**
     * Test: Operación exitosa en el primer intento
     *
     * Verifica que si la operación tiene éxito inmediatamente,
     * no se realizan reintentos innecesarios.
     */
    @Test
    fun `retryWithExponentialBackoff returns result on first success`() = runTest {
        var attempts = 0

        val result = retryWithExponentialBackoff {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    /**
     * Test: Éxito después de fallos
     *
     * Simula una operación que falla 2 veces antes de tener éxito.
     * Verifica que el retry funciona correctamente.
     */
    @Test
    fun `retryWithExponentialBackoff succeeds after failures`() = runTest {
        var attempts = 0

        val result = retryWithExponentialBackoff(maxRetries = 3) {
            attempts++
            if (attempts < 3) {
                throw IOException("Network error")
            }
            "success after retries"
        }

        assertEquals("success after retries", result)
        assertEquals(3, attempts)
    }

    /**
     * Test: Fallo después de agotar reintentos
     *
     * Verifica que después de maxRetries intentos fallidos,
     * la excepción se propaga correctamente.
     */
    @Test
    fun `retryWithExponentialBackoff throws after max retries`() = runTest {
        var attempts = 0

        val exception = assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                retryWithExponentialBackoff(maxRetries = 2) {
                    attempts++
                    throw IOException("Always fails")
                }
            }
        }

        assertEquals("Always fails", exception.message)
        assertEquals(3, attempts) // 1 inicial + 2 reintentos
    }

    /**
     * Test: shouldRetry predicate respetado
     *
     * Verifica que el predicado shouldRetry controla qué errores
     * se reintentan y cuáles se propagan inmediatamente.
     */
    @Test
    fun `retryWithExponentialBackoff respects shouldRetry predicate`() = runTest {
        var attempts = 0

        val exception = assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                retryWithExponentialBackoff(
                    maxRetries = 5,
                    shouldRetry = { e -> e is IOException }
                ) {
                    attempts++
                    // IllegalArgumentException no debe reintentarse
                    throw IllegalArgumentException("Invalid input")
                }
            }
        }

        assertEquals("Invalid input", exception.message)
        assertEquals(1, attempts) // Solo 1 intento, sin reintentos
    }

    /**
     * Test: shouldRetry permite reintentar errores específicos
     *
     * Verifica que IOException sí se reintenta cuando el predicado lo permite.
     */
    @Test
    fun `retryWithExponentialBackoff retries when shouldRetry returns true`() = runTest {
        var attempts = 0

        val result = retryWithExponentialBackoff(
            maxRetries = 3,
            shouldRetry = { e -> e is IOException }
        ) {
            attempts++
            if (attempts < 2) {
                throw IOException("Temporary failure")
            }
            "recovered"
        }

        assertEquals("recovered", result)
        assertEquals(2, attempts)
    }

    /**
     * Test: retryOnNetworkError helper
     *
     * Verifica que la función helper reintenta solo errores de red.
     */
    @Test
    fun `retryOnNetworkError retries on network messages`() = runTest {
        var attempts = 0

        val result = retryOnNetworkError(maxRetries = 3) {
            attempts++
            if (attempts < 2) {
                throw Exception("Connection timeout")
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, attempts)
    }

    /**
     * Test: Delays aumentan exponencialmente
     *
     * Verifica que los delays siguen el patrón exponencial.
     * Usamos runTest que salta los delays automáticamente.
     */
    @Test
    fun `retryWithExponentialBackoff uses exponential delays`() = runTest {
        val delays = mutableListOf<Long>()
        var lastTime = 0L
        var attempts = 0

        try {
            retryWithExponentialBackoff(
                maxRetries = 3,
                initialDelayMs = 100L,
                factor = 2.0
            ) {
                attempts++
                val currentTime = testScheduler.currentTime
                if (lastTime > 0) {
                    delays.add(currentTime - lastTime)
                }
                lastTime = currentTime
                throw IOException("Always fails")
            }
        } catch (e: IOException) {
            // Esperado
        }

        // Con virtual time, los delays se registran
        // Delay 1: 100ms, Delay 2: 200ms, Delay 3: 400ms
        assertEquals(3, delays.size)
        assertTrue("First delay should be ~100ms", delays[0] >= 100)
        assertTrue("Second delay should be ~200ms", delays[1] >= 200)
        assertTrue("Third delay should be ~400ms", delays[2] >= 400)
    }

    /**
     * Test: maxDelay respetado
     *
     * Verifica que el delay nunca excede maxDelayMs.
     */
    @Test
    fun `retryWithExponentialBackoff respects maxDelay`() = runTest {
        var attempts = 0
        val delays = mutableListOf<Long>()
        var lastTime = 0L

        try {
            retryWithExponentialBackoff(
                maxRetries = 5,
                initialDelayMs = 100L,
                factor = 10.0,  // Crece muy rápido
                maxDelayMs = 500L
            ) {
                attempts++
                val currentTime = testScheduler.currentTime
                if (lastTime > 0) {
                    delays.add(currentTime - lastTime)
                }
                lastTime = currentTime
                throw IOException("Always fails")
            }
        } catch (e: IOException) {
            // Esperado
        }

        // Ningún delay debe exceder maxDelayMs
        delays.forEach { delay ->
            assertTrue("Delay $delay should not exceed 500ms", delay <= 500)
        }
    }
}

/**
 * =============================================================================
 * NOTAS SOBRE TESTING CON COROUTINES
 * =============================================================================
 *
 * 1. runTest vs runBlocking:
 *    - runTest: Usa virtual time, salta delays, detecta leaks
 *    - runBlocking: Usa tiempo real, tests más lentos
 *
 *    SIEMPRE preferir runTest para tests unitarios.
 *
 * 2. testScheduler.currentTime:
 *    Permite verificar cuánto "tiempo virtual" ha pasado.
 *    Útil para verificar delays sin esperar tiempo real.
 *
 * 3. advanceTimeBy / advanceUntilIdle:
 *    Controlan manualmente el avance del tiempo virtual.
 *    ```kotlin
 *    advanceTimeBy(1000) // Avanza 1 segundo
 *    advanceUntilIdle() // Ejecuta todo el trabajo pendiente
 *    ```
 *
 * 4. UnconfinedTestDispatcher vs StandardTestDispatcher:
 *    - Unconfined: Ejecuta coroutines inmediatamente
 *    - Standard: Requiere avanzar manualmente (más control)
 *
 * =============================================================================
 */
