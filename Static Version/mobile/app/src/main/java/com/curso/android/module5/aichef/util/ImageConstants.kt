package com.curso.android.module5.aichef.util

/**
 * =============================================================================
 * IMAGE CONSTANTS - Constantes para procesamiento de imágenes
 * =============================================================================
 *
 * CONCEPTO: Constantes vs Valores Hardcodeados
 * Extraer valores numéricos a constantes ofrece varios beneficios:
 *
 * 1. MANTENIBILIDAD:
 *    Cambiar un valor en un solo lugar actualiza toda la app.
 *    Si Gemini cambia sus límites, solo modificamos aquí.
 *
 * 2. DOCUMENTACIÓN:
 *    El nombre de la constante explica el propósito del valor.
 *    `1024` no dice nada, `MAX_IMAGE_DIMENSION` sí.
 *
 * 3. REUTILIZACIÓN:
 *    Múltiples pantallas pueden usar las mismas constantes.
 *
 * 4. TESTABILIDAD:
 *    Los tests pueden verificar que se respetan los límites.
 *
 * LÍMITES DE GEMINI PARA IMÁGENES:
 * - Máximo 20MB por imagen (después de encoding)
 * - Recomendado: imágenes < 4MB para mejor performance
 * - Dimensión recomendada: 1024x1024 o menor
 * - Formatos soportados: JPEG, PNG, WEBP, GIF, HEIC
 *
 * =============================================================================
 */
object ImageConstants {

    /**
     * Dimensión máxima (ancho o alto) para imágenes enviadas a Gemini.
     *
     * ¿POR QUÉ 1024?
     * - Gemini puede procesar imágenes más grandes, pero:
     *   - Mayor tamaño = Mayor tiempo de procesamiento
     *   - Mayor tamaño = Mayor consumo de datos
     *   - 1024px es suficiente para identificar ingredientes
     *
     * CÁLCULO DE IMPACTO:
     * - Foto de 4000x3000 = 12 megapíxeles
     * - Reducida a 1024x768 = 0.78 megapíxeles
     * - Reducción de ~15x en datos a procesar
     *
     * NOTA: Si se requiere más detalle (ej: leer etiquetas pequeñas),
     * considerar aumentar a 2048.
     */
    const val MAX_IMAGE_DIMENSION = 1024

    /**
     * Calidad de compresión JPEG (0-100).
     *
     * ¿POR QUÉ 85?
     * - 85-90: Balance óptimo entre calidad y tamaño
     * - < 80: Artefactos visibles en algunas imágenes
     * - > 90: Aumento de tamaño sin mejora perceptible
     *
     * IMPACTO EN TAMAÑO:
     * - JPEG quality 100: ~3MB para foto típica de 1024px
     * - JPEG quality 85: ~500KB para la misma foto
     * - JPEG quality 70: ~300KB (con artefactos notables)
     */
    const val JPEG_QUALITY = 85

    /**
     * Tamaño máximo de archivo en bytes antes de compresión adicional.
     *
     * Gemini tiene un límite de 20MB por imagen, pero usamos 4MB
     * como límite práctico para:
     * - Reducir tiempo de upload
     * - Evitar timeouts en conexiones lentas
     * - Mejorar tiempo de respuesta de la API
     */
    const val MAX_FILE_SIZE_BYTES = 4 * 1024 * 1024 // 4MB

    /**
     * Formato de imagen preferido para uploads.
     *
     * JPEG vs PNG:
     * - JPEG: Mejor compresión para fotos (con pérdida)
     * - PNG: Mejor para gráficos/texto (sin pérdida)
     *
     * Para fotos de ingredientes, JPEG es la mejor opción.
     */
    const val PREFERRED_FORMAT = "image/jpeg"
}

/**
 * =============================================================================
 * NOTAS ADICIONALES SOBRE PROCESAMIENTO DE IMÁGENES
 * =============================================================================
 *
 * 1. BITMAP EN MEMORIA:
 *    Un Bitmap de 4000x3000 con ARGB_8888 ocupa:
 *    4000 * 3000 * 4 bytes = 48MB de RAM
 *
 *    Después de redimensionar a 1024x768:
 *    1024 * 768 * 4 bytes = 3.1MB de RAM
 *
 *    IMPORTANTE: Siempre reciclar Bitmaps cuando no se usen.
 *
 * 2. CONFIGURACIONES DE BITMAP:
 *    - ARGB_8888: 32 bits/pixel (default, mejor calidad)
 *    - RGB_565: 16 bits/pixel (menos RAM, sin alpha)
 *    - ALPHA_8: 8 bits/pixel (solo alpha)
 *
 * 3. EXIF ORIENTATION:
 *    Las fotos de cámara pueden tener rotación en metadata EXIF.
 *    ImageDecoder (API 28+) lo maneja automáticamente.
 *    Para APIs anteriores, usar ExifInterface manualmente.
 *
 * 4. ALTERNATIVA - USAR URI DIRECTAMENTE:
 *    En lugar de cargar a Bitmap, algunos casos permiten:
 *    ```kotlin
 *    val inputStream = contentResolver.openInputStream(uri)
 *    // Enviar stream directamente a la API
 *    ```
 *    Esto evita cargar la imagen completa en memoria.
 *
 * =============================================================================
 */
