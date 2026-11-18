package com.bamboomosaic.llmchat.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.ContextWindow
import com.bamboomosaic.llmchat.core.domain.ContextWindowRepository
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.ConversationRepository
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import com.bamboomosaic.llmchat.core.domain.TokenTotals
import com.bamboomosaic.llmchat.core.domain.TokenUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class UiMessage(
    val role: MessageRole,
    val text: String,
    val isStreaming: Boolean = false,
    val promptTokens: Long? = null,
    val completionTokens: Long? = null,
    /** 推理模型的思考内容，展示在正文之前。 */
    val reasoning: String = "",
    /** 思考内容是否折叠。 */
    val reasoningCollapsed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contextWindowRepository: ContextWindowRepository,
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,
    private val tokenUsageRepository: TokenUsageRepository,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    /** 已保存的全部 API 配置。 */
    val apiConfigs: StateFlow<List<ApiConfig>> = configRepository
        .observeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前选中的配置（聊天页左下角按钮显示其模型名）。 */
    val currentConfig: StateFlow<ApiConfig?> = configRepository
        .observeCurrentConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val conversations: StateFlow<List<Conversation>> = conversationRepository
        .observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 已归档会话，供设置里的「恢复对话」使用。 */
    val archivedConversations: StateFlow<List<Conversation>> = conversationRepository
        .observeArchivedConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前对话标题（与侧边栏联动）。 */
    val currentTitle: StateFlow<String> = combine(
        conversations,
        _currentConversationId,
    ) { list, id ->
        list.firstOrNull { it.id == id }?.title.orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** 当前会话 token 明细。 */
    val sessionTotals: StateFlow<TokenTotals> = _currentConversationId
        .flatMapLatest { tokenUsageRepository.observeSessionTotals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TokenTotals())

    /** 全应用 token 明细。 */
    val globalTotals: StateFlow<TokenTotals> = tokenUsageRepository
        .observeGlobalTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TokenTotals())

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository
        .observeSettings()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettings(
                systemPrompt = AppSettings.defaultSystemPrompt(AppLanguage.SIMPLIFIED_CHINESE),
                model = AppSettings.DEFAULT_MODEL,
                baseUrl = AppSettings.DEFAULT_BASE_URL,
                apiKey = AppSettings.DEFAULT_API_KEY,
            ),
        )

    private var streamJob: Job? = null

    init {
        // 首次启动时把升级前的单条 DataStore 配置迁移成第一条默认 API 配置
        viewModelScope.launch { configRepository.ensureSeeded() }
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        _error.value = null

        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        val history = _messages.value
            .filter { !it.isStreaming }
            .map { ChatMessage(it.role, it.text) }
        val outgoing = history + ChatMessage(MessageRole.USER, text)

        val withSystem = listOf(ChatMessage(MessageRole.SYSTEM, currentSettings.systemPrompt)) + outgoing
        val compressed = if (currentSettings.autoCompressEnabled) {
            contextWindowRepository.compress(
                withSystem,
                ContextWindow(maxTokens = currentSettings.maxContextTokens),
            )
        } else {
            withSystem
        }

        _messages.update {
            it + UiMessage(MessageRole.USER, text) + UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }

        // 流式渲染节流：正文很长时每个 chunk 都重建列表 + 重新排版会明显闪烁，
        // 这里限制到约 16fps，结束时再强制渲染一次完整内容。
        val reasoningState = MutableStateFlow("")

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            conversationRepository.ensureConversation(conversationId, text.take(MAX_TITLE_LENGTH))
            conversationRepository.updateTitleIfPlaceholder(conversationId, text.take(MAX_TITLE_LENGTH))
            conversationRepository.appendMessage(conversationId, ChatMessage(MessageRole.USER, text))

            var accumulated = ""
            var lastRenderAt = 0L

            fun render(force: Boolean) {
                val now = System.currentTimeMillis()
                if (!force && now - lastRenderAt < RENDER_INTERVAL_MS) return
                lastRenderAt = now
                val textSnapshot = accumulated
                val reasoningSnapshot = reasoningState.value
                _messages.update { list ->
                    val collapsed = list.lastOrNull()?.reasoningCollapsed ?: false
                    list.dropLast(1) + UiMessage(
                        role = MessageRole.ASSISTANT,
                        text = textSnapshot,
                        isStreaming = true,
                        reasoning = reasoningSnapshot,
                        reasoningCollapsed = collapsed,
                    )
                }
            }

            chatRepository.sendChatStream(
                sessionId = conversationId,
                messages = compressed,
                model = currentConfig.value?.model ?: currentSettings.model,
                thinkingEnabled = currentSettings.thinkingEnabled,
                maxTokens = currentSettings.maxOutputTokens.takeIf { it > 0 },
                onReasoning = { chunk ->
                    // 回调在 OkHttp 线程，StateFlow 的 update 是线程安全的
                    reasoningState.update { it + chunk }
                    render(force = false)
                },
            )
                .catch { e -> _error.value = e.message ?: "Unknown error" }
                .collect { chunk ->
                    accumulated += chunk
                    render(force = false)
                }

            // 流结束，强制渲染一次完整内容（避免节流丢掉最后一段）
            render(force = true)

            val reasoningText = reasoningState.value
            if (accumulated.isNotEmpty()) {
                conversationRepository.appendMessage(
                    conversationId,
                    ChatMessage(MessageRole.ASSISTANT, accumulated, reasoningText.takeIf { it.isNotBlank() }),
                )
            } else if (_error.value == null) {
                // 请求成功（可能消耗了 token）但没有任何正文：给出明确原因，而不是留一个空白气泡
                _error.value = emptyResponseHint(currentSettings)
            }

            // 本轮消耗：流结束后读取该会话最近一条 usage
            val usage = tokenUsageRepository.getLastUsage(conversationId)
            _messages.update { list ->
                val last = list.lastOrNull()
                list.dropLast(1) + UiMessage(
                    role = MessageRole.ASSISTANT,
                    text = accumulated,
                    promptTokens = usage?.promptTokens,
                    completionTokens = usage?.completionTokens,
                    reasoning = reasoningText,
                    reasoningCollapsed = last?.reasoningCollapsed ?: false,
                )
            }
        }
    }

    /** 折叠 / 展开某条消息的思考内容。 */
    fun toggleReasoning(index: Int) {
        _messages.update { list ->
            if (index !in list.indices) return@update list
            list.toMutableList().also { mutable ->
                val item = mutable[index]
                mutable[index] = item.copy(reasoningCollapsed = !item.reasoningCollapsed)
            }
        }
    }

    fun newConversation() {
        streamJob?.cancel()
        _error.value = null
        _currentConversationId.value = UUID.randomUUID().toString()
        _messages.value = emptyList()
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        streamJob?.cancel()
        _error.value = null
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            val loaded = conversationRepository.getMessages(conversationId)
                .map { UiMessage(it.role, it.content, reasoning = it.reasoning.orEmpty()) }
            _messages.value = loaded
        }
    }

    fun renameConversation(conversationId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { conversationRepository.renameConversation(conversationId, title) }
    }

    /** 归档会话：从侧边栏移到「恢复对话」。 */
    fun archiveConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.setArchived(conversationId, true)
            // 归档的是当前会话时，切到一个新会话，避免停留在已归档内容上
            if (conversationId == _currentConversationId.value) {
                newConversation()
            }
        }
    }

    /** 从「恢复对话」里恢复到侧边栏。 */
    fun restoreConversation(conversationId: String) {
        viewModelScope.launch { conversationRepository.setArchived(conversationId, false) }
    }

    /** 永久删除会话及其消息（不可恢复）。 */
    fun deleteConversationPermanently(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
            if (conversationId == _currentConversationId.value) {
                newConversation()
            }
        }
    }

    // ---- 多套 API 配置管理 ----

    /** 聊天页切换当前配置（仅影响后续请求）。 */
    fun selectConfig(id: Long) {
        viewModelScope.launch { configRepository.select(id) }
    }

    fun saveConfig(config: ApiConfig) {
        viewModelScope.launch { configRepository.save(config) }
    }

    fun deleteConfig(id: Long) {
        viewModelScope.launch { configRepository.delete(id) }
    }

    fun setDefaultConfig(id: Long) {
        viewModelScope.launch { configRepository.setDefault(id) }
    }

    fun seedConfigs() {
        viewModelScope.launch { configRepository.ensureSeeded() }
    }

    fun setSystemPrompt(value: String) {
        viewModelScope.launch { settingsRepository.setSystemPrompt(value) }
    }

    fun setModel(value: String) {
        viewModelScope.launch { settingsRepository.setModel(value) }
    }

    fun setBaseUrl(value: String) {
        viewModelScope.launch { settingsRepository.setBaseUrl(value) }
    }

    fun setApiKey(value: String) {
        viewModelScope.launch { settingsRepository.setApiKey(value) }
    }

    fun setProvider(providerId: String) {
        viewModelScope.launch { settingsRepository.setProvider(providerId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setThinkingEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setThinkingEnabled(enabled) }
    }

    fun setMaxContextTokens(value: Int) {
        viewModelScope.launch { settingsRepository.setMaxContextTokens(value) }
    }

    fun setMaxOutputTokens(value: Int) {
        viewModelScope.launch { settingsRepository.setMaxOutputTokens(value) }
    }

    fun setAutoCompressEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoCompressEnabled(enabled) }
    }

    /** 请求成功但没有任何正文时的提示（避免只留一个空白气泡）。 */
    private fun emptyResponseHint(settings: AppSettings): String = when (settings.language) {
        AppLanguage.SIMPLIFIED_CHINESE ->
            "模型没有返回正文内容（token 已消耗）。请确认模型名支持对话，或检查「最大输出」是否过小。"
        AppLanguage.TRADITIONAL_CHINESE ->
            "模型沒有回傳正文內容（token 已消耗）。請確認模型名稱支援對話，或檢查「最大輸出」是否過小。"
        AppLanguage.ENGLISH ->
            "The model returned no content (tokens were consumed). Check the model name supports chat, or whether Max output tokens is too small."
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 24
        const val RENDER_INTERVAL_MS = 60L
    }
}
