package com.bamboomosaic.llmchat.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.bamboomosaic.llmchat.core.domain.ApiError
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.MessageRole

/**
 * 桌面端主界面（桌面端第 2 轮）：左会话列表 + 右对话区 + 顶栏入口（API 配置 / 设置）。
 * 可用级 UI：只保证布局不溢出、鼠标/滚轮/窗口缩放可用，不做视觉打磨。
 */
@Composable
fun DesktopApp(vm: DesktopChatViewModel) {
    val settings by vm.settings.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val currentId by vm.currentId.collectAsState()
    val messages by vm.messages.collectAsState()
    val streamingText by vm.streamingText.collectAsState()
    val busy by vm.busy.collectAsState()
    val thinking by vm.thinking.collectAsState()
    val error by vm.error.collectAsState()
    val currentConfig by vm.currentConfig.collectAsState()

    var input by remember { mutableStateOf("") }
    var showConfig by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    // 流式回复跟随滚动到底部
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length, streamingText.length) {
        val count = messages.size + if (streamingText.isNotEmpty()) 1 else 0
        if (count > 0) listState.scrollToItem(count - 1)
    }

    fun send() {
        vm.send(input)
        input = ""
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── 左栏：会话列表 ──
                Column(
                    modifier = Modifier.width(230.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(onClick = { vm.newConversation() }, modifier = Modifier.fillMaxWidth()) {
                        Text("开启新对话")
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            ConversationRow(
                                conversation = conv,
                                selected = conv.id == currentId,
                                onSelect = { vm.selectConversation(conv.id) },
                                onRename = { vm.renameConversation(conv.id, it) },
                                onTogglePin = { vm.togglePinned(conv.id, !conv.pinned) },
                                onDelete = { vm.deleteConversation(conv.id) },
                            )
                        }
                    }
                    Text(
                        "会话：${conversations.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── 右区：对话 ──
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 顶栏：当前配置 / 模型 + 功能入口
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${currentConfig?.name ?: "未配置"} · ${currentConfig?.model ?: "—"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { showConfig = true }) { Text("API 配置") }
                        OutlinedButton(onClick = { showSettings = true }) { Text("设置") }
                    }

                    // 消息列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(messages, key = { _, m -> m.id }) { _, message ->
                            Bubble(message.role, message.content, showThinking = false)
                        }
                        if (streamingText.isNotEmpty() || (busy && thinking)) {
                            item {
                                Bubble(
                                    MessageRole.ASSISTANT,
                                    streamingText,
                                    showThinking = streamingText.isEmpty(),
                                )
                            }
                        }
                    }

                    // 输入区：Enter 发送 / Shift+Enter 换行
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            label = { Text("输入消息（Enter 发送，Shift+Enter 换行）") },
                            maxLines = 5,
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { e ->
                                    val isEnter = e.key == Key.Enter || e.key == Key.NumPadEnter
                                    if (isEnter && e.type == KeyEventType.KeyDown &&
                                        !e.isShiftPressed && !e.isCtrlPressed
                                    ) {
                                        send()
                                        true
                                    } else {
                                        false
                                    }
                                },
                        )
                        Button(onClick = { send() }, enabled = !busy) {
                            Text(if (busy) "发送中…" else "发送")
                        }
                    }
                    Text(
                        "语言：${settings.language.tag} · 省Token档位：${settings.tokenSavingLevel} · 密钥只存本机数据库",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (showConfig) ConfigDialog(vm = vm, onDismiss = { showConfig = false })
            if (showSettings) SettingsDialog(vm = vm, onDismiss = { showSettings = false })
            ErrorDialog(error = error, onDismiss = { vm.error.value = null })
        }
    }
}

/** 会话行：标题 + 置顶标记，hover 显示操作按钮（置顶/改名/删除）。 */
@Composable
private fun ConversationRow(
    conversation: Conversation,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var renaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(conversation.title) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        border = if (hovered && !selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interaction),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (if (conversation.pinned) "📌 " else "") + conversation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
            if (hovered) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSelect) { Text("打开") }
                    TextButton(onClick = onTogglePin) { Text(if (conversation.pinned) "取消置顶" else "置顶") }
                    TextButton(onClick = { renameText = conversation.title; renaming = true }) { Text("改名") }
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("重命名会话") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(renameText); renaming = false }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("取消") } },
        )
    }
}

/** 消息气泡：用户右对齐主色容器、AI 左对齐副色容器；hover 高亮边框；文本可选中复制。 */
@Composable
internal fun Bubble(role: MessageRole, text: String, showThinking: Boolean) {
    val isUser = role == MessageRole.USER
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            border = if (hovered) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .hoverable(interactionSource = interaction),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (showThinking) {
                    Text(
                        "（思考中…）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SelectionContainer {
                    Text(
                        text.ifEmpty { "…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/** 消息气泡：用户右对齐主色容器、AI 左对齐副色容器；hover 高亮边框；文本可选中复制。 */
@Composable
internal fun ErrorDialog(error: ApiError?, onDismiss: () -> Unit) {
    if (error == null) return
    // 与手机端 AppStrings 同文案：按 ApiError 分类给解决指引
    val guide = when (error.kind) {
        ApiError.Kind.AUTH -> "密钥无效 / 密钥填写错误，请检查 API 密钥"
        ApiError.Kind.INSUFFICIENT_BALANCE -> "余额不足，请前往服务商充值"
        ApiError.Kind.RATE_LIMIT -> "请求频率超限，请降低发送速度或稍后重试"
        ApiError.Kind.TIMEOUT -> "网络连接超时，请检查网络、代理、API 地址"
        ApiError.Kind.NETWORK -> "无网络连接，请检查设备网络"
        ApiError.Kind.UNKNOWN -> "请求失败，请核对配置或稍后重试"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
        title = { Text("请求失败") },
        text = { Text("${error.raw}\n\n$guide") },
    )
}

/** 简单下拉选择：按钮 + DropdownMenu（避开 ExposedDropdownMenuBox 的版本敏感 API）。 */
@Composable
internal fun SimpleDropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        // 注意：按钮不 fillMaxWidth——在 Row 里会吃掉剩余全部宽度，把后续控件挤出窗口
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                "$label：$value",
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelect(i)
                    },
                )
            }
        }
    }
}

@Composable
internal fun Spacer6() = Spacer(Modifier.height(6.dp))
