package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.DailyTokenStats
import com.bamboomosaic.llmchat.core.domain.HourlyTokenStats
import com.bamboomosaic.llmchat.core.domain.TokenTotals
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlin.math.max

/**
 * Token 统计二级页：与设置页风格一致（Surface 卡片 + ← 返回 + titleMedium 标题）。
 * 顶部三张累计卡片，下方 1天/7天切换 + Compose 原生柱状图。
 */
@Composable
fun TokenStatsScreen(
    totals: TokenTotals,
    dailyStats: List<DailyTokenStats>,
    hourlyStats: List<HourlyTokenStats>,
    days: Int,
    onSetDays: (Int) -> Unit,
    onDismiss: () -> Unit,
    /** 23 轮：当前会话累计（金额折算"本次"用）；null = 无会话。 */
    sessionTotals: TokenTotals? = null,
    /** 23 轮：当前模型名（决定单价；无单价模型不展示金额行）。 */
    currentModel: String = "",
) {
    val s = LocalAppStrings.current
    // 24 轮：实验性毛玻璃（Dialog 窗口由 ChatScreen 侧提供）
    GlassDialogEffect()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = glassSurfaceColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(max = 560.dp),
    ) {
        Column {
            // 标题栏：← 返回 + 标题（对齐设置二级页）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                TextButton(onClick = onDismiss) { Text("←") }
                Text(
                    text = s.tokenSummary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // 顶部四张累计卡片（21 轮：新增缓存命中）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TokenCard(label = s.tokenCacheHit, value = totals.cacheHitTokens, modifier = Modifier.weight(1f))
                    TokenCard(label = s.inputTokens, value = totals.promptTokens, modifier = Modifier.weight(1f))
                    TokenCard(label = s.outputTokens, value = totals.completionTokens, modifier = Modifier.weight(1f))
                    TokenCard(label = s.totalTokens, value = totals.total, modifier = Modifier.weight(1f))
                }

                // 23 轮：金额折算行（本次 = 当前会话累计 / 累计 = 全应用累计）；无单价模型不展示
                val sessionCost = sessionTotals?.let {
                    com.bamboomosaic.llmchat.core.domain.ModelPricing.estimateCNY(
                        currentModel, it.promptTokens, it.completionTokens, it.cacheHitTokens,
                    )
                }
                val globalCost = com.bamboomosaic.llmchat.core.domain.ModelPricing.estimateCNY(
                    currentModel, totals.promptTokens, totals.completionTokens, totals.cacheHitTokens,
                )
                if (sessionCost != null && globalCost != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = s.costSummary(
                            com.bamboomosaic.llmchat.core.domain.ModelPricing.formatCNY(sessionCost),
                            com.bamboomosaic.llmchat.core.domain.ModelPricing.formatCNY(globalCost),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 周期切换：近 1 天 / 近 7 天
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PeriodButton(text = s.last1Day, selected = days == 1, onClick = { onSetDays(1) }, modifier = Modifier.weight(1f))
                    PeriodButton(text = s.last7Days, selected = days == 7, onClick = { onSetDays(7) }, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))

                if (days == 1) {
                    TokenHourlyBarChart(stats = hourlyStats, modifier = Modifier.fillMaxWidth())
                } else {
                    TokenBarChart(stats = dailyStats, days = days, modifier = Modifier.fillMaxWidth())
                }

                // 21 轮：三色图例（缓存命中 / 输入 / 输出）
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 22 轮：三色改为主题色透明度分层（不再硬编码紫色）
                    LegendDot(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), label = s.tokenCacheHit)
                    LegendDot(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), label = s.inputTokens)
                    LegendDot(color = MaterialTheme.colorScheme.primary, label = s.outputTokens)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TokenCard(label: String, value: Long, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatCount(value),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PeriodButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable { onClick() },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

/** Compose 原生柱状图：按天数铺满柱子，高度按最大值归一化；21 轮起三色分段（缓存命中/输入/输出）。 */
@Composable
private fun TokenBarChart(
    stats: List<DailyTokenStats>,
    days: Int,
    modifier: Modifier = Modifier,
) {
    // SQLite 返回 yyyy-MM-dd，这里统一取 MM-dd 作为 key，与下方标签格式一致
    val byDate = stats.associate { it.date.takeLast(5) to it }
    // 生成连续日期序列（最近 days 天，MM-dd），缺失的补 0
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val labels = mutableListOf<String>()
    repeat(days) { i ->
        val c = cal.clone() as java.util.Calendar
        c.add(java.util.Calendar.DAY_OF_MONTH, -(days - 1 - i))
        labels.add(String.format("%02d-%02d", c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH)))
    }
    val items = labels.map { label -> byDate[label] ?: DailyTokenStats(date = label, tokens = 0) }
    val maxValue = items.maxOf { it.tokens } ?: 0L
    // 22 轮：柱状三色改为主题色透明度分层
    val cacheColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val inputColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    val outputColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val chartHeight = 120.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items.forEachIndexed { index, stat ->
            val value = stat.tokens
            val fraction = if (maxValue > 0) max(0.03f, value.toFloat() / maxValue.toFloat()) else 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 数值（无数据时占位保持高度一致）
                Text(
                    text = if (value > 0) formatCount(value) else " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                // 三色分段柱（自下而上：缓存命中 / 输入 / 输出）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (value <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight * fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(emptyColor),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight * fraction),
                        ) {
                            val totalF = value.toFloat().coerceAtLeast(1f)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((chartHeight * fraction * (stat.outputTokens / totalF)))
                                    .background(outputColor),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((chartHeight * fraction * (stat.inputTokens / totalF)))
                                    .background(inputColor),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(cacheColor),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = labels[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 图例：彩色圆点 + 文本。 */
@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 近 1 天的 3 小时分段柱状图：8 根柱子（00/03/.../21），21 轮起三色分段。 */
@Composable
private fun TokenHourlyBarChart(
    stats: List<HourlyTokenStats>,
    modifier: Modifier = Modifier,
) {
    val byHour = stats.associate { it.hour to it }
    val hours = (0..21 step 3).toList()
    val items = hours.map { hour -> byHour[hour] ?: HourlyTokenStats(hour = hour, tokens = 0) }
    val maxValue = items.maxOf { it.tokens } ?: 0L
    // 22 轮：柱状三色改为主题色透明度分层
    val cacheColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val inputColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    val outputColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val chartHeight = 120.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items.forEachIndexed { index, stat ->
            val value = stat.tokens
            val fraction = if (maxValue > 0) max(0.03f, value.toFloat() / maxValue.toFloat()) else 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (value > 0) formatCount(value) else " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (value <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight * fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(emptyColor),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chartHeight * fraction),
                        ) {
                            val totalF = value.toFloat().coerceAtLeast(1f)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((chartHeight * fraction * (stat.outputTokens / totalF)))
                                    .background(outputColor),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((chartHeight * fraction * (stat.inputTokens / totalF)))
                                    .background(inputColor),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(cacheColor),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format("%02d", stat.hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun formatCount(value: Long): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
    value >= 1_000 -> String.format("%.1fK", value / 1_000f)
    else -> value.toString()
}
