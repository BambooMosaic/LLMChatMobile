package com.bamboomosaic.llmchat.feature.chat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 21 轮：UI 配色预设——深浅两套分组，预设避开主紫色系。
 * default = 原版配色；ocean 深海蓝 / forest 森绿 / ember 琥珀 为 21 轮新增（深浅同名字两组）。
 * 22 轮：改为「浅色预设 x6 / 深色预设 x6」两组 12 套，预设 id 自带深浅前缀（light_ / dark_），
 * 选预设即同时确定明暗模式（浅色/深色模式开关取消）。
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
    primary = Color(0xFFF5C08A), onPrimary = Color(0xFF241A0C),
    primaryContainer = Color(0xFF3E3320), onPrimaryContainer = Color(0xFFFFE9CC),
    background = Color(0xFF121110), surface = Color(0xFF201D18),
    surfaceVariant = Color(0xFF2A2620), onSurfaceVariant = Color(0xFFC9C2B4),
    surfaceContainerLow = Color(0xFF181613), surfaceContainer = Color(0xFF201D18),
    surfaceContainerHigh = Color(0xFF2B2721), outlineVariant = Color(0xFF3B362D),
)

/** 22 轮：纯黑——UI 全黑，按钮白色。 */
private val darkBlack = Palette(
    primary = Color(0xFFF5F5F5), onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFFF5F5F5), onPrimaryContainer = Color(0xFF0A0A0A),
    background = Color(0xFF000000), surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF161616), onSurfaceVariant = Color(0xFFB5B5B5),
    surfaceContainerLow = Color(0xFF0D0D0D), surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818), outlineVariant = Color(0xFF2C2C2C),
)

/** 22 轮：深红。 */
private val darkRed = Palette(
    primary = Color(0xFFFF8A8A), onPrimary = Color(0xFF2A0A0F),
    primaryContainer = Color(0xFF4A1B22), onPrimaryContainer = Color(0xFFFFD9D9),
    background = Color(0xFF140A0C), surface = Color(0xFF1F1013),
    surfaceVariant = Color(0xFF2A1519), onSurfaceVariant = Color(0xFFD0B9BC),
    surfaceContainerLow = Color(0xFF180D10), surfaceContainer = Color(0xFF1F1013),
    surfaceContainerHigh = Color(0xFF271518), outlineVariant = Color(0xFF3E2226),
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

/** 22 轮：浅灰——背景与菜单纯白，仅控件按钮为灰度。 */
private val lightGray = Palette(
    primary = Color(0xFF636363), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8E8), onPrimaryContainer = Color(0xFF2A2A2A),
    background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceContainerLow = Color(0xFFFAFAFA), surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEBEBEB), outlineVariant = Color(0xFFD9D9D9),
)

/** 22 轮：浅粉——菜单 UI 跟随主题色（粉）。 */
private val lightPink = Palette(
    // 23 轮反馈：按钮色只比粉色背景深一档（原 D6496F 偏重）；发送键（primaryContainer）保持不变
    primary = Color(0xFFE68BA7), onPrimary = Color.White,
    // 24 轮反馈：用户气泡（primaryContainer）加深，发送键随同；AI 气泡（surfaceVariant）更浅
    primaryContainer = Color(0xFFF8BCD2), onPrimaryContainer = Color(0xFF4A0F22),
    background = Color(0xFFFFEDF4), surface = Color(0xFFFFDFEB),
    surfaceVariant = Color(0xFFFFE9F2), onSurfaceVariant = Color(0xFF5C3646),
    surfaceContainerLow = Color(0xFFFFF1F7), surfaceContainer = Color(0xFFFCDCE8),
    surfaceContainerHigh = Color(0xFFF7CBDC), outlineVariant = Color(0xFFEDA9C2),
)

