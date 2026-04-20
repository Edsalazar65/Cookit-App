package com.kos.android.proyecto.cookit.di

import com.kos.android.proyecto.cookit.data.firebase.AuthRepository
import com.kos.android.proyecto.cookit.data.firebase.FirestoreRepository
import com.kos.android.proyecto.cookit.data.firebase.IAuthRepository
import com.kos.android.proyecto.cookit.data.firebase.IFirestoreRepository
import com.kos.android.proyecto.cookit.data.firebase.IStorageRepository
import com.kos.android.proyecto.cookit.data.firebase.StorageRepository
import com.kos.android.proyecto.cookit.data.remote.AiLogicDataSource
import com.kos.android.proyecto.cookit.data.remote.IAiLogicDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.kos.android.proyecto.cookit.data.firebase.IUserRepository
import com.kos.android.proyecto.cookit.data.firebase.UserRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepository
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepository
    ): IAuthRepository


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
