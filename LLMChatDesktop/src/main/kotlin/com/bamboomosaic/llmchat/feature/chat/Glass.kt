package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 桌面端 Glass 降级版（全量移植轮）：
 * - Windows 无 Android 的 blurBehind 跨窗模糊 API，玻璃档固定 0（LocalGlassLevel = 0），
 *   所有弹窗走非玻璃分支（实色 Surface），观感与手机端关闭玻璃时一致；
 * - API 签名与手机端完全一致（GlassDialogEffect / GlassDialogShell / glassSurfaceColor /
 *   GlassPopupEffect / GlassAlertDialog / wideDialogMax），调用代码零改动；
 * - TODO 后续接 Windows DWM 模糊（SetWindowCompositionAttribute）实现桌面专属毛玻璃。
 */

/** 玻璃材质档位本地值（桌面端固定 0 = 不启用，实色弹窗）。 */
val LocalGlassLevel = staticCompositionLocalOf { 0 }

/** 30 轮：全局统一轻度模糊半径（dp）——桌面端保留常量避免调用点编译错误。 */
internal val GLASS_BLUR_RADIUS = 10.dp

/** 桌面端无窗口 blur API：空操作（与关闭玻璃时行为一致）。 */
@Composable
fun GlassDialogEffect(enabled: Boolean = LocalGlassLevel.current >= 1, cornerRadius: Dp = 24.dp) {
}

/** 弹窗玻璃外壳：桌面端恒走非玻璃分支（实色 Surface），与手机端关闭玻璃时外观一致。 */
@Composable
fun GlassDialogShell(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
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
    // 桌面端 LocalGlassLevel 固定 0，此分支不可达；保留实现以维持 API 语义
    GlassDialogEffect(cornerRadius = cornerRadius)
    Surface(
        modifier = modifier
            .border(width = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape),
        shape = shape,
        // 桌面端：没有窗口背景 drawable 打底，外壳自己承担半透明底色（修复全透明）
        color = glassSurfaceColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { content() }
}

/** 弹窗表面色：桌面端玻璃关闭，恒为实色 surface。 */
@Composable
fun glassSurfaceColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    if (LocalGlassLevel.current < 1) return surface
    return surface.copy(alpha = 0.85f)
}

/**
 * 桌面端允许滑到毛玻璃档：Windows 暂无实时模糊（TODO DWM），毛玻璃档自动降级为
 * 半透明观感；返回 true 保证滑块可自由拖动（false 会把滑块锁死在半透明档）。
 */
@Composable
fun blurSupported(): Boolean = true

/** 小菜单玻璃：桌面端空操作（与手机端降级行为一致）。 */
@Composable
fun GlassPopupEffect(enabled: Boolean = LocalGlassLevel.current >= 1) {
}

/** 玻璃版 AlertDialog：布局对齐 M3（与手机端实现一致，仅无窗口玻璃打底）。 */
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

/** 宽屏弹窗限宽：桌面端用窗口容器宽度（LocalWindowInfo）替代 Android 的 LocalConfiguration。 */
@Composable
fun Modifier.wideDialogMax(maxWidth: androidx.compose.ui.unit.Dp): Modifier {
    val wide = LocalWindowInfo.current.containerDpSize.width >= 600.dp
    return if (wide) this.then(Modifier.widthIn(max = maxWidth)) else this
}
