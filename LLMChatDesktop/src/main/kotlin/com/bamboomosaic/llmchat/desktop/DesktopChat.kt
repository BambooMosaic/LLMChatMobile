package com.bamboomosaic.llmchat.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.ApiError
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.ContextReminderProviderImpl
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.core.domain.ModeConfig
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.SystemPromptBuilder
import com.bamboomosaic.llmchat.core.network.OpenAiSseClient
import com.bamboomosaic.llmchat.core.network.SseEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 会话消息（桌面端内存态；持久化留到下一轮 DataStore-KMP）。 */
private data class UiMessage(val role: MessageRole, val text: String)

/**
 * 34 轮：桌面端基础对话页。
 * - 复用共享域：自定义模式 system 提示词（SystemPromptBuilder）+ 环境快照注入当前轮
 *   用户消息开头（"[当前环境] " + reminder），与手机端请求组装逻辑一致
 * - 复用共享网络层 OpenAiSseClient：流式 SSE 逐字渲染
 * - 服务商/模型/密钥均为内存态（本轮不做持久化）；错误复用 ApiError 分类与手机端同款文案，
 *   以桌面弹窗展示（1308 等 Coding Plan 限额错误由服务端以 HTTP 429 返回，落入限流分类）
 */
@Composable
fun DesktopChatApp() {
    val scope = rememberCoroutineScope()
    val sseClient = remember { OpenAiSseClient() }

    // 连接配置（内存态）
    var selectedProvider by remember {
        mutableStateOf(Providers.all.firstOrNull { it.id == "glm" } ?: Providers.all.first())
    }
    var model by remember { mutableStateOf(selectedProvider.models.firstOrNull() ?: "") }
    var apiKey by remember { mutableStateOf("") }
    var customBaseUrl by remember { mutableStateOf("") }

    // 会话状态（内存态）
    var messages by remember { mutableStateOf(emptyList<UiMessage>()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiError?>(null) }

    val listState = rememberLazyListState()
    val effectiveBaseUrl =
        if (selectedProvider.id == Providers.CUSTOM) customBaseUrl.trim() else selectedProvider.baseUrl

    // 流式回复到达时跟随滚动到底部（简单策略：每次列表更新都贴底）
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        if (effectiveBaseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            error = ApiError(
                ApiError.Kind.UNKNOWN,
                "请先填写 API 密钥" +
                    if (selectedProvider.id == Providers.CUSTOM) "与 API 地址" else "",
            )
            return
        }
        // 手机端同款组装：自定义模式 system + 环境快照注入当前轮用户消息
        val mode = ChatModes.find(ModeConfig.CUSTOM, AppLanguage.SIMPLIFIED_CHINESE)
        val systemText = SystemPromptBuilder.build(
            mode, userPrompt = null, language = AppLanguage.SIMPLIFIED_CHINESE,
        ).prompt
        val reminder = ContextReminderProviderImpl().buildReminder(AppLanguage.SIMPLIFIED_CHINESE)
        val payload = listOf(ChatMessage(MessageRole.SYSTEM, systemText)) +
            messages.map { ChatMessage(it.role, it.text) } +
            ChatMessage(MessageRole.USER, "[当前环境] $reminder\n\n$text")

        messages = messages + UiMessage(MessageRole.USER, text) + UiMessage(MessageRole.ASSISTANT, "")
        input = ""
        busy = true
        thinking = false
        scope.launch {
            try {
                sseClient.stream(
                    baseUrl = effectiveBaseUrl,
                    apiKey = apiKey,
                    model = model,
                    messages = payload,
                ).collect { event ->
                    when (event) {
                        is SseEvent.Text -> {
                            thinking = false
                            messages = messages.toMutableList().also { list ->
                                val last = list.removeAt(list.lastIndex)
                                list.add(last.copy(text = last.text + event.chunk))
                            }
                        }
                        is SseEvent.Reasoning -> thinking = true
                        else -> Unit // Usage / 搜索事件桌面端本轮忽略
                    }
                }
                if (messages.lastOrNull()?.text?.isBlank() == true) {
                    // 失败/空气泡不保留（与手机端一致：请求失败的空气泡不显示）
                    messages = messages.dropLast(1)
                    error = ApiError(ApiError.Kind.UNKNOWN, "模型返回了空回复")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (messages.lastOrNull()?.role == MessageRole.ASSISTANT &&
                    messages.lastOrNull()?.text?.isBlank() == true
                ) {
                    messages = messages.dropLast(1)
                }
                error = ApiError.from(e.message)
            } finally {
                busy = false
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 顶栏：服务商 / 模型 / 密钥 / 清空
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SimpleDropdown(
                        label = "服务商",
                        value = selectedProvider.displayName,
                        options = Providers.all.map { it.displayName },
                        modifier = Modifier.widthIn(min = 150.dp),
                        onSelect = { i ->
                            selectedProvider = Providers.all[i]
                            model = selectedProvider.models.firstOrNull() ?: ""
                        },
                    )
                    if (selectedProvider.id == Providers.CUSTOM) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("模型") },
                            singleLine = true,
                            modifier = Modifier.widthIn(min = 160.dp),
                        )
                    } else {
                        SimpleDropdown(
                            label = "模型",
                            value = model,
                            options = selectedProvider.models,
                            modifier = Modifier.widthIn(min = 160.dp),
                            onSelect = { model = selectedProvider.models[it] },
                        )
                    }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API 密钥（仅内存保存）") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            messages = emptyList()
                            thinking = false
                            error = null
                        },
                    ) { Text("清空") }
                }
                if (selectedProvider.id == Providers.CUSTOM) {
                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("API 地址（含版本段，如 https://host/v1）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 消息气泡列表：LazyColumn 天然支持鼠标滚轮；窗口缩放自动重排
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(messages) { index, message ->
                        Bubble(
                            message = message,
                            showThinking = thinking && index == messages.lastIndex,
                        )
                    }
                }

                // 输入区：Enter 发送 / Shift+Enter 换行（桌面惯例）
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
            }
            ErrorDialog(error = error, onDismiss = { error = null })
        }
    }
}

/** 消息气泡：用户右对齐主色容器、AI 左对齐副色容器；hover 高亮边框；文本可选中复制。 */
@Composable
private fun Bubble(message: UiMessage, showThinking: Boolean) {
    val isUser = message.role == MessageRole.USER
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
                if (showThinking && !isUser) {
                    Text(
                        "（思考中…）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SelectionContainer {
                    Text(
                        message.text.ifEmpty { "…" },
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

/** 错误弹窗：按 ApiError 分类展示解决指引（文案与手机端 AppStrings 保持一致）。 */
@Composable
private fun ErrorDialog(error: ApiError?, onDismiss: () -> Unit) {
    if (error == null) return
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
private fun SimpleDropdown(
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
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(
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
