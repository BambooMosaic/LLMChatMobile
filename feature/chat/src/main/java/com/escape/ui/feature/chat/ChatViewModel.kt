package com.escape.ui.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.escape.ui.core.domain.AppSettings
import com.escape.ui.core.domain.ChatMessage
import com.escape.ui.core.domain.ChatRepository
import com.escape.ui.core.domain.ContextWindow
import com.escape.ui.core.domain.ContextWindowRepository
import com.escape.ui.core.domain.Conversation
import com.escape.ui.core.domain.ConversationRepository
import com.escape.ui.core.domain.MessageRole
import com.escape.ui.core.domain.SettingsRepository
import com.escape.ui.core.domain.TokenUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class UiMessage(val role: MessageRole, val text: String, val isStreaming: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contextWindowRepository: ContextWindowRepository,
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,
    tokenUsageRepository: TokenUsageRepository,
) : ViewModel() {

    /** 当前会话 ID；新建会话时是一个尚未入库的 UUID，首次发送时写入。 */
    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    /** 侧边栏会话列表（按最近更新倒序）。 */
    val conversations: StateFlow<List<Conversation>> = conversationRepository
        .observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前会话累计 token，随 currentConversationId 切换。 */
    val sessionTotalTokens: StateFlow<Long> = _currentConversationId
        .flatMapLatest { tokenUsageRepository.observeSessionTotal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val contextWindow: StateFlow<ContextWindow> = contextWindowRepository
        .observeWindow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ContextWindow())

    val settings: StateFlow<AppSettings> = settingsRepository
        .observeSettings()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettings("", AppSettings.DEFAULT_MODEL, AppSettings.DEFAULT_BASE_URL, AppSettings.DEFAULT_API_KEY),
        )

    /** 正在进行的流式任务，切换会话时取消。 */
    private var streamJob: Job? = null

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        _error.value = null

        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        // 发送前基于已完成的展示消息推导上下文（不含正在流式的占位）
        val history = _messages.value
            .filter { !it.isStreaming }
            .map { ChatMessage(it.role, it.text) }
        val outgoing = history + ChatMessage(MessageRole.USER, text)

        val withSystem = listOf(ChatMessage(MessageRole.SYSTEM, currentSettings.systemPrompt)) + outgoing
        val compressed = contextWindowRepository.compress(withSystem, contextWindow.value)

        _messages.update {
            it + UiMessage(MessageRole.USER, text) + UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            // 首条消息时确保会话入库，并用首条消息生成标题
            conversationRepository.ensureConversation(conversationId, text.take(MAX_TITLE_LENGTH))
            conversationRepository.updateTitleIfPlaceholder(conversationId, text.take(MAX_TITLE_LENGTH))
            conversationRepository.appendMessage(conversationId, ChatMessage(MessageRole.USER, text))

            var accumulated = ""
            chatRepository.sendChatStream(conversationId, compressed, currentSettings.model)
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
            }
            _messages.update { list ->
                list.dropLast(1) + UiMessage(MessageRole.ASSISTANT, accumulated)
            }
        }
    }

    /** 新建会话：切换到新的 UUID，清空当前展示。 */
    fun newConversation() {
        streamJob?.cancel()
        _error.value = null
        _currentConversationId.value = UUID.randomUUID().toString()
        _messages.value = emptyList()
    }

    /** 切换到某个历史会话，从 Room 加载消息。 */
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

    private companion object {
        const val MAX_TITLE_LENGTH = 24
    }
}
