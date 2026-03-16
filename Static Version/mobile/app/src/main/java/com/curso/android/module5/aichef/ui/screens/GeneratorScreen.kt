package com.curso.android.module5.aichef.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.curso.android.module5.aichef.domain.model.UiState
import com.curso.android.module5.aichef.ui.viewmodel.ChefViewModel
import com.curso.android.module5.aichef.util.ImageConstants

/**
 * =============================================================================
 * GeneratorScreen - Pantalla de generación de recetas con IA
 * =============================================================================
 *
 * CONCEPTO: Firebase AI Logic para contenido multimodal
 * Esta pantalla demuestra cómo usar Firebase AI Logic (Gemini) para:
 * 1. Procesar imágenes (ingredientes)
 * 2. Generar texto estructurado (recetas)
 *
 * FLUJO:
 * 1. Usuario selecciona imagen de galería (PickVisualMedia)
 * 2. Convertimos URI a Bitmap
 * 3. Enviamos Bitmap a Firebase AI Logic
 * 4. Gemini analiza y genera receta
 * 5. Guardamos en Firestore y regresamos al Home
 *
 * CONCEPTO: PickVisualMedia
 * Es la nueva API recomendada para seleccionar media (Android 11+):
 * - No requiere permisos de almacenamiento
 * - UI consistente del sistema
 * - Más seguro que ACTION_PICK
 *
 * =============================================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: ChefViewModel,
    onNavigateBack: () -> Unit,
    onRecipeGenerated: () -> Unit
) {
    val context = LocalContext.current

    // Estado de la imagen seleccionada
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Observar estado de generación
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()

    // Snackbar para errores
    val snackbarHostState = remember { SnackbarHostState() }

    /**
     * CONCEPTO: Photo Picker (PickVisualMedia)
     *
     * rememberLauncherForActivityResult crea un launcher para
     * Activity Results API de forma compatible con Compose.
     *
     * PickVisualMedia es el contrato recomendado para seleccionar
     * imágenes/videos desde Android 11 (API 30).
     *
     * Ventajas sobre ACTION_PICK tradicional:
     * - No necesita READ_EXTERNAL_STORAGE en Android 13+
     * - UI consistente proporcionada por el sistema
     * - Mejor privacidad para el usuario
     */
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Convertir URI a Bitmap
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }

                // Redimensionar si es muy grande (Gemini tiene límites)
                // Ver ImageConstants para documentación de límites
                selectedBitmap = resizeBitmapIfNeeded(bitmap, maxDimension = ImageConstants.MAX_IMAGE_DIMENSION)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Manejar estado de generación
    LaunchedEffect(generationState) {
        when (val state = generationState) {
            is UiState.Success -> {
                viewModel.clearGenerationState()
                onRecipeGenerated()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearGenerationState()
            }
            else -> {}
        }
    }

    val isLoading = generationState is UiState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Generar Receta") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Área de imagen
            ImagePreviewBox(
                bitmap = selectedBitmap,
                isLoading = isLoading,
                onClick = {
                    if (!isLoading) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                }
            )

            // Instrucciones
            Text(
                text = if (selectedBitmap == null) {
                    "Selecciona una foto de tus ingredientes y la IA generará una receta deliciosa"
                } else {
                    "¡Imagen lista! Presiona el botón para generar tu receta"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            // Botón de seleccionar imagen
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (selectedBitmap == null) "Seleccionar Imagen" else "Cambiar Imagen")
            }

            // Botón de generar
            Button(
                onClick = {
                    selectedBitmap?.let { bitmap ->
                        viewModel.generateRecipe(bitmap)
                    }
                },
                enabled = selectedBitmap != null && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Generando...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Generar Receta con IA")
                }
            }
        }
    }
}

/**
 * Box para mostrar la imagen seleccionada o placeholder
 */
@Composable
private fun ImagePreviewBox(
    bitmap: Bitmap?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = if (bitmap != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                // Estado de carga
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analizando imagen con IA...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            bitmap != null -> {
                // Mostrar imagen seleccionada
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                // Placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca para seleccionar una foto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Redimensiona un Bitmap si excede el tamaño máximo
 *
 * IMPORTANTE: Gemini tiene límites en el tamaño de imágenes.
 * Redimensionar previene errores y reduce tiempo de procesamiento.
 */
private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxDimension && height <= maxDimension) {
        return bitmap
    }

    val ratio = minOf(
        maxDimension.toFloat() / width,
        maxDimension.toFloat() / height
    )

    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
