package com.bamboomosaic.llmchat.feature.chat

import kotlin.math.max

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 应用图标（从 PackageManager 加载，裁成圆形），供开屏和引导页复用。 */
@Composable
internal fun AppIconCircular(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon = remember { context.packageManager.getApplicationIcon(context.packageName) }
    val bitmap = remember(icon) { icon.toImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

private fun android.graphics.drawable.Drawable.toImageBitmap(): ImageBitmap =
    if (this is android.graphics.drawable.BitmapDrawable) {
        bitmap.asImageBitmap()
    } else {
        val w = max(1, intrinsicWidth)
        val h = max(1, intrinsicHeight)
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        setBounds(0, 0, w, h)
        draw(canvas)
        bmp.asImageBitmap()
    }

/** 开屏启动动画：应用图标缩放淡入 + 应用名淡入，约 1 秒后回调。 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = tween(700),
        label = "splashScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700),
        label = "splashAlpha",
    )

    // 27 轮：平板模式开屏显示 LLMChatPro（手机保持 LLMChatMobile；应用内其余文案不受影响）
    val splashWide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(1000)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconCircular(modifier = Modifier.size(120.dp).scale(scale).alpha(alpha))
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (splashWide) "LLMChatPro" else "LLMChatMobile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(alpha),
        )
    }
}
