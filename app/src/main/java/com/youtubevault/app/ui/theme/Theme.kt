package com.youtubevault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VaultRed = Color(0xFFFF0033)
val VaultPurple = Color(0xFF9333EA)

private val DarkColors = darkColorScheme(
    primary = VaultRed,
    secondary = VaultPurple,
    background = Color(0xFF0F0F12),
    surface = Color(0xFF18181D),
    surfaceVariant = Color(0xFF23232B),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFFA1A1AA)
)

private val LightColors = lightColorScheme(
    primary = VaultRed,
    secondary = VaultPurple,
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun YouTubeVaultTheme(
    darkTheme: Boolean = true,
    privateMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val accent = if (privateMode) VaultPurple else VaultRed
    val scheme = if (darkTheme) DarkColors.copy(primary = accent) else LightColors.copy(primary = accent)
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
