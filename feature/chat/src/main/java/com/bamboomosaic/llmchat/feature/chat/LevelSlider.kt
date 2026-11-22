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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * 21 轮：离散档位滑块（等间隔）。
 * - 每档占用等宽单元格，圆点停在单元格中心，进度条从第一档（如「关」）中心延申到圆点
 * - 圆点、进度条、下方标签三者共用同一坐标系，严格对齐
 * - 支持拖动与点按，档位切换带滑动动画
 * [activeColor] 可传警示色（终极档特效）。
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
        BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp)) {
            val stopCount = levels.size.coerceAtLeast(2)
            val cell = maxWidth / stopCount
            val animatedIndex by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = tween(200),
                label = "levelThumb",
            )
            fun indexAt(x: Dp): Int =
                floor((x / cell).coerceIn(0f, stopCount.toFloat() - 0.01f)).toInt()

            // 轨道：从第一档中心到最后档中心
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = cell / 2)
                    .width(maxWidth - cell)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackColor),
            )
            // 进度：从第一档中心延申到当前档中心（关=第一档时宽度为 0）
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = cell / 2)
                    .width(cell * animatedIndex)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(activeColor),
            )
            // 圆点：压在轨道上，位于当前档中心
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = cell * animatedIndex)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(activeColor),
            )
            // 点按选择
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(stopCount) {
                        detectTapGestures { offset -> onSelect(indexAt(offset.x.toDp())) }
                    },
            )
            // 拖动选择
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(stopCount) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            onSelect(indexAt(change.position.x.toDp()))
                        }
                    },
            )
        }
        Row(Modifier.fillMaxWidth()) {
            levels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selectedIndex) activeColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) },
                )
            }
        }
    }
}
