package com.bamboomosaic.llmchat.feature.chat

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
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
/** 25 轮：玻璃材质档位（0=关 1=半透明 2=毛玻璃），全局由外观设置提供。 */
val LocalGlassLevel = staticCompositionLocalOf { 0 }

/** 24 轮：对当前 Compose Dialog 的窗口启用系统级背后模糊（Android 12+ 生效，其他设备跳过）。 */
@Composable
fun GlassDialogEffect(enabled: Boolean = LocalGlassLevel.current >= 1) {
    val level = LocalGlassLevel.current
    if (level < 1) return
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            // 玻璃模式统一：窗口背景透明 + 去掉背后的黑色遮罩（用户感知的"阴影"）
            window.setBackgroundDrawableResource(android.R.color.transparent)
            val attrs = window.attributes
            attrs.dimAmount = 0f
            // 毛玻璃档（2）且系统支持：叠加整窗背后实时模糊
            if (level >= 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                attrs.blurBehindRadius = 90
            }
            window.attributes = attrs
        }
        onDispose {
            if (window != null) {
                val attrs = window.attributes
                attrs.blurBehindRadius = 0
                attrs.dimAmount = 0.32f
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
    val glass = LocalGlassLevel.current >= 1
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
    if (LocalGlassLevel.current < 1) return surface
    // 半透明档：两主题统一 0.78，效果肉眼可见；毛玻璃档背后有系统模糊，同样 0.78
    return surface.copy(alpha = 0.78f)
}

/** 25 轮：设备是否支持系统级跨窗口模糊（毛玻璃档位可用性）。 */
@Composable
fun blurSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return runCatching {
        (ctx.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager)
            .isCrossWindowBlurEnabled
    }.getOrDefault(false)
}

/**
 * 27 轮：平板/宽屏弹窗限宽——宽屏时限宽居中（保持正常比例不拉伸），窄屏原样返回。
 * 用法：`Modifier.fillMaxWidth(0.94f).wideDialogMax(440.dp)`（窄屏按比例铺满，宽屏被 max 截断居中）。
 */
@Composable
fun Modifier.wideDialogMax(maxWidth: androidx.compose.ui.unit.Dp): Modifier {
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    return if (wide) this.then(Modifier.widthIn(max = maxWidth)) else this
}
