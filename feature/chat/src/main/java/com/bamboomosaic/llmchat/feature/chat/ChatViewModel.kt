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
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.ModeConfig
import com.bamboomosaic.llmchat.core.domain.SystemPromptBuilder
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    /** 消息数据库 id（0 = 尚未持久化，如正在流式生成的消息）。 */
    val dbId: Long = 0,
    /** 版本号：同一用户提问下的第 N 版 AI 回复。 */
    val version: Int = 1,
    /** 该组最新版本号（>1 时 UI 显示版本切换标识）。 */
    val latestVersion: Int = 1,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contextWindowRepository: ContextWindowRepository,
    private val settingsRepository: SettingsRepository,
    private val contextReminderProvider: com.bamboomosaic.llmchat.core.domain.ContextReminderProvider,
    private val conversationRepository: ConversationRepository,
    private val tokenUsageRepository: TokenUsageRepository,
    private val configRepository: ConfigRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    /** 新建对话的初始标题（发送首条消息后被占位标题逻辑替换）。 */
    private suspend fun newConversationTitle() = "新对话"

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

    /** 临时会话的内存模式配置（未转正前不入库）。声明必须在 init 之前，避免初始化顺序 NPE。 */
    private val _draftModeId = MutableStateFlow(ModeConfig.CHAT)
    private val _draftUserPrompt = MutableStateFlow<String?>(null)

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
        // 启动自动恢复上次对话（无任何会话则新建「新对话」）
        viewModelScope.launch { restoreLastConversation() }
    }

    /** 恢复最近更新的会话；一个会话都没有时新建「新对话」（聊天模式）。 */
    private suspend fun restoreLastConversation() {
        val last = conversations.value.maxByOrNull { it.updatedAt }
        if (last != null) {
            _currentConversationId.value = last.id
            _messages.value = loadLatestMessages(last.id)
        } else {
            newConversation()
        }
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
            history = outgoing,
            currentSettings = currentSettings,
            userTextForTitle = text,
            userImages = images,
            persistUserMessage = true,
            modeId = effectiveModeId.value,
            // 普通发送：用户消息在本流中持久化，versionKey 取该消息 dbId
            versionKeyOf = {
                conversationRepository.getMessages(conversationId)
                    .lastOrNull { it.role == MessageRole.USER }?.id ?: 0L
            },
        )
    }

    /**
     * 重新生成最后一条助手回复：多版本保留，不覆盖旧版本。
     * versionKey = 被重新生成的助手消息所属版本组（定位不到时回退触发它的用户消息 id）。
     */
    fun regenerateLast() {
        if (_isGenerating.value) return
        _error.value = null
        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        val msgs = _messages.value
        // 找到最后一条助手消息及其版本组
        val lastAssistant = msgs.lastOrNull()?.takeIf { it.role == MessageRole.ASSISTANT } ?: return
        val lastUser = msgs.filter { it.role == MessageRole.USER }.lastOrNull() ?: return
        // 去掉尾部的助手消息（新版本将追加在原位）
        val withoutLastAssistant = if (msgs.lastOrNull()?.role == MessageRole.ASSISTANT) {
            msgs.dropLast(1)
        } else {
            msgs
        }

        _messages.update {
            withoutLastAssistant + UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }
        _isGenerating.value = true

        // 历史消息（不含最后的助手消息），用于拼请求
        val history = withoutLastAssistant
            .filter { !it.isStreaming }
            .map { ChatMessage(it.role, it.text, images = it.images) }

        launchStream(
            conversationId = conversationId,
            history = history,
            currentSettings = currentSettings,
            userTextForTitle = lastUser.text,
            persistUserMessage = false,
            modeId = effectiveModeId.value,
            // 重新生成：versionKey 复用原助手消息的版本组
            versionKeyOf = {
                val dbId = lastAssistant.dbId
                if (dbId > 0) {
                    val original = conversationRepository.getMessageById(dbId)
                    val key = original?.versionKey
                    if (key != null && key > 0) {
                        key
                    } else {
                        // 老数据（迁移前）：没有 versionKey，回退用触发它的用户消息 id
                        conversationRepository.getMessages(conversationId)
                            .lastOrNull { it.role == MessageRole.USER }?.id ?: 0L
                    }
                } else {
                    conversationRepository.getMessages(conversationId)
                        .lastOrNull { it.role == MessageRole.USER }?.id ?: 0L
                }
            },
        )
    }

    /** 长按复制消息全文（纯文本）。 */
    fun copyMessage(index: Int) {
        // 复制动作由 UI 层直接读 _messages[index].text 写剪贴板，这里无需处理
    }

    /** 编辑用户消息：回填输入框由 UI 监听 editDraft 事件完成。 */
    private val _editDraft = MutableStateFlow<String?>(null)
    val editDraft: StateFlow<String?> = _editDraft.asStateFlow()

    fun startEditMessage(index: Int) {
        val msg = _messages.value.getOrNull(index) ?: return
        if (msg.role != MessageRole.USER) return
        _editDraft.value = msg.text
    }

    fun consumeEditDraft() {
        _editDraft.value = null
    }

    /**
     * 切换某条助手消息的显示版本（1/N）：[delta] +1 下一版 / -1 上一版，循环滚动。
     */
    fun switchVersion(index: Int, delta: Int) {
        val current = _messages.value.getOrNull(index) ?: return
        if (current.latestVersion <= 1 || current.dbId <= 0) return
        viewModelScope.launch {
            val siblings = conversationRepository.getSiblingVersions(current.dbId)
            if (siblings.size <= 1) return@launch
            val versions = siblings.map { it.version }
            val curIdx = versions.indexOf(current.version).takeIf { it >= 0 } ?: 0
            val nextIdx = ((curIdx + delta) % versions.size + versions.size) % versions.size
            val target = siblings[nextIdx]
            // 切换版本的 token 消耗同步更新
            val usage = target.usageId?.let { tokenUsageRepository.getUsageById(it) }
            _messages.update { list ->
                list.toMutableList().also { mutable ->
                    mutable[index] = UiMessage(
                        role = target.role,
                        text = target.content,
                        reasoning = target.reasoning.orEmpty(),
                        searchPages = target.searchPages,
                        dbId = target.id,
                        version = target.version,
                        latestVersion = versions.maxOrNull() ?: target.version,
                        promptTokens = usage?.promptTokens,
                        completionTokens = usage?.completionTokens,
                    )
                }
            }
        }
    }

    /**
     * 编辑用户消息并重发：内容回填已由 UI 处理；这里用原文触发新一轮生成。
     * 产出的助手回复挂到新的版本组（原组保留），旧消息不入库删除。
     */
    fun editAndResend(messageIndex: Int, newText: String) {
        if (_isGenerating.value) return
        if (newText.isBlank()) return
        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        // 截断该用户消息及之后的所有 UI 消息（历史请求历史也截断到此消息之前）
        val history = _messages.value.take(messageIndex).filter { !it.isStreaming }
        _messages.value = history

        _messages.update {
            it + UiMessage(MessageRole.USER, newText) +
                UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }
        _isGenerating.value = true

        val outgoing = history.map { ChatMessage(it.role, it.text, images = it.images) } +
            ChatMessage(MessageRole.USER, newText)
        launchStream(
            conversationId = conversationId,
            history = outgoing,
            currentSettings = currentSettings,
            userTextForTitle = newText,
            persistUserMessage = true,
            modeId = effectiveModeId.value,
            versionKeyOf = {
                conversationRepository.getMessages(conversationId)
                    .lastOrNull { it.role == MessageRole.USER }?.id ?: 0L
            },
        )
    }

    /** 组装请求消息：system + 历史，可选压缩。 */
    /** 请求组装结果：消息列表 + 模式采样参数 + 用户层是否被截断。 */
    data class RequestBundle(
        val messages: List<ChatMessage>,
        val temperature: Float?,
        val topP: Float?,
        val truncated: Boolean,
    )

    /**
     * 双层系统提示词组装：按当前会话绑定的模式 + 用户提示词拼接（SystemPromptBuilder），
     * temperature/top_p 从会话模式配置读取，最后按最大上下文压缩。
     */
    private suspend fun buildRequestBundle(
        currentSettings: AppSettings,
        history: List<ChatMessage>,
    ): RequestBundle {
        val conversationId = _currentConversationId.value
        // 直接查库：会话列表 Flow 可能有刷新延迟（新建后立刻发送的场景）
        val conversation = conversationRepository.getConversationById(conversationId)
        val mode = ChatModes.find(conversation?.modeId ?: ModeConfig.CUSTOM, currentSettings.language)
        // 自定义模式参数：会话冗余值优先，否则用模式默认
        val temperature = conversation?.effectiveTemperature
            ?: (if (mode.modeId == ModeConfig.CUSTOM) mode.temperature else mode.temperature)
        val topP = conversation?.effectiveTopP ?: mode.topP
        // 截断上限 = 最大上下文的 30%（字符数近似）
        val maxChars = (currentSettings.maxContextTokens * 30 / 100).coerceAtLeast(500)
        val sp = SystemPromptBuilder.build(mode, conversation?.userPrompt, maxChars = maxChars, language = currentSettings.language)
        // 环境上下文注入：ds_web 强制开启；其他模式跟随「自动注入环境信息」开关
        val shouldInject = mode.injectContext || currentSettings.contextInjectionEnabled
        val systemText = if (shouldInject) {
            sp.prompt + "\n\n[当前环境] " +
                contextReminderProvider.buildReminder(currentSettings.language)
        } else {
            sp.prompt
        }
        val withSystem = listOf(ChatMessage(MessageRole.SYSTEM, systemText)) + history
        // 省 Token 档位：质量/中等/最大 → 压缩阈值（所有模式生效）
        val compressLimitK = when (currentSettings.tokenSavingLevel) {
            1 -> 100_000
            2 -> 50_000
            3 -> 20_000
            else -> 0
        }
        // 档位启用时无视全局自动压缩开关，按阈值压缩
        val compressed = if (compressLimitK > 0 || currentSettings.autoCompressEnabled) {
            contextWindowRepository.compress(
                withSystem,
                ContextWindow(
                    maxTokens = if (compressLimitK > 0) compressLimitK else currentSettings.maxContextTokens,
                ),
            )
        } else {
            withSystem
        }
        return RequestBundle(compressed, temperature, topP, sp.userLayerTruncated)
    }

    /** 发起流式请求的核心逻辑（普通发送与重新生成共用）。 */
    private fun launchStream(
        conversationId: String,
        history: List<ChatMessage>,
        currentSettings: AppSettings,
        userTextForTitle: String,
        userImages: List<String> = emptyList(),
        persistUserMessage: Boolean,
        /** 当前会话模式 ID（省 Token 档位豁免判断用）。 */
        modeId: String = ModeConfig.CUSTOM,
        /** 多版本上下文：触发本次回复的用户消息 dbId（0 = 普通新消息，持久化后再取 id）。 */
        versionKeyOf: suspend () -> Long = { 0L },
    ) {
        viewModelScope.launch {
            val bundle = buildRequestBundle(currentSettings, history)
            if (bundle.truncated) {
                _error.value = null
                _truncatedToast.tryEmit(Unit)
            }
            streamInternal(
                conversationId, bundle.messages, currentSettings, userTextForTitle,
                userImages, persistUserMessage, bundle.temperature, bundle.topP, modeId, versionKeyOf,
            )
        }
    }

    /** 截断轻提示事件。 */
    private val _truncatedToast = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val truncatedToast: SharedFlow<Unit> = _truncatedToast.asSharedFlow()

    private fun streamInternal(
        conversationId: String,
        compressed: List<ChatMessage>,
        currentSettings: AppSettings,
        userTextForTitle: String,
        userImages: List<String> = emptyList(),
        persistUserMessage: Boolean,
        temperature: Float?,
        topP: Float?,
        modeId: String = ModeConfig.CUSTOM,
        versionKeyOf: suspend () -> Long = { 0L },
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
                // 临时空白会话发消息即转正入库（用户层为空也转正——规则 7）
                if (conversations.value.none { it.id == conversationId }) {
                    conversationRepository.ensureConversationWithMode(
                        conversationId = conversationId,
                        title = "新对话",
                        modeId = _draftModeId.value,
                        userPrompt = _draftUserPrompt.value,
                        temperature = null,
                        topP = null,
                    )
                }
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
                maxTokens = effectiveMaxOutputTokens(currentSettings, modeId),
                onReasoning = { chunk ->
                    // 回调在 OkHttp 线程，StateFlow 的 update 是线程安全的
                    reasoningState.update { it + chunk }
                    render(force = false)
                },
                searchEnabled = currentSettings.searchEnabled,
                temperature = temperature,
                topP = topP,
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
            var savedReplyId = 0L
            var savedVersion = 1
            if (accumulated.isNotEmpty()) {
                val vk = versionKeyOf()
                // usageId 在 data 层写入消息时自动关联最近一条用量记录
                val saved = conversationRepository.appendVersionedReply(
                    conversationId,
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = accumulated,
                        reasoning = reasoningText.takeIf { it.isNotBlank() },
                        searchPages = searchPagesState.value,
                    ),
                    versionKey = vk,
                )
                savedReplyId = saved.id
                savedVersion = saved.version
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
                    dbId = savedReplyId,
                    version = savedVersion,
                    latestVersion = maxOf(savedVersion, last?.latestVersion ?: 1),
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

    /**
     * 新建对话：生成一条「临时空白对话」，仅存在于内存（不入库、侧边栏不显示）。
     * 转正条件（唯一）：用户层提示词有有效内容（补充提示词 / 角色卡输出），或用户发送了消息。
     * 内存临时状态：模式 ID、用户提示词暂存，转正时一并写入。
     */
    fun newConversation() {
        streamJob?.cancel()
        _error.value = null
        _currentConversationId.value = UUID.randomUUID().toString()
        _messages.value = emptyList()
        // 临时会话内存配置：默认模式设置非 off 用之，否则聊天模式；用户层为空
        val defaultMode = settings.value.defaultModeId
        _draftModeId.value = if (defaultMode != "off") defaultMode else ModeConfig.CHAT
        _draftUserPrompt.value = null
    }

    /** 当前会话生效的模式 ID：正式会话读库，临时会话读内存草稿。 */
    val effectiveModeId: StateFlow<String> = combine(
        conversations,
        _currentConversationId,
        _draftModeId,
    ) { list, id, draft ->
        list.firstOrNull { it.id == id }?.modeId ?: draft
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModeConfig.CHAT)

    /** 当前会话生效的用户提示词：正式会话读库，临时会话读内存草稿。 */
    val effectiveUserPrompt: StateFlow<String?> = combine(
        conversations,
        _currentConversationId,
        _draftUserPrompt,
    ) { list, id, draft ->
        list.firstOrNull { it.id == id }?.userPrompt ?: draft
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 当前会话是否为临时空白（未入库）。 */
    val isDraftConversation: StateFlow<Boolean> = combine(
        conversations,
        _currentConversationId,
    ) { list, id ->
        list.firstOrNull { it.id == id } == null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * 转正临时会话（唯一入库点）：用户层提示词有内容时调用。
     * 写库 + 清空草稿；用户层为空则什么都不做（保持临时态）。
     */
    private suspend fun promoteDraftIfValid(
        modeId: String,
        userPrompt: String?,
        temperature: Float?,
        topP: Float?,
    ) {
        if (userPrompt.isNullOrBlank()) return
        val id = _currentConversationId.value
        // 已是正式会话则更新配置，否则入库转正
        if (conversations.value.any { it.id == id }) {
            conversationRepository.updateConversationMode(id, modeId, userPrompt, temperature, topP)
        } else {
            conversationRepository.ensureConversationWithMode(id, "新对话", modeId, userPrompt, temperature, topP)
        }
        _draftModeId.value = modeId
        _draftUserPrompt.value = userPrompt
    }

    /** 当前会话是否已锁定模式（发送过消息）。 */
    val currentConversationLocked: Boolean
        get() = _messages.value.any { it.role == MessageRole.ASSISTANT && it.dbId > 0 }

    /**
     * 创建带模式配置的会话并设为当前。
     * [userPrompt] 缓存到模式模板记忆（下次新建同模式自动回填）。
     */
    fun createConversationWithMode(modeId: String, userPrompt: String?, temperature: Float? = null, topP: Float? = null) {
        val mode = ChatModes.find(modeId, settings.value.language)
        val userPromptTrimmed = userPrompt?.trim()?.takeIf { it.isNotBlank() }
        // 模式绑定模型：当前配置不含绑定模型时，自动切换到含该模型的配置
        mode.bindModel?.let { bind ->
            val match = apiConfigs.value.firstOrNull { it.model == bind }
            if (match != null && currentConfig.value?.model != bind) {
                selectConfig(match.id)
            }
        }
        // 草稿记录（临时会话内存态）；仅当用户层有内容时转正入库
        _draftModeId.value = modeId
        _draftUserPrompt.value = userPromptTrimmed
        viewModelScope.launch {
            promoteDraftIfValid(modeId, userPromptTrimmed, temperature, topP)
        }
        saveModeUserPrompt(modeId, userPromptTrimmed)
        if (modeId == ModeConfig.CUSTOM && temperature != null && topP != null) {
            saveModeCustomParams(modeId, temperature, topP)
        }
    }

    /** 更新当前会话的模式配置（仅未发送消息时可调）。 */
    fun updateConversationMode(modeId: String, userPrompt: String?, temperature: Float? = null, topP: Float? = null) {
        if (currentConversationLocked) return
        val userPromptTrimmed = userPrompt?.trim()?.takeIf { it.isNotBlank() }
        // 更新内存草稿（临时会话态）
        _draftModeId.value = modeId
        _draftUserPrompt.value = userPromptTrimmed
        viewModelScope.launch {
            // 有用户层内容才转正/更新入库；为空保持临时态
            if (userPromptTrimmed != null) {
                conversationRepository.ensureConversationWithMode(
                    conversationId = _currentConversationId.value,
                    title = "新对话",
                    modeId = modeId,
                    userPrompt = userPromptTrimmed,
                    temperature = temperature,
                    topP = topP,
                )
                conversationRepository.updateConversationMode(
                    conversationId = _currentConversationId.value,
                    modeId = modeId,
                    userPrompt = userPromptTrimmed,
                    temperature = temperature,
                    topP = topP,
                )
            }
        }
        saveModeUserPrompt(modeId, userPromptTrimmed)
        if (modeId == ModeConfig.CUSTOM && temperature != null && topP != null) {
            saveModeCustomParams(modeId, temperature, topP)
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        streamJob?.cancel()
        _error.value = null
        // 切走临时会话：草稿直接丢弃（未转正的不留任何数据）
        _draftModeId.value = ModeConfig.CHAT
        _draftUserPrompt.value = null
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            _messages.value = loadLatestMessages(conversationId)
        }
    }

    /**
     * 加载会话消息并只保留每组的最新版本：
     * 普通消息（versionKey=null）直接保留；助手多版本组只留 version 最大的一条。
     */
    private suspend fun loadLatestMessages(conversationId: String): List<UiMessage> {
        val all = conversationRepository.getMessages(conversationId)
        val latestById = mutableMapOf<Long, ChatMessage>() // versionKey -> 最新版
        val result = mutableListOf<UiMessage>()
        for (m in all) {
            val key = m.versionKey
            if (key == null) {
                result += m.toUi()
            } else {
                val cur = latestById[key]
                if (cur == null || m.version > cur.version) latestById[key] = m
            }
        }
        // 按原始顺序回填多版本组的最新版
        val out = mutableListOf<UiMessage>()
        var lastKey: Long? = null
        for (m in all) {
            if (m.versionKey == null) {
                out += m.toUi()
                lastKey = null
            } else if (m.versionKey != lastKey) {
                lastKey = m.versionKey
                val latest = latestById[m.versionKey]
                if (latest != null) {
                    val siblings = conversationRepository.getSiblingVersions(latest.id)
                    val maxV = siblings.maxOfOrNull { it.version } ?: latest.version
                    out += latest.toUi(latestVersion = maxV)
                }
            }
        }
        return out
    }

    private fun ChatMessage.toUi(latestVersion: Int = version): UiMessage = UiMessage(
        role = role,
        text = content,
        reasoning = reasoning.orEmpty(),
        images = images,
        searchPages = searchPages,
        dbId = id,
        version = version,
        latestVersion = latestVersion,
    )

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
            // 导出只包含每组最新版本的回复（不导出历史版本）
            val all = conversationRepository.getMessages(conversationId)
            val latestByKey = all.filter { it.versionKey != null }
                .groupBy { it.versionKey }
                .mapValues { (_, group) -> group.maxByOrNull { it.version }!! }
            val messages = all.filter { it.versionKey == null || latestByKey[it.versionKey]?.id == it.id }
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

    fun setDefaultModeId(modeId: String) {
        viewModelScope.launch { settingsRepository.setDefaultModeId(modeId) }
    }

    fun setContextInjectionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setContextInjectionEnabled(enabled) }
    }

    fun setTokenSavingLevel(level: Int) {
        viewModelScope.launch { settingsRepository.setTokenSavingLevel(level) }
    }

    /**
     * 实际生效的单次输出上限。
     * 省 Token 档位：1=质量 16384 / 2=中等 8192 / 3=最大 4096；
     * 豁免模式（代码开发 / 创意写作 / Legacy）不受档位限制，回退到全局配置值。
     * 0 或豁免时用 maxOutputTokens（0 = 不限制）。
     */
    private fun effectiveMaxOutputTokens(settings: AppSettings, modeId: String): Int? {
        val exempt = modeId == ModeConfig.CODE ||
            modeId == ModeConfig.CREATIVE ||
            modeId == ModeConfig.LEGACY
        if (!exempt) {
            val cap = when (settings.tokenSavingLevel) {
                1 -> 16384
                2 -> 8192
                3 -> 4096
                else -> 0
            }
            if (cap > 0) {
                // 档位上限与用户全局上限取较小值（都设置时以更严格者为准）
                val userCap = settings.maxOutputTokens.takeIf { it > 0 }
                return minOf(cap, userCap ?: cap)
            }
        }
        return settings.maxOutputTokens.takeIf { it > 0 }
    }

    /** 模式模板记忆：保存/清除某模式的用户提示词缓存。 */
    fun saveModeUserPrompt(modeId: String, prompt: String?) {
        viewModelScope.launch { settingsRepository.setModeUserPrompt(modeId, prompt) }
    }

    fun observeModeUserPrompt(modeId: String) = settingsRepository.observeModeUserPrompt(modeId)

    fun observeModeCustomParams(modeId: String) = settingsRepository.observeModeCustomParams(modeId)

    fun saveModeCustomParams(modeId: String, temperature: Float, topP: Float) {
        viewModelScope.launch { settingsRepository.setModeCustomParams(modeId, temperature, topP) }
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
