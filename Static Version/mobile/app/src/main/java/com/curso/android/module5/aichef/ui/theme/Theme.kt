package com.curso.android.module5.aichef.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Colores personalizados para AI Chef
private val ChefOrange = Color(0xFFFF6B35)        // Color principal "chef"
private val ChefOrangeVariant = Color(0xFFE55A2B)
private val ChefGreen = Color(0xFF4CAF50)          // Verde para éxito/ingredientes
private val ChefCream = Color(0xFFFFF8E1)          // Fondo cálido

private val LightColorScheme = lightColorScheme(
    primary = ChefOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3D1300),

    secondary = ChefGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF1B5E20),

    background = ChefCream,
    onBackground = Color(0xFF1C1B1F),

    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5DED4),
    onSurfaceVariant = Color(0xFF52443C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF862200),
    onPrimaryContainer = Color(0xFFFFDBCF),

    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFC8E6C9),

    background = Color(0xFF1A1110),
    onBackground = Color(0xFFEDE0DC),

    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8)
)

@Composable
fun AiChefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
