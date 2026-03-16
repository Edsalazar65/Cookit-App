package com.curso.android.module5.aichef.di

import com.curso.android.module5.aichef.data.firebase.AuthRepository
import com.curso.android.module5.aichef.data.firebase.FirestoreRepository
import com.curso.android.module5.aichef.data.firebase.IAuthRepository
import com.curso.android.module5.aichef.data.firebase.IFirestoreRepository
import com.curso.android.module5.aichef.data.firebase.IStorageRepository
import com.curso.android.module5.aichef.data.firebase.StorageRepository
import com.curso.android.module5.aichef.data.remote.AiLogicDataSource
import com.curso.android.module5.aichef.data.remote.IAiLogicDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * =============================================================================
 * APP MODULE - Configuración de Hilt para Inyección de Dependencias
 * =============================================================================
 *
 * CONCEPTO: Hilt Modules
 * Los módulos de Hilt definen cómo se crean las dependencias.
 * A diferencia de Koin (DSL de Kotlin), Hilt usa anotaciones:
 * - @Module: Marca la clase como un módulo de DI
 * - @InstallIn: Define el scope (SingletonComponent = toda la app)
 * - @Binds: Conecta una interface con su implementación
 * - @Provides: Crea instancias manualmente (para clases de terceros)
 *
 * COMPARACIÓN CON KOIN:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                           KOIN                                       │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ val appModule = module {                                            │
 * │     single<IAuthRepository> { AuthRepository() }                    │
 * │     single<IFirestoreRepository> { FirestoreRepository() }         │
 * │     viewModel { ChefViewModel(get(), get(), get(), get()) }        │
 * │ }                                                                   │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │                           HILT                                       │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ @Module @InstallIn(SingletonComponent::class)                       │
 * │ abstract class AppModule {                                          │
 * │     @Binds @Singleton                                               │
 * │     abstract fun bindAuthRepository(impl: AuthRepository):          │
 * │         IAuthRepository                                             │
 * │ }                                                                   │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * NOTA: Hilt no se cubrió en clase, se incluye como referencia avanzada.
 *
 * =============================================================================
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    /**
     * CONCEPTO: @Binds
     * Conecta una interface con su implementación concreta.
     * Hilt automáticamente inyecta AuthRepository cuando se solicita IAuthRepository.
     *
     * Requisitos para @Binds:
     * - El método debe ser abstract
     * - Debe tener un solo parámetro (la implementación)
     * - El return type es la interface
     * - La implementación debe tener @Inject constructor
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepository
    ): IAuthRepository

    /**
     * Binding para Firestore Repository
     * Permite testear con un mock de IFirestoreRepository
     */
    @Binds
    @Singleton
    abstract fun bindFirestoreRepository(
        impl: FirestoreRepository
    ): IFirestoreRepository

    /**
     * Binding para Storage Repository
     */
    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        impl: StorageRepository
    ): IStorageRepository

    /**
     * Binding para AI Logic DataSource
     * Permite testear sin llamar a la API real de Gemini
     */
    @Binds
    @Singleton
    abstract fun bindAiLogicDataSource(
        impl: AiLogicDataSource
    ): IAiLogicDataSource
}

/**
 * =============================================================================
 * NOTAS ADICIONALES SOBRE HILT
 * =============================================================================
 *
 * 1. SCOPES DE HILT:
 *    - SingletonComponent: Una instancia para toda la app
 *    - ActivityComponent: Una instancia por Activity
 *    - ViewModelComponent: Una instancia por ViewModel
 *    - FragmentComponent: Una instancia por Fragment
 *
 * 2. @Provides vs @Binds:
 *    - @Binds: Para interfaces implementadas por nuestras clases
 *    - @Provides: Para clases de terceros (Retrofit, OkHttp, etc.)
 *
 *    Ejemplo de @Provides:
 *    ```kotlin
 *    @Module
 *    @InstallIn(SingletonComponent::class)
 *    object NetworkModule {
 *        @Provides
 *        @Singleton
 *        fun provideOkHttpClient(): OkHttpClient {
 *            return OkHttpClient.Builder()
 *                .addInterceptor(HttpLoggingInterceptor())
 *                .build()
 *        }
 *    }
 *    ```
 *
 * 3. VIEWMODEL CON HILT:
 *    ```kotlin
 *    @HiltViewModel
 *    class MyViewModel @Inject constructor(
 *        private val repository: IMyRepository
 *    ) : ViewModel()
 *    ```
 *
 * 4. TESTING CON HILT:
 *    ```kotlin
 *    @HiltAndroidTest
 *    class MyTest {
 *        @BindValue
 *        val fakeRepository: IMyRepository = FakeRepository()
 *    }
 *    ```
 *
 * =============================================================================
 */
