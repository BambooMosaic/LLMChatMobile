package com.bamboomosaic.llmchat.feature.chat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 21 轮：UI 配色预设——深浅两套分组，预设避开主紫色系。
 * default = 原版配色；ocean 深海蓝 / forest 森绿 / ember 琥珀 为 21 轮新增（深浅同名字两组）。
 */
private data class Palette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val outlineVariant: Color,
)

private val darkDefault = Palette(
    primary = Color(0xFF9B8CFF), onPrimary = Color(0xFF1A1440),
    primaryContainer = Color(0xFF3A3560), onPrimaryContainer = Color(0xFFE6E2FF),
    background = Color(0xFF0A0A0A), surface = Color(0xFF272727),
    surfaceVariant = Color(0xFF333333), onSurfaceVariant = Color(0xFFCACACA),
    surfaceContainerLow = Color(0xFF212121), surfaceContainer = Color(0xFF272727),
    surfaceContainerHigh = Color(0xFF2E2E2E), outlineVariant = Color(0xFF454545),
)

private val darkOcean = Palette(
    primary = Color(0xFF7FB2FF), onPrimary = Color(0xFF0A1526),
    primaryContainer = Color(0xFF1B2C4F), onPrimaryContainer = Color(0xFFD6E6FF),
    background = Color(0xFF0B1220), surface = Color(0xFF16213A),
    surfaceVariant = Color(0xFF1E2C47), onSurfaceVariant = Color(0xFFB9C4D8),
    surfaceContainerLow = Color(0xFF121A2E), surfaceContainer = Color(0xFF16213A),
    surfaceContainerHigh = Color(0xFF1B2740), outlineVariant = Color(0xFF2A3A5C),
)

private val darkForest = Palette(
    primary = Color(0xFF8FD6A8), onPrimary = Color(0xFF0A1F12),
    primaryContainer = Color(0xFF1B3A28), onPrimaryContainer = Color(0xFFD6F0E0),
    background = Color(0xFF0C1510), surface = Color(0xFF16241C),
    surfaceVariant = Color(0xFF1F3328), onSurfaceVariant = Color(0xFFB8CCBF),
    surfaceContainerLow = Color(0xFF121D16), surfaceContainer = Color(0xFF16241C),
    surfaceContainerHigh = Color(0xFF1B2D23), outlineVariant = Color(0xFF2A4034),
)

private val darkEmber = Palette(
    primary = Color(0xFFFFB27F), onPrimary = Color(0xFF26140A),
    primaryContainer = Color(0xFF4A2E1B), onPrimaryContainer = Color(0xFFFFE4CC),
    background = Color(0xFF1A120B), surface = Color(0xFF2A2016),
    surfaceVariant = Color(0xFF362A1D), onSurfaceVariant = Color(0xFFD0C2B2),
    surfaceContainerLow = Color(0xFF211810), surfaceContainer = Color(0xFF2A2016),
    surfaceContainerHigh = Color(0xFF34281C), outlineVariant = Color(0xFF4A3A2A),
)

private val lightDefault = Palette(
    primary = Color(0xFF6C5CE7), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E1FF), onPrimaryContainer = Color(0xFF1F1A4D),
    background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2), onSurfaceVariant = Color(0xFF48454E),
    surfaceContainerLow = Color(0xFFFAFAFA), surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF), outlineVariant = Color(0xFFE3E3E3),
)

private val lightCloud = Palette(
    primary = Color(0xFF3A6FA8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFB), onPrimaryContainer = Color(0xFF0F2A47),
    background = Color(0xFFF7FAFF), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF1F8), onSurfaceVariant = Color(0xFF3D4A5C),
    surfaceContainerLow = Color(0xFFFBFDFF), surfaceContainer = Color(0xFFF2F7FC),
    surfaceContainerHigh = Color(0xFFE8F0F9), outlineVariant = Color(0xFFD8E4F0),
)

private val lightMint = Palette(
    primary = Color(0xFF2E8B7F), onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F0EB), onPrimaryContainer = Color(0xFF0E3A33),
    background = Color(0xFFF4FBF9), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6F2EF), onSurfaceVariant = Color(0xFF35524C),
    surfaceContainerLow = Color(0xFFF8FDFB), surfaceContainer = Color(0xFFEFF9F6),
    surfaceContainerHigh = Color(0xFFE4F2EE), outlineVariant = Color(0xFFD0E5E0),
)

private val lightSand = Palette(
    primary = Color(0xFFA87832), onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E9D5), onPrimaryContainer = Color(0xFF3D2C0F),
    background = Color(0xFFFDFAF5), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5EFE6), onSurfaceVariant = Color(0xFF5A4F42),
    surfaceContainerLow = Color(0xFFFEFDFB), surfaceContainer = Color(0xFFFAF6F0),
    surfaceContainerHigh = Color(0xFFF3EDE4), outlineVariant = Color(0xFFE5DDD0),
)

private fun darkColors(p: Palette) = darkColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.primary,
    onSecondary = p.onPrimary,
    background = p.background,
    onBackground = Color(0xFFEDEDED),
    surface = p.surface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    surfaceContainerLowest = p.background,
    surfaceContainerLow = p.surfaceContainerLow,
    surfaceContainer = p.surfaceContainer,
    surfaceContainerHigh = p.surfaceContainerHigh,
    surfaceContainerHighest = p.surfaceContainer,
    outline = p.surfaceContainerHigh,
    outlineVariant = p.outlineVariant,
    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
)

private fun lightColors(p: Palette) = lightColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.primary,
    onSecondary = p.onPrimary,
    background = p.background,
    onBackground = Color(0xFF1B1B1F),
    surface = p.surface,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = p.surfaceContainerLow,
    surfaceContainer = p.surfaceContainer,
    surfaceContainerHigh = p.surfaceContainerHigh,
    surfaceContainerHighest = p.background,
    outline = Color(0xFF9E9E9E),
    outlineVariant = p.outlineVariant,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

/** 应用主题：深色/浅色即时切换，按 [colorPreset] 选择配色预设。 */
@Composable
fun LLMChatTheme(
    darkTheme: Boolean,
    colorPreset: String = "default",
    content: @Composable () -> Unit,
) {
    val dark = mapOf(
        "default" to darkDefault,
        "blue" to darkOcean,
        "green" to darkForest,
        "warm" to darkEmber,
    )
    val light = mapOf(
        "default" to lightDefault,
        "blue" to lightCloud,
        "green" to lightMint,
        "warm" to lightSand,
    )
    val palette = (if (darkTheme) dark else light)[colorPreset]
        ?: if (darkTheme) darkDefault else lightDefault
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(palette) else lightColors(palette),
        content = content,
    )
}
