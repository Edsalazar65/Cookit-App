

plugins {


    alias(libs.plugins.android.application) apply false

    // Plugin de Kotlin para Android
    alias(libs.plugins.kotlin.android) apply false

    // Plugin de Compose Compiler
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.google.services) apply false


    alias(libs.plugins.hilt.android) apply false

    // Kotlin Serialization para parsing de JSON
    alias(libs.plugins.kotlin.serialization) apply false

    // Plugin de KSP (Kotlin Symbol Processing)
    alias(libs.plugins.ksp) apply false
}
