# =============================================================================
# ProGuard Rules for AI Chef
# =============================================================================

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firebase AI Logic
-keep class com.google.firebase.ai.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data classes for Firestore serialization
-keep class com.curso.android.module5.aichef.domain.model.** { *; }
