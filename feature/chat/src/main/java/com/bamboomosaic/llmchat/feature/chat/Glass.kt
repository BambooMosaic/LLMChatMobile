package com.bamboomosaic.llmchat.feature.chat

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

/**
 * 24 轮：实验性毛玻璃 / 半透明磨砂材质（半透明 + 高配毛玻璃）。
 * 30 轮终版：恢复 0.7.3 的 blurBehind 全屏毛玻璃（用户设备上唯一实测可渲染的系统方案；
 * `Window.setBackgroundBlurRadius` 虽语义更准——只糊窗口 bounds 内——但模拟器合成器与
 * 用户三台 ROM（澎湃/ColorOS/原生）均不渲染，全部实测淘汰），并统一推广到所有弹窗：
 * - 窗口外：弹窗窗口背景透明 + 无遮罩（dimAmount=0），卡片本体半透明+细边框；
 * - 毛玻璃档：FLAG_BLUR_BEHIND + [GLASS_BLUR_RADIUS]（10dp，较 0.7.3 的 30px 更轻）；
 * - 系统不启用跨窗模糊时（动画关 / 省电 / ROM 禁用——用户三台设备即此状态）
 *   自动退化为纯半透明；小菜单不参与毛玻璃（保持系统默认菜单样式）。
 * 性能与兼容性自负：低端机开实时模糊可能掉帧，故做成默认关闭的实验性开关。
 */
/** 25 轮：玻璃材质档位（0=关 1=半透明 2=毛玻璃），全局由外观设置提供。 */
val LocalGlassLevel = staticCompositionLocalOf { 0 }

/** 30 轮：全局统一轻度模糊半径（dp）。 */
internal val GLASS_BLUR_RADIUS = 10.dp

/**
 * 24 轮：对当前 Compose Dialog 的窗口做玻璃化打底——窗口背景改为半透明圆角 drawable
 * （卡片底色 + 圆角），去掉背后的黑色遮罩；毛玻璃档（2）叠加 blurBehind 整屏背后实时模糊。
 */
@Composable
fun GlassDialogEffect(enabled: Boolean = LocalGlassLevel.current >= 1, cornerRadius: Dp = 24.dp) {
    val level = LocalGlassLevel.current
    if (level < 1) return
    val view = LocalView.current
    val bgColor = glassSurfaceColor()
    DisposableEffect(enabled, bgColor) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window == null) {
            onDispose { }
        } else {
            val density = view.resources.displayMetrics.density
            // 半透明圆角窗口背景：卡片底色 + 圆角（外壳 Surface 保持透明避免双色调和）
            val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor.toArgb())
                // 显式 setter：apply 块内 cornerRadius = 会被解析成外层的 Dp 参数
                setCornerRadius(cornerRadius.value * density)
            }
            window.setBackgroundDrawable(bgDrawable)
            val attrs = window.attributes
            attrs.dimAmount = 0f
            // 毛玻璃档（2）且系统支持：整屏背后实时模糊（blurBehind，0.7.3 同款、实测可渲染）
            if (level >= 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attrs.blurBehindRadius = (GLASS_BLUR_RADIUS.value * density).roundToInt()
            }
            window.attributes = attrs
            onDispose {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundDrawableResource(android.R.color.transparent)
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
 * Android 12+ 且系统允许时窗口矩形背后实时模糊（"高配毛玻璃"，不支持的设备就是纯半透明）；
 * 关闭时与原版 Surface 完全一致（不加边框）。
 */
@Composable
fun GlassDialogShell(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    /** 卡片圆角，同步给窗口背景 drawable（blur 归属 + 底色圆角与其对齐）。 */
    cornerRadius: Dp = 24.dp,
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
    GlassDialogEffect(cornerRadius = cornerRadius)
    Surface(
        modifier = modifier
            .border(width = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape),
        shape = shape,
        // 30 轮：底色由窗口背景 drawable 提供（blur 归属需要 alpha>0），外壳自身透明避免双色调和
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { content() }
}

/** 24 轮：弹窗表面色——毛玻璃开启时降透明度（深色主题近乎不透明，保证文字可读）。 */
@Composable
fun glassSurfaceColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    if (LocalGlassLevel.current < 1) return surface
    // 半透明档：两主题统一 0.85（0.7.3~0.7.4 为 0.78，浅色主题下偏透看不清，30 轮调实）
    return surface.copy(alpha = 0.85f)
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
 * 29 轮：小菜单（DropdownMenu / Popup）的毛玻璃。
 * 30 轮改判：blurBehindRadius 糊的是**整屏**（见 Glass 类注释），小菜单用它会全屏雾化，
 * 不可取；而菜单拿不到 Window 对象（LocalView 在 popup 内仍是主窗口 view，probe 只能取到
 * WM.LayoutParams），用不了弹窗那套 background blur——故菜单不做系统实时模糊，
 * 统一走「半透明容器 + 细边框 + 无投影」的玻璃感降级（containerColor = glassSurfaceColor）。
 * 函数保留为空操作以兼容既有调用点。
 */
@Composable
fun GlassPopupEffect(enabled: Boolean = LocalGlassLevel.current >= 1) {
    // 30 轮：无系统模糊可用（blurBehind 全屏 / popup 拿不到 Window），保持半透明降级观感
}

/**
 * 30 轮：玻璃版 AlertDialog——布局/字号/颜色对齐 M3 AlertDialog（标题 headlineSmall、
 * 正文 bodyMedium/onSurfaceVariant、按钮右对齐），外壳走 [GlassDialogShell]：
 * 卡片内轻毛玻璃 + 细边框，窗口外完全透明。毛玻璃关闭时外观与普通 AlertDialog 一致。
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GlassDialogShell(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            cornerRadius = 28.dp,
        ) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)) {
                title?.let {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.headlineSmall,
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) { it() }
                    Spacer(Modifier.height(16.dp))
                }
                text?.let {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    ) { it() }
                    Spacer(Modifier.height(24.dp))
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
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
