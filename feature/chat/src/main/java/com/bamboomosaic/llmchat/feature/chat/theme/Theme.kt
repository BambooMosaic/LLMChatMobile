package com.bamboomosaic.llmchat.feature.chat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7CF8),
    onPrimary = Color(0xFF1A1440),
    primaryContainer = Color(0xFF3A3168),
    onPrimaryContainer = Color(0xFFDDD7FF),
    secondary = Color(0xFF9AA0FF),
    background = Color(0xFF131315),
    onBackground = Color(0xFFECECEF),
    surface = Color(0xFF1B1B1E),
    onSurface = Color(0xFFECECEF),
    surfaceVariant = Color(0xFF2A2A2F),
    onSurfaceVariant = Color(0xFFC7C7CE),
    outline = Color(0xFF4A4A52),
    outlineVariant = Color(0xFF333338),
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF1F1A4D),
    secondary = Color(0xFF5B5F9E),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE7E1EC),
    onSurfaceVariant = Color(0xFF48454E),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

/** 应用主题：深色/浅色即时切换。 */
@Composable
fun LLMChatTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
