package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 20 轮：轻量按压反馈——按下轻微缩放，松开回弹。
 * [interaction] 必须与目标按钮的 clickable/IconButton 共用同一个实例。
 * 须在 Composable 作用域调用。
 */
@Composable
fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
