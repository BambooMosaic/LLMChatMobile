package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 27 轮修正：大版本更新公告（窗口式，轻量）——替代旧版「大版本重走首次引导」。
 * 只展示新版亮点，确认后不再出现（记录当前大版本段）；配置与数据完全不动。
 */
@Composable
fun UpdateNoticeDialog(
    versionName: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassDialogShell(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wideDialogMax(420.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "更新公告",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "beta0.7 · $versionName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                ) {
                    MarkdownText(content = UPDATE_NOTICE_BODY, enableHorizontalScroll = false)
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("开始使用")
                    }
                }
            }
        }
    }
}

private const val UPDATE_NOTICE_BODY = """### beta0.7 更新亮点

- **提示词模板库**：选择模式弹窗新增入口，5 类 20 个轻量模板（润色 / 翻译 / 摘要 / 代码 / 学习），点选直接填入补充上下文
- **会话流程图**：顶栏环形按钮左侧新分支图标，竖版平面图展示对话流程，AI 回复按「第 N 次生成」编号，点分支即可切换
- **消息分叉**：每一次重新生成 / 编辑对话都是独立分支，各分支上下文完全隔离，互不可见；Token 统计仍按全会话累计
- **语音朗读**：AI 回复消息下方新增朗读按钮（DeepSeek TTS，海星 / 暗潮两种音色），设置 → 其他设置可开关与选音色
- **平板界面**：模式选择窗口改为两列式，各弹窗平板上限宽居中不再拉伸；平板开屏显示 LLMChatPro
"""
