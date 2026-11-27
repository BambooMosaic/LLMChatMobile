package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 27 轮修正：会话流程图面板——**Canvas 自绘分层树**（真正的流程图）：
 * - 自上而下：用户消息 → 「第 N 次生成」回复 → 下一轮用户消息……
 * - 每一层【重新生成】版本组 / 【编辑对话】用户分支横向展开，边线上标分支序号；
 * - 当前激活路径高亮；点击任意非激活节点切到该分支（上下文随之切换）。
 * 节点不展示正文：用户节点为内容摘要、AI 节点为生成次数标签。
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
                .heightIn(max = 620.dp),
        ) {
            Column {
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                if (nodes.isEmpty()) {
                    Text(
                        text = s.flowEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    val measurer = rememberTextMeasurer()
                    val density = LocalDensity.current
                    val baseNodeW = with(density) { 96.dp.toPx() }
                    val nodeH = with(density) { 30.dp.toPx() }
                    val colW = with(density) { 106.dp.toPx() }
                    val rowH = with(density) { 56.dp.toPx() }
                    val padL = with(density) { 14.dp.toPx() }
                    val padT = with(density) { 12.dp.toPx() }
                    val rootGap = with(density) { 18.dp.toPx() }

                    // 布局：逐棵主线树分配叶子列游标；纵向堆叠（每棵树带行偏移）
                    val layouts = remember(nodes) {
                        val cursor = intArrayOf(0)
                        var rowOffset = 0
                        val out = mutableListOf<RootLayout>()
                        nodes.forEach { root ->
                            val maxDepthBox = intArrayOf(0)
                            val laid = layoutTree(root, 0, depth = 0, cursor = cursor, maxDepth = maxDepthBox)
                            out += RootLayout(laid, rowOffset, maxDepthBox[0])
                            rowOffset += maxDepthBox[0] + 2 // 树间留一行空隙
                        }
                        out
                    }
                    val totalCols = layouts.maxOfOrNull { it.root.maxCol + 1 } ?: 1
                    val totalRows = layouts.lastOrNull()?.let { it.rowOffset + it.maxDepth + 1 } ?: 0
                    val canvasW = padL * 2 + totalCols * colW
                    val canvasH = padT * 2 + totalRows * rowH + rootGap * (layouts.size - 1).coerceAtLeast(0)

                    val lineColor = MaterialTheme.colorScheme.outlineVariant
                    val activeFill = MaterialTheme.colorScheme.primaryContainer
                    val activeBorder = MaterialTheme.colorScheme.primary
                    val idleFill = MaterialTheme.colorScheme.surfaceContainerLow
                    val idleText = MaterialTheme.colorScheme.onSurfaceVariant
                    val activeText = MaterialTheme.colorScheme.onSurface
                    val numColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        val hitRects = remember { mutableListOf<Pair<Long, Rect>>() }
                        Canvas(
                            modifier = Modifier
                                .width(with(density) { canvasW.toDp() })
                                .height(with(density) { canvasH.toDp() })
                                .pointerInput(layouts) {
                                    detectTapGestures { pos ->
                                        hitRects.lastOrNull { (_, r) -> r.contains(pos) }?.let { (id, _) ->
                                            onSelectBranch(id)
                                        }
                                    }
                                },
                        ) {
                            hitRects.clear()
                            val styleIdle = TextStyle(fontSize = 10.sp, color = idleText)
                            val styleActive = TextStyle(fontSize = 10.sp, color = activeText)
                            val styleNum = TextStyle(fontSize = 9.sp, color = numColor)

                            fun drawLaid(laid: TreeLaid, rowOffset: Int) {
                                // 28 轮：AI 节点无文字 → 半宽小方块（居中于连线）
                                val nodeW = if (laid.node.label.isEmpty()) baseNodeW * 0.45f else baseNodeW
                                val x = padL + laid.col * colW + (baseNodeW - nodeW) / 2f
                                val y = padT + rowOffset * rowH + laid.depthInRoot * rowH
                                val fill = if (laid.node.active) activeFill else idleFill
                                val border = if (laid.node.active) activeBorder else Color.Transparent
                                drawRoundRect(
                                    color = fill,
                                    topLeft = Offset(x, y),
                                    size = Size(nodeW, nodeH),
                                    cornerRadius = CornerRadius(7.dp.toPx()),
                                )
                                if (border != Color.Transparent) {
                                    drawRoundRect(
                                        color = border,
                                        topLeft = Offset(x, y),
                                        size = Size(nodeW, nodeH),
                                        cornerRadius = CornerRadius(7.dp.toPx()),
                                        style = Stroke(width = 1.5.dp.toPx()),
                                    )
                                }
                                if (laid.node.label.isNotEmpty()) {
                                    val tl = measurer.measure(
                                        laid.node.label,
                                        style = if (laid.node.active) styleActive else styleIdle,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                    drawText(tl, topLeft = Offset(x + (nodeW - tl.size.width) / 2f, y + (nodeH - tl.size.height) / 2f))
                                }
                                hitRects += laid.node.messageId to Rect(Offset(x, y), Size(nodeW, nodeH))

                                // 子分支：折线（父底 → midY → 横 → 子顶），分支边标序号
                                val bx = x + nodeW / 2
                                val by = y + nodeH
                                laid.kids.forEach { child ->
                                    val cx = padL + child.col * colW + baseNodeW / 2
                                    val cyTop = padT + rowOffset * rowH + child.depthInRoot * rowH
                                    val midY = (by + cyTop) / 2f
                                    drawLine(lineColor, Offset(bx, by), Offset(bx, midY), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    drawLine(lineColor, Offset(bx, midY), Offset(cx, midY), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    drawLine(lineColor, Offset(cx, midY), Offset(cx, cyTop), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    if (child.branchIndex > 0) {
                                        val numLayout = measurer.measure(child.branchIndex.toString(), style = styleNum, maxLines = 1, softWrap = false)
                                        drawText(
                                            numLayout,
                                            topLeft = Offset((bx + cx) / 2f - numLayout.size.width / 2f, midY - numLayout.size.height - 2.dp.toPx()),
                                        )
                                    }
                                    drawLaid(child, rowOffset)
                                }
                            }

                            layouts.forEach { rootLayout ->
                                drawLaid(rootLayout.root, rootLayout.rowOffset)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 一棵主线树的布局结果：根 + 行偏移（多棵树纵向堆叠用）。 */
private class RootLayout(val root: TreeLaid, val rowOffset: Int, val maxDepth: Int)

/** 布局后的节点：叶子均分配列；depthInRoot=树内深度（行）；branchIndex=分支序号（0=主线延续）。 */
private class TreeLaid(
    val node: ChatViewModel.FlowNodeUi,
    val col: Int,
    val depthInRoot: Int,
    val branchIndex: Int,
    val kids: List<TreeLaid>,
) {
    val maxCol: Int get() = kids.maxOfOrNull { it.maxCol } ?: col
}

/** 分层树布局：叶子均分配列，内部节点取孩子列中位。 */
private fun layoutTree(
    root: ChatViewModel.FlowNodeUi,
    branchIndex: Int,
    depth: Int,
    cursor: IntArray,
    maxDepth: IntArray,
): TreeLaid {
    val kids = root.children.mapIndexed { i, child ->
        layoutTree(child, i + 1, depth + 1, cursor, maxDepth)
    }
    if (depth > maxDepth[0]) maxDepth[0] = depth
    val col = if (kids.isEmpty()) cursor[0]++ else (kids.first().col + kids.last().col) / 2
    return TreeLaid(root, col, depth, branchIndex, kids)
}
