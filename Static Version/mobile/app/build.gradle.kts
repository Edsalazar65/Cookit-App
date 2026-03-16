// =============================================================================
// AI CHEF - MÓDULO 5: FIREBASE AI LOGIC
// Build Configuration (App Module)
// =============================================================================
// Este archivo configura el módulo principal incluyendo todas las
// dependencias de Firebase necesarias para Auth, Firestore y AI Logic.
// =============================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // ==========================================================================
    // GOOGLE SERVICES PLUGIN
    // ==========================================================================
    // OBLIGATORIO para Firebase. Este plugin:
    // 1. Lee google-services.json del directorio app/
    // 2. Genera valores de configuración (API keys, project ID, etc.)
    // 3. Los inyecta en los recursos de la app
    //
    // Sin este plugin aplicado, Firebase.auth, Firebase.firestore, y
    // Firebase.ai NO funcionarán correctamente.
    // ==========================================================================
    alias(libs.plugins.google.services)

    // ==========================================================================
    // HILT - INYECCIÓN DE DEPENDENCIAS
    // ==========================================================================
    // Hilt usa generación de código para crear el grafo de dependencias.
    // El plugin procesa las anotaciones (@Inject, @Module, @Binds) y
    // genera las clases necesarias en compile-time.
    //
    // NOTA: Hilt no se cubrió en clase, se incluye como referencia avanzada.
    // ==========================================================================
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp) // Reemplaza a KAPT

    // Kotlin Serialization para JSON parsing
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.curso.android.module5.aichef"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.curso.android.module5.aichef"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // =========================================================================
    // CORE ANDROID
    // =========================================================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // =========================================================================
    // JETPACK COMPOSE
    // =========================================================================
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    implementation(libs.androidx.navigation.compose)

    // =========================================================================
    // LIFECYCLE (incluye collectAsStateWithLifecycle)
    // =========================================================================
    implementation(libs.bundles.lifecycle)

    // =========================================================================
    // FIREBASE
    // =========================================================================
    // BoM (Bill of Materials) - SIEMPRE usar platform() para el BoM
    // Esto asegura que todas las librerías de Firebase sean compatibles
    implementation(platform(libs.firebase.bom))

    // Firebase Auth - Para login/registro de usuarios
    implementation(libs.firebase.auth)

    // Cloud Firestore - Base de datos NoSQL
    implementation(libs.firebase.firestore)

    // Firebase Storage - Almacenamiento de archivos (cache de imágenes generadas)
    implementation(libs.firebase.storage)

    // ==========================================================================
    // FIREBASE AI LOGIC - EL NUEVO SDK UNIFICADO (2025)
    // ==========================================================================
    // Este es el SDK CORRECTO para acceder a modelos Gemini desde apps móviles.
    //
    // HISTORIA:
    // - 2024: Se lanzó "Vertex AI in Firebase" (firebase-vertexai)
    // - 2025: Renombrado a "Firebase AI Logic" (firebase-ai)
    //
    // VENTAJAS DE FIREBASE AI vs SDK CLIENTE DIRECTO:
    // 1. Seguridad: No necesitas API Keys en el código
    // 2. App Check: Protección contra uso no autorizado
    // 3. Integración: Funciona con Firebase Auth y reglas de seguridad
    // 4. Billing: Se maneja a través de Firebase/GCP
    //
    // NO USAR:
    // - com.google.firebase:firebase-vertexai (legacy)
    // - com.google.ai.client.generativeai (cliente directo, deprecado)
    // ==========================================================================
    implementation(libs.firebase.ai)

    // Firebase App Check - Debug provider para desarrollo
    debugImplementation(libs.firebase.appcheck.debug)

    // =========================================================================
    // COIL - Carga de imágenes
    // =========================================================================
    implementation(libs.coil.compose)

    // =========================================================================
    // COROUTINES
    // =========================================================================
    implementation(libs.kotlinx.coroutines.android)

    // =========================================================================
    // HILT - Inyección de Dependencias
    // =========================================================================
    // CONCEPTO: Hilt usa anotaciones para configurar DI:
    // - @HiltAndroidApp: En la clase Application
    // - @AndroidEntryPoint: En Activities/Fragments
    // - @HiltViewModel: En ViewModels
    // - @Inject constructor: Para recibir dependencias
    // - @Module + @Binds: Para proveer interfaces
    //
    // El compilador (kapt) genera el código necesario en compile-time.
    // =========================================================================
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // =========================================================================
    // SERIALIZATION - Para JSON parsing
    // =========================================================================
    implementation(libs.kotlinx.serialization.json)

    // =========================================================================
    // TESTING
    // =========================================================================
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

// Configuración de KAPT para Hilt (ELIMINADO por MIGRACIÓN A KSP)
// kapt {
//    correctErrorTypes = true
// }
