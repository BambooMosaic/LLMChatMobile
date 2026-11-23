package com.bamboomosaic.llmchat.feature.chat

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 24 轮：实验性毛玻璃 / 半透明磨砂材质（半透明 + 高配毛玻璃）。
 * - [GlassDialogShell]：毛玻璃开启时弹窗为半透明表面 + 细边框；Android 12+ 且系统允许时
 *   叠加系统级整窗背后模糊（blurBehindRadius），即"高配毛玻璃"；不支持的设备就是纯半透明。
 * - 深色主题表面近乎不透明，避免模糊后的亮色内容压暗文字对比（纯黑预设尤其明显）。
 * 性能与兼容性自负：低端机开实时模糊可能掉帧，故做成默认关闭的实验性开关。
 */
val LocalExperimentalGlass = staticCompositionLocalOf { false }

/** 24 轮：对当前 Compose Dialog 的窗口启用系统级背后模糊（Android 12+ 生效，其他设备跳过）。 */
@Composable
fun GlassDialogEffect(enabled: Boolean = LocalExperimentalGlass.current) {
    if (!enabled) return
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val canBlur = window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val originalDim = window?.attributes?.dimAmount ?: 0f
        if (window != null && canBlur) {
            window.setBackgroundDrawableResource(android.R.color.transparent)
            // 半透明内容窗口要用 LayoutParams.blurBehindRadius（整窗背后模糊），
            // setBackgroundBlurRadius 只模糊"背景 drawable 区域"，透明背景下等于没模糊
            val attrs = window.attributes
            attrs.blurBehindRadius = 90
            attrs.dimAmount = 0.05f
            window.attributes = attrs
        }
        onDispose {
            if (window != null && canBlur) {
                val attrs = window.attributes
                attrs.blurBehindRadius = 0
                attrs.dimAmount = originalDim
                window.attributes = attrs
            }
        }
    }
}

/**
 * 24 轮：弹窗玻璃外壳——毛玻璃开启时：半透明表面 + 细边框，
 * Android 12+ 且系统允许时叠加系统级背后模糊（"高配毛玻璃"，不支持的设备就是纯半透明）；
 * 关闭时与原版 Surface 完全一致（不加边框）。
 */
@Composable
fun GlassDialogShell(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    val glass = LocalExperimentalGlass.current
    if (!glass) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { content() }
        return
    }
    GlassDialogEffect()
    Surface(
        modifier = modifier
            .clip(shape)
            .border(width = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape),
        shape = shape,
        // 24 轮：半透明 + 染色；系统支持时 blurBehindRadius 在背后叠加实时模糊（高配）
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { content() }
}

/** 24 轮：弹窗表面色——毛玻璃开启时降透明度（深色主题近乎不透明，保证文字可读）。 */
@Composable
fun glassSurfaceColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    if (!LocalExperimentalGlass.current) return surface
    val dark = surface.luminance() < 0.5f
    return surface.copy(alpha = if (dark) 0.94f else 0.86f)
}
