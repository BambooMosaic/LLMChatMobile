package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 21 轮：离散档位滑块——支持拖动与点击，档位切换带滑动动画；
 * 文字标签排在小滑块下方，左右两端分别对齐。
 * [activeColor] 可传入警示色（终极档特效）。
 */
@Composable
fun LevelSlider(
    levels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val travel: Dp = maxWidth - 20.dp
            val animated = animateFloatAsState(
                targetValue = selectedIndex / (levels.size - 1).coerceAtLeast(1).toFloat(),
                animationSpec = tween(200),
                label = "levelThumb",
            )
            fun indexAt(x: Dp): Int {
                val fraction = (x / (maxWidth.coerceAtLeast(1.dp))).coerceIn(0f, 1f)
                return (fraction * (levels.size - 1)).roundToInt().coerceIn(0, levels.size - 1)
            }
            // 轨道
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackColor),
            )
            // 已选中填充段
            Box(
                Modifier
                    .width((travel * animated.value) + 20.dp)
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(3.dp))
                    .background(activeColor),
            )
            // 滑块圆点
            Box(
                Modifier
                    .offset(x = travel * animated.value)
                    .size(20.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(activeColor),
            )
            // 点击选择
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(levels.size) {
                        detectTapGestures { offset -> onSelect(indexAt(offset.x.toDp())) }
                    },
            )
            // 拖动选择
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(levels.size) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            onSelect(indexAt(change.position.x.toDp()))
                        }
                    },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            levels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selectedIndex) activeColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (i) {
                        0 -> androidx.compose.ui.text.style.TextAlign.Start
                        levels.size - 1 -> androidx.compose.ui.text.style.TextAlign.End
                        else -> androidx.compose.ui.text.style.TextAlign.Center
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) },
                )
            }
        }
    }
}
