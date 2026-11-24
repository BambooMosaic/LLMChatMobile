package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 27 轮修正：会话流程图面板（窗口式，非全屏）——**平面分支树**可视化当前会话：
 * - 每一轮【重新生成】是 AI 回复的一层分支（横排，标「第 N 次生成」，不展示正文）；
 * - 每一次【编辑对话】是一个用户消息分支（横排，标内容摘要 + 「改N」）；
 * - 各分支下的子对话各自延伸成子树（上下文互相隔离，当前激活路径高亮）；
 * - 点击非激活分支直接切换（复用 [ChatViewModel.selectBranch]，不改消息存储结构）。
 */
@Composable
fun FlowPanelDialog(
    nodes: List<ChatViewModel.FlowNodeUi>,
    onSelectBranch: (messageId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassDialogShell(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wideDialogMax(520.dp)
                .heightIn(max = 600.dp),
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
                Text(
                    text = s.flowTapToSwitch,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )

                if (nodes.isEmpty()) {
                    Text(
                        text = s.flowEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            nodes.forEach { root -> FlowNodeColumn(root, onSelectBranch) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 递归树节点列：节点胶囊 + 子分支组横向并排（每列继续递归）。
 * 激活分支高亮描边，非激活半透明可点击。
 */
@Composable
private fun FlowNodeColumn(
    node: ChatViewModel.FlowNodeUi,
    onSelectBranch: (messageId: Long) -> Unit,
) {
    Column(horizontalAlignment = Alignment.Start) {
        FlowNodeChip(node, onSelectBranch)
        if (node.children.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = 10.dp, top = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // 分支连接竖线
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )
                }
                Spacer(Modifier.width(8.dp))
                node.children.forEach { child ->
                    Column(modifier = Modifier.padding(end = 10.dp)) {
                        FlowNodeColumn(child, onSelectBranch)
                    }
                }
            }
        }
    }
}

/** 节点胶囊：圆点 + 标签（用户=内容摘要；AI=「第 N 次生成」）。 */
@Composable
private fun FlowNodeChip(
    node: ChatViewModel.FlowNodeUi,
    onSelectBranch: (messageId: Long) -> Unit,
) {
    val dotColor = when {
        node.active -> MaterialTheme.colorScheme.primary
        node.isUser -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (node.active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f),
        border = if (node.active) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier
            .then(
                if (!node.active) {
                    Modifier.clickable { onSelectBranch(node.messageId) }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = node.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                color = if (node.active) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}