private fun darkColors(p: Palette) = darkColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.primary,
    onSecondary = p.onPrimary,
    // 22 轮：secondary/tertiary 容器跟随主色——不定义时用 Material 默认薰衣草灰，
    // 非默认配色下 FilterChip 选中态等会明显跳色
    secondaryContainer = p.primaryContainer,
    onSecondaryContainer = p.onPrimaryContainer,
    tertiary = p.primary,
    onTertiary = p.onPrimary,
    tertiaryContainer = p.primaryContainer,
    onTertiaryContainer = p.onPrimaryContainer,
    background = p.background,
    onBackground = Color(0xFFEDEDED),
    surface = p.surface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    // 22 轮：surfaceTint 固定为中性表面色——默认随 primary，非默认配色下
    // 弹出菜单等带 tonalElevation 的表面会被主色"染色"（阴影/背景偏色的根因）
    surfaceTint = p.surfaceContainer,
    surfaceContainerLowest = p.background,
    surfaceContainerLow = p.surfaceContainerLow,
    surfaceContainer = p.surfaceContainer,
    surfaceContainerHigh = p.surfaceContainerHigh,
    surfaceContainerHighest = p.surfaceContainer,
    surfaceDim = p.background,
    surfaceBright = p.surfaceContainerHigh,
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
    secondaryContainer = p.primaryContainer,
    onSecondaryContainer = p.onPrimaryContainer,
    tertiary = p.primary,
    onTertiary = p.onPrimary,
    tertiaryContainer = p.primaryContainer,
    onTertiaryContainer = p.onPrimaryContainer,
    background = p.background,
    onBackground = Color(0xFF1B1B1F),
    surface = p.surface,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    surfaceTint = p.surfaceContainer,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = p.surfaceContainerLow,
    surfaceContainer = p.surfaceContainer,
    surfaceContainerHigh = p.surfaceContainerHigh,
    surfaceContainerHighest = p.background,
    surfaceDim = p.surfaceContainerLow,
    surfaceBright = p.surfaceContainerHigh,
    outline = Color(0xFF9E9E9E),
    outlineVariant = p.outlineVariant,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

/** 22 轮：设置页色块的预览色（与上方调色板同源，避免 UI 反向 import 私有调色板）。 */
data class PresetOption(val id: String, val swatchBg: Color, val swatchPrimary: Color)

val lightColorPresetOptions = listOf(
    PresetOption("light_default", Color(0xFFF2F2F2), Color(0xFF6C5CE7)),
    PresetOption("light_blue", Color(0xFFDCEAFB), Color(0xFF3A6FA8)),
    PresetOption("light_green", Color(0xFFD8F0EB), Color(0xFF2E8B7F)),
    PresetOption("light_yellow", Color(0xFFF5E9D5), Color(0xFFA87832)),
    PresetOption("light_gray", Color(0xFFFFFFFF), Color(0xFF636363)),
    PresetOption("light_pink", Color(0xFFFFDFEB), Color(0xFFE68BA7)),
)

val darkColorPresetOptions = listOf(
    PresetOption("dark_default", Color(0xFF272727), Color(0xFF9B8CFF)),
    PresetOption("dark_blue", Color(0xFF16213A), Color(0xFF7FB2FF)),
    PresetOption("dark_green", Color(0xFF16241C), Color(0xFF8FD6A8)),
    PresetOption("dark_brown", Color(0xFF201D18), Color(0xFFF5C08A)),
    PresetOption("dark_black", Color(0xFF050505), Color(0xFFF5F5F5)),
    PresetOption("dark_red", Color(0xFF1F1013), Color(0xFFFF8A8A)),
)

/** 应用主题：按 [colorPreset] 的 light_/dark_ 前缀同时决定明暗与配色（22 轮起）。 */
@Composable
fun LLMChatTheme(
    darkTheme: Boolean,
    colorPreset: String = "dark_default",
    content: @Composable () -> Unit,
) {
    val dark = mapOf(
        "dark_default" to darkDefault,
        "dark_blue" to darkOcean,
        "dark_green" to darkForest,
        "dark_brown" to darkEmber,
        "dark_black" to darkBlack,
        "dark_red" to darkRed,
        // 21 轮旧 id 兜底（SettingsStore 已做迁移，历史安装升级后不会再出现）
        "default" to darkDefault,
        "blue" to darkOcean,
        "green" to darkForest,
        "warm" to darkEmber,
    )
    val light = mapOf(
        "light_default" to lightDefault,
        "light_blue" to lightCloud,
        "light_green" to lightMint,
        "light_yellow" to lightSand,
        "light_gray" to lightGray,
        "light_pink" to lightPink,
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
