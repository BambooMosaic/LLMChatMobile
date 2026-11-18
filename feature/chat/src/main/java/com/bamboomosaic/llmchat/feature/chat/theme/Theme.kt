package com.bamboomosaic.llmchat.feature.chat.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 深色：背景深灰，其余 UI（顶栏/输入栏/侧边栏/气泡）用中灰分层。
 * 不用系统默认的带紫暗色。
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B8CFF),
    onPrimary = Color(0xFF1A1440),
    primaryContainer = Color(0xFF3A3560),
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = Color(0xFFA9AEFF),
    onSecondary = Color(0xFF1A1440),

    // 页面底色：深灰（不是纯黑，也不是白）
    background = Color(0xFF1C1C1C),
    onBackground = Color(0xFFEDEDED),

    // 顶栏 / 侧边栏 / 卡片：中灰
    surface = Color(0xFF272727),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFCACACA),

    // Material3 1.3 的容器分级，统一收敛到中灰一档，避免出现系统默认暗紫
    surfaceContainerLowest = Color(0xFF141414),
    surfaceContainerLow = Color(0xFF212121),
    surfaceContainer = Color(0xFF272727),
    surfaceContainerHigh = Color(0xFF2E2E2E),
    surfaceContainerHighest = Color(0xFF383838),

    outline = Color(0xFF616161),
    // 分界线：比中灰再亮一档，清晰可见
    outlineVariant = Color(0xFF454545),

    errorContainer = Color(0xFF5C1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** 浅色：纯白底，其余浅灰分层。 */
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
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF48454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE9E9E9),
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
