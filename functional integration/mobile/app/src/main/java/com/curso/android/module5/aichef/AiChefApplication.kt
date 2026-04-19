package com.curso.android.module5.aichef

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

/**
 * =============================================================================
 * AiChefApplication - Clase Application de la aplicación
 * =============================================================================
 *
 * CONCEPTO: @HiltAndroidApp
 * Esta anotación es OBLIGATORIA para usar Hilt en tu app.
 * Genera el código necesario para inyección de dependencias.
 *
 * ¿QUÉ HACE @HiltAndroidApp?
 * 1. Genera una clase base que Application extiende
 * 2. Crea el componente raíz de Hilt (SingletonComponent)
 * 3. Inicializa el grafo de dependencias automáticamente
 *
 * CONCEPTO: Inicialización de Firebase
 * Firebase se inicializa automáticamente gracias al plugin google-services
 * y el archivo google-services.json. No es necesario llamar
 * FirebaseApp.initializeApp() manualmente en la mayoría de casos.
 *
 * CONCEPTO: Firebase AI Logic y Seguridad
 * A diferencia del SDK cliente de Google AI (com.google.ai.client.generativeai),
 * Firebase AI Logic NO requiere API Keys hardcodeadas en el código.
 *
 * La seguridad se maneja mediante:
 * 1. google-services.json: Contiene configuración del proyecto
 * 2. Firebase App Check: Verifica que las requests vienen de tu app
 * 3. Reglas de Firestore/Storage: Control de acceso a datos
 *
 * =============================================================================
 * APP CHECK - DEBUG VS PRODUCCIÓN
 * =============================================================================
 *
 * Firebase AI Logic REQUIERE App Check habilitado para funcionar.
 *
 * EN DESARROLLO (este código):
 * - Usamos DebugAppCheckProviderFactory
 * - Permite probar sin configuración adicional
 * - El token de debug se genera automáticamente
 *
 * PARA REGISTRAR EL TOKEN DE DEBUG EN FIREBASE CONSOLE:
 * 1. Ejecuta la app en debug
 * 2. Busca en Logcat: "DebugAppCheckProvider: Enter this debug secret..."
 * 3. Copia el token
 * 4. Ve a Firebase Console > App Check > Apps > Manage debug tokens
 * 5. Agrega el token copiado
 *
 * EN PRODUCCIÓN:
 * ```kotlin
 * // Reemplazar DebugAppCheckProviderFactory por:
 * Firebase.appCheck.installAppCheckProviderFactory(
 *     PlayIntegrityAppCheckProviderFactory.getInstance()
 * )
 * ```
 *
 * IMPORTANTE: Play Integrity requiere configuración adicional:
 * 1. Agregar dependencia: firebase-appcheck-playintegrity
 * 2. Habilitar Play Integrity API en Google Cloud Console
 * 3. Registrar la app en Firebase Console > App Check
 *
 * =============================================================================
 */
@HiltAndroidApp
class AiChefApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Firebase
        Firebase.initialize(this)

        // =====================================================================
        // FIREBASE APP CHECK - CONFIGURACIÓN DE SEGURIDAD
        // =====================================================================
        // App Check verifica que las requests a Firebase vienen de tu app real,
        // no de scripts maliciosos o apps modificadas.
        //
        // DebugAppCheckProviderFactory: SOLO PARA DESARROLLO
        // - Genera un token de debug que debes registrar en Firebase Console
        // - NO usar en producción (cualquiera podría falsificar el token)
        //
        // Para producción, cambiar a PlayIntegrityAppCheckProviderFactory
        // Ver documentación arriba para instrucciones.
        // =====================================================================
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Guardar referencia para uso global (opcional, ya no necesario con Hilt)
        instance = this
    }

    companion object {
        lateinit var instance: AiChefApplication
            private set
    }
}
