package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 27 轮：会话流程图面板（窗口式，非全屏）——竖版时间线可视化当前会话：
 * 用户消息 → AI 回复，多版本回复（重新生成 / 编辑重发产生）以分支小卡展示，
 * 当前显示版本高亮，点其他版本直接切换（复用 switchVersionTo，不改消息存储结构）。
 * 数据由 [ChatViewModel.loadFlowDiagram] 从现有消息列表推导。
 */
@Composable
fun FlowPanelDialog(
    turns: List<ChatViewModel.FlowTurn>,
    onSwitchTo: (uiIndex: Int, version: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassDialogShell(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wideDialogMax(480.dp)
                .heightIn(max = 620.dp),
        ) {
            Column {
                // 标题栏：← 返回 + 标题（与其他二级弹窗一致）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    TextButton(onClick = onDismiss) { Text("←") }
                    Text(
                        text = s.flowTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider()

                if (turns.isEmpty()) {
                    Text(
                        text = s.flowEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        turns.forEachIndexed { turnIdx, turn ->
                            FlowTurnRow(turn = turn, onSwitchTo = onSwitchTo)
                            if (turnIdx != turns.lastIndex) Spacer(Modifier.height(10.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/** 一轮对话：左侧时间线（圆点+竖线），右侧用户气泡 + AI 回复（多版本分支小卡）。 */
@Composable
private fun FlowTurnRow(
    turn: ChatViewModel.FlowTurn,
    onSwitchTo: (uiIndex: Int, version: Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        // 时间线列：节点圆点 + 贯穿竖线
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(18.dp)) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (turn.userText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = turn.userText.take(200),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            // AI 回复：单版本直卡；多版本 = 分支组（当前版本高亮，点其他版本切换）
            turn.replies.forEach { reply ->
                val active = reply.version == turn.activeVersion
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
                    border = if (active) {
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .then(
                            // 已入库的分支才能点击切换（无回复轮 / 单版本组不可点）
                            if (turn.uiIndex >= 0 && turn.replies.size > 1 && !active) {
                                Modifier.clickable { onSwitchTo(turn.uiIndex, reply.version) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (turn.replies.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "v${reply.version}" + if (active) " ✓" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(8.dp))
                                if (active) {
                                    Text(
                                        text = "‹ ${turn.activeVersion}/${turn.replies.size} ›",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!active) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = LocalAppStrings.current.flowTapToSwitch,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = reply.text.ifBlank { "…" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            color = if (active) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
