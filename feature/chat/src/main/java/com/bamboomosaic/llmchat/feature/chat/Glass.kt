package com.bamboomosaic.llmchat.feature.chat

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 24 轮：实验性毛玻璃 / 半透明磨砂材质。三层方案，任何设备都能看到磨砂效果：
 * 1. [GlassDialogShell] 通用降级——打开弹窗瞬间截取宿主窗口内容（decorView 离屏绘制），
 *    缩小再放大（双线性柔化）铺在弹窗底层；纯软件实现，全版本可用。
 *    背景为打开瞬间的静态画面，聊天场景足够。
 * 2. Android 12+ 且系统允许跨窗口模糊时，叠加系统级 blurBehindRadius（更真实的实时模糊）。
 * 3. 表面染色（主色 surface 半透明）保证文字可读性；深色主题近乎不透明，避免文字发黑。
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
 * 24 轮：弹窗玻璃外壳——毛玻璃开启时：快照背景 + 表面染色 + 细边框（毛玻璃模式无系统阴影可依赖）；
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
    Box(
        modifier = modifier
            .clip(shape)
            .border(width = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        BackdropSnapshot(modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)))
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { content() }
    }
}

/**
 * 24 轮：宿主窗口快照背景——decorView 离屏绘制 → 缩小 1/10 → 铺满弹窗，
 * 双线性放大后即为柔和磨砂（纯软件实现，任何设备可用）。
 */
@Composable
private fun BackdropSnapshot(modifier: Modifier = Modifier) {
    val view = LocalView.current
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        val decor = view.context.findActivity()?.window?.decorView ?: return@LaunchedEffect
        withContext(Dispatchers.Main) {
            runCatching {
                val w = decor.width.coerceAtLeast(1)
                val h = decor.height.coerceAtLeast(1)
                val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                decor.draw(android.graphics.Canvas(full))
                snapshot = Bitmap.createScaledBitmap(full, (w / 10).coerceAtLeast(1), (h / 10).coerceAtLeast(1), true)
            }
        }
    }
    snapshot?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** 24 轮：弹窗表面色——毛玻璃开启时降透明度（深色主题近乎不透明，保证文字可读）。 */
@Composable
fun glassSurfaceColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    if (!LocalExperimentalGlass.current) return surface
    val dark = surface.luminance() < 0.5f
    return surface.copy(alpha = if (dark) 0.94f else 0.86f)
}
