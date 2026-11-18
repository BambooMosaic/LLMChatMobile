package com.bamboomosaic.llmchat.feature.chat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 深色：黑灰底色，不用带紫的暗色。 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B8CFF),
    onPrimary = Color(0xFF1A1440),
    primaryContainer = Color(0xFF2F2A5C),
    onPrimaryContainer = Color(0xFFE2DDFF),
    secondary = Color(0xFFA9AEFF),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFC9C9C9),
    outline = Color(0xFF4A4A4A),
    // 分界线：比背景明显一档
    outlineVariant = Color(0xFF3A3A3A),
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** 浅色：纯白底。 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF),
    onPrimaryContainer = Color(0xFF1F1A4D),
    secondary = Color(0xFF5B5F9E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF48454E),
    outline = Color(0xFF9E9E9E),
    // 分界线：浅灰，纯白背景上可见
    outlineVariant = Color(0xFFE3E3E3),
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
