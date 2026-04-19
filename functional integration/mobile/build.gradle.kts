// =============================================================================
// AI CHEF - MÓDULO 5: FIREBASE AI LOGIC
// Build Configuration (Project Level)
// =============================================================================
// Este archivo configura los plugins disponibles para todo el proyecto.
// Google Services Plugin es ESENCIAL para procesar google-services.json
// =============================================================================

plugins {
    // Plugin de Android Application - NO aplicar aquí
    alias(libs.plugins.android.application) apply false

    // Plugin de Kotlin para Android
    alias(libs.plugins.kotlin.android) apply false

    // Plugin de Compose Compiler
    alias(libs.plugins.kotlin.compose) apply false

    // ==========================================================================
    // GOOGLE SERVICES PLUGIN
    // ==========================================================================
    // Este plugin procesa el archivo google-services.json y genera
    // la configuración necesaria para conectar con Firebase.
    //
    // IMPORTANTE: Sin este plugin, Firebase NO funcionará correctamente.
    // El archivo google-services.json debe descargarse de Firebase Console.
    // ==========================================================================
    alias(libs.plugins.google.services) apply false

    // ==========================================================================
    // HILT PLUGIN - INYECCIÓN DE DEPENDENCIAS
    // ==========================================================================
    // Hilt es el framework de DI recomendado por Google para Android.
    //
    // COMPARACIÓN HILT VS KOIN:
    // ┌─────────────────────────┬─────────────────────────┐
    // │          HILT           │          KOIN           │
    // ├─────────────────────────┼─────────────────────────┤
    // │ Anotaciones (@Inject)   │ DSL de Kotlin           │
    // │ Validación compile-time │ Validación runtime      │
    // │ Generación de código    │ Sin generación          │
    // │ Curva de aprendizaje ↑  │ Más fácil de aprender   │
    // │ Mejor para apps grandes │ Ideal para apps medianas│
    // │ Oficial de Google       │ Comunidad               │
    // └─────────────────────────┴─────────────────────────┘
    //
    // En este módulo usamos Hilt para demostrar un enfoque diferente
    // al Koin usado en los módulos anteriores.
    //
    // NOTA: Hilt no se cubrió en clase, se incluye como referencia.
    // ==========================================================================
    alias(libs.plugins.hilt.android) apply false

    // Kotlin Serialization para parsing de JSON
    alias(libs.plugins.kotlin.serialization) apply false

    // Plugin de KSP (Kotlin Symbol Processing)
    alias(libs.plugins.ksp) apply false
}
