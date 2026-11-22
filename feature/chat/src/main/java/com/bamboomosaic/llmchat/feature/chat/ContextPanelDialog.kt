package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bamboomosaic.llmchat.core.domain.TokenTotals
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 21 轮：上下文统计圆角弹窗——本对话命中/输入/输出/联网搜索/深度思考、容量占比、三分项条形、手动压缩入口。
 * 22 轮：容量分项移除联网搜索（搜索内容不进上下文）；token 行增加联网搜索/深度思考分项。
 */
@Composable
fun ContextPanelDialog(
    usage: ChatViewModel.ContextUsage,
    sessionTotals: TokenTotals,
    onManualCompress: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    // 22 轮：联网搜索内容不进上下文，容量分项只剩三项（系统提示词/消息/深度思考）
    val segSystem = Color(0xFF2E2660)
    val segMessages = Color(0xFF9B8CFF)
    val segReasoning = Color(0xFFD9CCFF)
    val totalSeg = listOf(
        Triple(s.segSystem, usage.systemChars, segSystem),
        Triple(s.segMessages, usage.messageChars, segMessages),
        Triple(s.segReasoning, usage.reasoningChars, segReasoning),
    )
    val usedSeg = totalSeg.sumOf { it.second }.coerceAtLeast(1)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(s.contextStatsTitle, style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(16.dp))
                Text(s.contextCapacity, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                // 容量占用条：三分项（系统提示词 / 消息 / 深度思考）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row {
                        totalSeg.forEach { (_, chars, color) ->
                            val w = if (usedSeg <= 0) 0f else chars.toFloat() / usedSeg
                            if (w > 0f) {
                                Box(
                                    Modifier
                                        .weight(w)
                                        .height(14.dp)
                                        .background(color),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                totalSeg.forEach { (label, chars, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$label：${formatCount(chars)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${s.contextUsed} ${formatCount(usage.usedChars)} / ${s.contextCapacity} ${formatCount(usage.capacityTokens)}" +
                        "（${(usage.fraction * 100).toInt()}%）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                // 22 轮：会话 token 行扩为五项（缓存命中/输入/输出/联网搜索/深度思考）
                Text(
                    text = s.tokenCacheHit + " / " + s.inputTokens + " / " + s.outputTokens +
                        " / " + s.segSearch + " / " + s.segReasoning,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatCount(sessionTotals.cacheHitTokens)} / ${formatCount(sessionTotals.promptTokens)} / " +
                        "${formatCount(sessionTotals.completionTokens)} / ${formatCount(sessionTotals.searchTokens)} / " +
                        "${formatCount(sessionTotals.reasoningTokens)}",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(20.dp))
                Button(onClick = onManualCompress, modifier = Modifier.fillMaxWidth()) {
                    Text(s.manualCompressButton)
                }
            }
        }
    }
}
