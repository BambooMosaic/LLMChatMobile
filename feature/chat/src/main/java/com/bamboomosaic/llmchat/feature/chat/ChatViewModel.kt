package com.bamboomosaic.llmchat.feature.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.DailyTokenStats
import com.bamboomosaic.llmchat.core.domain.HourlyTokenStats
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    /** 多模态图片 base64（仅用户消息）。 */
    val images: List<String> = emptyList(),
    /** 联网搜索到的网页累计数。null = 本轮未开启搜索；0 = 已发起尚未有结果。 */
    val searchPages: Int? = null,
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
    @ApplicationContext private val appContext: Context,
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

    /** 是否首次启动（欢迎弹窗）。 */
    val isFirstLaunch: StateFlow<Boolean> = settingsRepository
        .observeFirstLaunch()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setFirstLaunchDone() {
        viewModelScope.launch { settingsRepository.setFirstLaunchDone() }
    }

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

    /** Token 统计图表周期（天数）：1 或 7。 */
    private val _tokenStatsDays = MutableStateFlow(7)
    val tokenStatsDays: StateFlow<Int> = _tokenStatsDays.asStateFlow()

    fun setTokenStatsDays(days: Int) {
        _tokenStatsDays.value = days
    }

    /** 按当前周期聚合的每日 token 消耗（供近 7 天柱状图）。 */
    val dailyTokenStats: StateFlow<List<DailyTokenStats>> = _tokenStatsDays
        .flatMapLatest { days ->
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.add(java.util.Calendar.DAY_OF_MONTH, -(days - 1))
            tokenUsageRepository.observeDailyTotals(cal.timeInMillis)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 今天的 3 小时分段 token 消耗（供近 1 天柱状图）。 */
    val hourlyTokenStats: StateFlow<List<HourlyTokenStats>> = tokenUsageRepository
        .let { repo ->
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            repo.observeHourlyTotals(cal.timeInMillis)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /** 是否正在生成（控制发送按钮变「停止」）。 */
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        // 首次启动时把升级前的单条 DataStore 配置迁移成第一条默认 API 配置
        viewModelScope.launch { configRepository.ensureSeeded() }
    }

    /** 中断当前流式生成：取消协程，把已生成内容固化到消息里。 */
    fun stopGeneration() {
        val job = streamJob ?: return
        job.cancel()
        streamJob = null
        _isGenerating.value = false
        // 把当前"流式中"的助手消息转为已完成（保留已生成的部分）
        _messages.update { list ->
            val last = list.lastOrNull() ?: return@update list
            if (!last.isStreaming) return@update list
            list.dropLast(1) + last.copy(isStreaming = false, text = last.text)
        }
        // 已生成的部分也要入库
        val conversationId = _currentConversationId.value
        val lastMsg = _messages.value.lastOrNull()
        if (lastMsg != null && lastMsg.role == MessageRole.ASSISTANT && lastMsg.text.isNotEmpty()) {
            viewModelScope.launch {
                conversationRepository.appendMessage(
                    conversationId,
                    ChatMessage(MessageRole.ASSISTANT, lastMsg.text, lastMsg.reasoning.takeIf { it.isNotBlank() }),
                )
            }
        }
    }

    /** 待发送的图片（base64），发送后清空。 */
    private val _pendingImages = MutableStateFlow<List<String>>(emptyList())
    val pendingImages: StateFlow<List<String>> = _pendingImages.asStateFlow()

    fun setPendingImages(images: List<String>) {
        _pendingImages.value = images
    }

    fun clearPendingImages() {
        _pendingImages.value = emptyList()
    }

    fun sendUserMessage(text: String) {
        val images = _pendingImages.value
        if (text.isBlank() && images.isEmpty()) return
        _error.value = null

        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        val history = _messages.value
            .filter { !it.isStreaming }
            .map { ChatMessage(it.role, it.text, images = it.images) }
        val outgoing = history + ChatMessage(MessageRole.USER, text, images = images)

        _messages.update {
            it + UiMessage(MessageRole.USER, text, images = images) +
                UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }
        _isGenerating.value = true
        _pendingImages.value = emptyList()

        launchStream(
            conversationId = conversationId,
            compressed = buildRequestMessages(currentSettings, outgoing),
            currentSettings = currentSettings,
            userTextForTitle = text,
            userImages = images,
            persistUserMessage = true,
        )
    }

    /** 重新生成最后一条助手回复：复用上一条用户消息重新请求。 */
    fun regenerateLast() {
        if (_isGenerating.value) return
        _error.value = null
        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        val msgs = _messages.value
        // 找到最后一条用户消息（去掉最后的助手消息）
        val lastUser = msgs.filter { it.role == MessageRole.USER }.lastOrNull() ?: return
        // 去掉最后一条助手消息（将被覆盖）
        val withoutLastAssistant = if (msgs.lastOrNull()?.role == MessageRole.ASSISTANT) {
            msgs.dropLast(1)
        } else {
            msgs
        }

        _messages.update {
            withoutLastAssistant + UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }
        _isGenerating.value = true

        // 历史消息（不含被覆盖的助手消息），用于拼请求
        val history = withoutLastAssistant
            .filter { !it.isStreaming }
            .map { ChatMessage(it.role, it.text, images = it.images) }

        launchStream(
            conversationId = conversationId,
            compressed = buildRequestMessages(currentSettings, history),
            currentSettings = currentSettings,
            userTextForTitle = lastUser.text,
            persistUserMessage = false,
        )
    }

    /** 组装请求消息：system + 历史，可选压缩。 */
    private fun buildRequestMessages(
        currentSettings: AppSettings,
        history: List<ChatMessage>,
    ): List<ChatMessage> {
        // 会话独立提示词优先，否则用全局默认
        val conversationId = _currentConversationId.value
        val customPrompt = conversations.value
            .firstOrNull { it.id == conversationId }
            ?.systemPrompt
        val systemPrompt = customPrompt ?: currentSettings.systemPrompt
        val withSystem = listOf(ChatMessage(MessageRole.SYSTEM, systemPrompt)) + history
        return if (currentSettings.autoCompressEnabled) {
            contextWindowRepository.compress(
                withSystem,
                ContextWindow(maxTokens = currentSettings.maxContextTokens),
            )
        } else {
            withSystem
        }
    }

    /** 发起流式请求的核心逻辑（普通发送与重新生成共用）。 */
    private fun launchStream(
        conversationId: String,
        compressed: List<ChatMessage>,
        currentSettings: AppSettings,
        userTextForTitle: String,
        userImages: List<String> = emptyList(),
        persistUserMessage: Boolean,
    ) {
        // 流式渲染节流：正文很长时每个 chunk 都重建列表 + 重新排版会明显闪烁，
        // 这里限制到约 16fps，结束时再强制渲染一次完整内容。
        val reasoningState = MutableStateFlow("")
        // 联网搜索状态：null = 本轮未开启；0 = 已发起未出结果；N = 累计网页数
        val searchPagesState = MutableStateFlow<Int?>(if (currentSettings.searchEnabled) 0 else null)

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            conversationRepository.ensureConversation(conversationId, userTextForTitle.take(MAX_TITLE_LENGTH))
            conversationRepository.updateTitleIfPlaceholder(conversationId, userTextForTitle.take(MAX_TITLE_LENGTH))
            if (persistUserMessage) {
                conversationRepository.appendMessage(
                    conversationId,
                    ChatMessage(MessageRole.USER, userTextForTitle, images = userImages),
                )
            }

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
                        searchPages = searchPagesState.value,
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
                searchEnabled = currentSettings.searchEnabled,
                onSearchPages = { count ->
                    // SearchStarted 回调 0（保持累计值不变），SearchCompleted 回调本次网页数（累加）
                    searchPagesState.update { (it ?: 0) + count }
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
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = accumulated,
                        reasoning = reasoningText.takeIf { it.isNotBlank() },
                        searchPages = searchPagesState.value,
                    ),
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
                    searchPages = searchPagesState.value,
                )
            }
            _isGenerating.value = false
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
                .map { UiMessage(it.role, it.content, reasoning = it.reasoning.orEmpty(), images = it.images) }
            _messages.value = loaded
        }
    }

    fun renameConversation(conversationId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { conversationRepository.renameConversation(conversationId, title) }
    }

    /** 设置会话独立系统提示词（空串回退全局默认）。 */
    fun setConversationSystemPrompt(conversationId: String, prompt: String) {
        viewModelScope.launch {
            conversationRepository.setSystemPrompt(conversationId, prompt)
        }
    }

    /** 导出会话（markdown=true 为 Markdown，否则纯文本）。成功后在 exportMessage 回传路径。 */
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    fun exportConversation(markdown: Boolean) {
        exportConversationBy(_currentConversationId.value, markdown)
    }

    fun exportConversationBy(conversationId: String, markdown: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = conversations.value.firstOrNull { it.id == conversationId }
            val title = conversation?.title?.ifBlank { "conversation" } ?: "conversation"
            val messages = conversationRepository.getMessages(conversationId)
            val content = buildExportContent(title, messages, markdown)
            val ext = if (markdown) "md" else "txt"
            val fileName = "${sanitizeFileName(title)}_${System.currentTimeMillis()}.$ext"
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS,
            )
            val file = java.io.File(dir, fileName)
            runCatching {
                file.writeText(content)
            }.onSuccess {
                _exportMessage.value = "$exportSuccessLabel: ${file.absolutePath}"
            }.onFailure { e ->
                _exportMessage.value = e.message ?: "export failed"
            }
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    private val exportSuccessLabel: String
        get() = when (settings.value.language) {
            AppLanguage.SIMPLIFIED_CHINESE -> "已导出到"
            AppLanguage.TRADITIONAL_CHINESE -> "已匯出到"
            AppLanguage.ENGLISH -> "Exported to"
        }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(50)

    private fun buildExportContent(title: String, messages: List<ChatMessage>, markdown: Boolean): String {
        val sb = StringBuilder()
        if (markdown) {
            sb.appendLine("# $title")
            sb.appendLine()
        } else {
            sb.appendLine(title)
            sb.appendLine()
        }
        messages.forEach { msg ->
            when (msg.role) {
                MessageRole.SYSTEM -> {
                    sb.appendLine(if (markdown) "### System" else "[System]")
                    sb.appendLine(msg.content)
                }
                MessageRole.USER -> {
                    sb.appendLine(if (markdown) "### User" else "[User]")
                    // 图片以占位符标注，不导出二进制内容
                    if (msg.images.isNotEmpty()) {
                        msg.images.forEach { sb.appendLine(if (markdown) "![图片]" else "[图片]") }
                    }
                    sb.appendLine(msg.content)
                }
                MessageRole.ASSISTANT -> {
                    sb.appendLine(if (markdown) "### Assistant" else "[Assistant]")
                    if (!msg.reasoning.isNullOrBlank()) {
                        sb.appendLine(if (markdown) "> 思考：${msg.reasoning}" else "思考：${msg.reasoning}")
                    }
                    sb.appendLine(msg.content)
                }
            }
            sb.appendLine()
        }
        return sb.toString()
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

    fun setSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSearchEnabled(enabled) }
    }

    /** 设置页「验证配置」：用表单里的配置发测试请求，不入库不计统计（与引导页一致）。 */
    suspend fun validateConfigRaw(config: ApiConfig): Result<Unit> = chatRepository.validateConfig(config)

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
