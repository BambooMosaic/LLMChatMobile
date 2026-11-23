package com.bamboomosaic.llmchat.feature.chat

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 24 轮：实验性毛玻璃 / 半透明磨砂材质。
 * - [LocalExperimentalGlass]：全局开关（设置 → 其他设置 → 实验性功能，默认关）
 * - [GlassDialogEffect]：对 Compose Dialog 窗口应用系统级背景模糊（Android 12+）并压低遮罩，
 *   旧系统自动跳过（仅表现为不模糊，不影响功能）
 * - [glassSurfaceColor]：弹窗表面色——开启时降透明度让模糊透出来，关闭时与原版一致
 * 性能与兼容性自负：低端机开实时模糊可能掉帧，故做成默认关闭的实验性开关。
 */
val LocalExperimentalGlass = staticCompositionLocalOf { false }

/** 24 轮：对当前 Compose Dialog 的窗口启用背景模糊（实验性毛玻璃，Android 12+ 生效）。 */
@Composable
fun GlassDialogEffect(enabled: Boolean = LocalExperimentalGlass.current) {
    if (!enabled) return
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val canBlur = window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val originalDim = window?.attributes?.dimAmount ?: 0f
        if (window != null && canBlur) {
            // 窗口背景透明 + 背景模糊半径，让宿主窗口内容模糊透出
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setBackgroundBlurRadius(90)
            window.setDimAmount(0.05f)
        }
        onDispose {
            if (window != null && canBlur) {
                window.setBackgroundBlurRadius(0)
                window.setDimAmount(originalDim)
            }
        }
    }
}

/** 24 轮：弹窗表面色——毛玻璃开启时降透明度（配合窗口模糊呈现磨砂质感）。 */
@Composable
fun glassSurfaceColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    return if (LocalExperimentalGlass.current) surface.copy(alpha = 0.86f) else surface
}
