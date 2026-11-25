package com.bamboomosaic.llmchat.feature.chat

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
 *   叠加系统级整窗背后实时模糊（blurBehindRadius），即"高配毛玻璃"；不支持的设备就是纯半透明。
 *   29 轮修正：必须显式加 FLAG_BLUR_BEHIND（只设 blurBehindRadius 是无效的）；
 *   半径取 30（90 太糊）；滑块可用性按能力判断（详见 [blurSupported]）。
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
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attrs.blurBehindRadius = 30
            }
            window.attributes = attrs
        }
        onDispose {
            if (window != null) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
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
 * Android 12+ 且系统允许时叠加系统级背后实时模糊（"高配毛玻璃"，不支持的设备就是纯半透明）；
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

/**
 * 25 轮：毛玻璃档位可用性（29 轮修正）——改按 **能力**（API 31+）判断。
 * 此前用 isCrossWindowBlurEnabled（当前是否启用）：系统动画关闭 / 省电模式 / 部分厂商策略
 * 都会令其返回 false，导致大量真机滑块被锁死滑不到毛玻璃。
 * 新逻辑：Android 12+ 一律允许选择毛玻璃档；系统不启用跨窗模糊时（动画关/省电/ROM 禁用）
 * FLAG_BLUR_BEHIND 自动无效，实际退化为半透明——不锁死、只降级。
 */
@Composable
fun blurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * 29 轮反馈：小菜单（DropdownMenu / Popup）的毛玻璃——Popup 也是独立 window。
 * 关键：LocalView 在 popup 内仍是主窗口的 view，拿不到 popup window；
 * 所以借 AndroidView 在 popup window 里"种"一个 view，attach 后从它的 root 取
 * WindowManager.LayoutParams 打 FLAG_BLUR_BEHIND（拿不到时静默跳过）。
 * 用法：放在 DropdownMenu 的 content 开头，配套菜单容器色用 [glassSurfaceColor]。
 */
@Composable
fun GlassPopupEffect(enabled: Boolean = LocalGlassLevel.current >= 1) {
    val level = LocalGlassLevel.current
    if (level < 1) return
    if (level < 2 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.view.View(ctx).also { probe ->
                probe.post {
                    runCatching {
                        val root = probe.rootView
                        val lp = root.layoutParams
                            as? android.view.WindowManager.LayoutParams
                            ?: return@runCatching
                        val isAttachedPopup =
                            lp.type == android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG ||
                                lp.type == android.view.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                        if (!isAttachedPopup) return@runCatching
                        val wm = ctx.getSystemService(android.content.Context.WINDOW_SERVICE)
                            as android.view.WindowManager
                        lp.flags = lp.flags or android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                        lp.blurBehindRadius = 30
                        wm.updateViewLayout(root, lp)
                    }
                }
            }
        },
        modifier = Modifier.size(0.dp),
    )
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
