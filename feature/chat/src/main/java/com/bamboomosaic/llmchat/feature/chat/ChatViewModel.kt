package com.bamboomosaic.llmchat.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatRepository
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
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contextWindowRepository: ContextWindowRepository,
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,
    private val tokenUsageRepository: TokenUsageRepository,
) : ViewModel() {

    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = conversationRepository
        .observeConversations()
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

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            conversationRepository.ensureConversation(conversationId, text.take(MAX_TITLE_LENGTH))
            conversationRepository.updateTitleIfPlaceholder(conversationId, text.take(MAX_TITLE_LENGTH))
            conversationRepository.appendMessage(conversationId, ChatMessage(MessageRole.USER, text))

            var accumulated = ""
            chatRepository.sendChatStream(
                sessionId = conversationId,
                messages = compressed,
                model = currentSettings.model,
                thinkingEnabled = currentSettings.thinkingEnabled,
                maxTokens = currentSettings.maxOutputTokens.takeIf { it > 0 },
            )
                .catch { e -> _error.value = e.message ?: "Unknown error" }
                .collect { chunk ->
                    accumulated += chunk
                    _messages.update { list ->
                        list.dropLast(1) + UiMessage(MessageRole.ASSISTANT, accumulated, isStreaming = true)
                    }
                }

            if (accumulated.isNotEmpty()) {
                conversationRepository.appendMessage(
                    conversationId,
                    ChatMessage(MessageRole.ASSISTANT, accumulated),
                )
            } else if (_error.value == null) {
                // 请求成功（可能消耗了 token）但没有任何正文：给出明确原因，而不是留一个空白气泡
                _error.value = emptyResponseHint(currentSettings)
            }

            // 本轮消耗：流结束后读取该会话最近一条 usage
            val usage = tokenUsageRepository.getLastUsage(conversationId)
            _messages.update { list ->
                list.dropLast(1) + UiMessage(
                    role = MessageRole.ASSISTANT,
                    text = accumulated,
                    promptTokens = usage?.promptTokens,
                    completionTokens = usage?.completionTokens,
                )
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
                .map { UiMessage(it.role, it.content) }
            _messages.value = loaded
        }
    }

    fun renameConversation(conversationId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { conversationRepository.renameConversation(conversationId, title) }
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
    }
}
