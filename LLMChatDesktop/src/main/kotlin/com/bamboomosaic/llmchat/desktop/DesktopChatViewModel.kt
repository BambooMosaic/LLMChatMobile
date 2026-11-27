package com.bamboomosaic.llmchat.desktop

import com.bamboomosaic.llmchat.core.domain.ApiError
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.ContextReminderProvider
import com.bamboomosaic.llmchat.core.domain.ContextWindow
import com.bamboomosaic.llmchat.core.domain.ContextWindowRepository
import com.bamboomosaic.llmchat.core.domain.ConversationRepository
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.core.domain.ModeConfig
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import com.bamboomosaic.llmchat.core.domain.SystemPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 桌面端全局状态容器（桌面端第 2 轮；非 Hilt，手动构造）。
 * 覆盖：会话列表 / 当前会话消息（持久化）/ 发送与流式渲染 / 设置 / API 配置。
 * 请求组装与手机端同构：custom 模式双层 system + 环境快照注入当前轮用户消息 + 省 Token 档位压缩。
 * 与手机端的差异（本轮裁剪）：会话固定自定义模式；AI 回复完整落库（不做逐字写库与多版本）。
 */
class DesktopChatViewModel(
    val settingsRepository: SettingsRepository,
    val configRepository: ConfigRepository,
    val conversationRepository: ConversationRepository,
    val chatRepository: ChatRepository,
    val contextWindowRepository: ContextWindowRepository,
    val contextReminderProvider: ContextReminderProvider,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = settingsRepository.observeSettings()
        .stateIn(scope, SharingStarted.Eagerly, emptySettings())
    val configs = configRepository.observeConfigs()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val currentConfig = configRepository.observeCurrentConfig()
        .stateIn(scope, SharingStarted.Eagerly, null)
    val conversations = conversationRepository.observeConversations()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _currentId = MutableStateFlow<String?>(null)
    val currentId = _currentId.asStateFlow()

    val messages = _currentId.filterNotNull()
        .flatMapLatest { conversationRepository.observeMessages(it) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** 流式中的 AI 气泡临时文本（回复完整后落库并清空）。 */
    val streamingText = MutableStateFlow("")
    val busy = MutableStateFlow(false)
    val thinking = MutableStateFlow(false)
    val error = MutableStateFlow<ApiError?>(null)

    init {
        // 首次启动把 DataStore 旧配置迁移为第一条默认配置（与手机端 ensureSeeded 行为一致）
        scope.launch { configRepository.ensureSeeded() }
    }

    private fun emptySettings(): AppSettings = AppSettings(
        systemPrompt = "", model = "", baseUrl = "", apiKey = "",
    )

    /** 新建临时会话（首条消息发出时才落库，与手机端语义一致）。 */
    fun newConversation() {
        _currentId.value = UUID.randomUUID().toString()
        streamingText.value = ""
    }

    fun selectConversation(id: String) {
        _currentId.value = id
        streamingText.value = ""
    }

    fun deleteConversation(id: String) {
        scope.launch {
            conversationRepository.deleteConversation(id)
            if (_currentId.value == id) _currentId.value = null
        }
    }

    fun renameConversation(id: String, title: String) {
        if (title.isBlank()) return
        scope.launch { conversationRepository.renameConversation(id, title.trim()) }
    }

    fun togglePinned(id: String, pinned: Boolean) {
        scope.launch { conversationRepository.setPinned(id, pinned) }
    }

    fun send(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || busy.value) return
        val config = currentConfig.value
        if (config == null || config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            error.value = ApiError(ApiError.Kind.UNKNOWN, "还没有可用的 API 配置，请先在「API 配置」里新增并填写密钥")
            return
        }
        val conversationId = _currentId.value ?: UUID.randomUUID().toString()
            .also { _currentId.value = it }

        busy.value = true
        thinking.value = false
        scope.launch {
            try {
                // 手机端同款时序：会话转正 → 标题 → 用户消息落库 → 组请求 → 流式 → 回复落库
                conversationRepository.ensureConversation(conversationId, text.take(24))
                conversationRepository.updateTitleIfPlaceholder(conversationId, text.take(24))
                conversationRepository.appendMessage(conversationId, ChatMessage(MessageRole.USER, text))

                val current = settings.value
                // 双层 system：桌面端本轮固定自定义模式（模式选择后续轮次）
                val mode = ChatModes.find(ModeConfig.CUSTOM, current.language)
                val systemText = SystemPromptBuilder.build(
                    mode, userPrompt = null, language = current.language,
                ).prompt
                // 环境快照注入当前轮用户消息开头（32 轮方案）
                val reminder = contextReminderProvider.buildReminder(current.language)
                val history = conversationRepository.getMessages(conversationId)
                val lastUserIdx = history.indexOfLast { it.role == MessageRole.USER }
                val payload = listOf(ChatMessage(MessageRole.SYSTEM, systemText)) +
                    history.toMutableList().also { list ->
                        list[lastUserIdx] = list[lastUserIdx].copy(
                            content = "[当前环境] $reminder\n\n${list[lastUserIdx].content}",
                        )
                    }

                // 省 Token 档位压缩（手机端同款档位阈值；桌面端无豁免模式）
                val savingLevel = current.tokenSavingLevel
                val compressLimit = when (savingLevel) {
                    1 -> 120_000; 2 -> 50_000; 3 -> 30_000; 4 -> 10_000; else -> 0
                }
                val compressed = when {
                    compressLimit > 0 -> contextWindowRepository.compress(
                        payload, ContextWindow(maxTokens = compressLimit),
                    )
                    current.autoCompressEnabled -> contextWindowRepository.compress(
                        payload, ContextWindow(maxTokens = current.maxContextTokens),
                    )
                    else -> payload
                }

                streamingText.value = ""
                chatRepository.sendChatStream(
                    sessionId = conversationId,
                    messages = compressed,
                    model = config.model.ifBlank { current.model },
                    thinkingEnabled = current.thinkingEnabled,
                    temperature = mode.temperature,
                    topP = mode.topP,
                ).collect { chunk ->
                    streamingText.value += chunk
                }
                // 完整回复落库（本轮不做逐字写库）
                val full = streamingText.value
                if (full.isNotBlank()) {
                    conversationRepository.appendMessage(
                        conversationId, ChatMessage(MessageRole.ASSISTANT, full),
                    )
                } else {
                    error.value = ApiError(ApiError.Kind.UNKNOWN, "模型返回了空回复")
                }
                streamingText.value = ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = ApiError.from(e.message)
                streamingText.value = ""
            } finally {
                busy.value = false
            }
        }
    }
}
