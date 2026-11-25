package com.bamboomosaic.llmchat.feature.chat

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.DailyTokenStats
import com.bamboomosaic.llmchat.core.domain.HourlyTokenStats
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.ApiError
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
import kotlinx.coroutines.withContext
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
    private val ttsClient: com.bamboomosaic.llmchat.core.network.DeepSeekTtsClient,
    private val qwenTtsClient: com.bamboomosaic.llmchat.core.network.QwenTtsClient,
    private val glmTtsClient: com.bamboomosaic.llmchat.core.network.GlmTtsClient,
    private val backupManager: com.bamboomosaic.llmchat.core.data.backup.BackupManager,
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

    private val _error = MutableStateFlow<ApiError?>(null)
    val error: StateFlow<ApiError?> = _error.asStateFlow()

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
            _messages.value = loadActivePath(last.id)
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
        // 21 轮：终极档只能向省钱模式对话发送消息
        if (settings.value.tokenSavingLevel == 4 && effectiveModeId.value != ModeConfig.SAVING) {
            _error.value = ApiError(ApiError.Kind.UNKNOWN, ultimateTierHint(settings.value))
            return
        }
        // 21 轮：终极档输入上限 1024 字符
        val outgoingText = if (settings.value.tokenSavingLevel == 4) text.take(1024) else text

        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        val history = _messages.value
            .filter { !it.isStreaming }
            .map { ChatMessage(it.role, it.text, images = it.images) }
        val outgoing = history + ChatMessage(MessageRole.USER, outgoingText, images = images)

        _messages.update {
            it + UiMessage(MessageRole.USER, outgoingText, images = images) +
                UiMessage(MessageRole.ASSISTANT, "", isStreaming = true)
        }
        _isGenerating.value = true
        _pendingImages.value = emptyList()

        launchStream(
            conversationId = conversationId,
            history = outgoing,
            currentSettings = currentSettings,
            userTextForTitle = outgoingText,
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
     * 19 轮：还原导入的模式 JSON 携带的对话记录。
     * 写入当前（新）会话；临时会话先按模式转正。消息顺序与导出一致，时间戳为导入时刻。
     */
    fun importConversationMessages(modeId: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        viewModelScope.launch {
            val id = _currentConversationId.value
            if (conversations.value.none { it.id == id }) {
                conversationRepository.ensureConversationWithMode(
                    conversationId = id,
                    title = "新对话",
                    modeId = modeId,
                    userPrompt = _draftUserPrompt.value,
                    temperature = null,
                    topP = null,
                )
            }
            messages.forEach { conversationRepository.appendMessage(id, it) }
            _messages.value = loadActivePath(id)
        }
    }

    /**
     * 重新生成最后一条助手回复：多版本保留，不覆盖旧版本。
     * versionKey = 被重新生成的助手消息所属版本组（定位不到时回退触发它的用户消息 id）。
     */
    fun regenerateLast() {
        if (_isGenerating.value) return
        _error.value = null
        // 21 轮：终极档只能向省钱模式对话发送消息
        if (settings.value.tokenSavingLevel == 4 && effectiveModeId.value != ModeConfig.SAVING) {
            _error.value = ApiError(ApiError.Kind.UNKNOWN, ultimateTierHint(settings.value))
            return
        }
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
            // 27 轮：分支切换 = 记录激活选择 + 整条路径重建（每个版本各自延伸独立子链）
            val key = target.versionKey ?: return@launch
            branchChoice[key] = target.id
            rebuildActivePath(index to target)
        }
    }

    /**
     * 编辑用户消息并重发：内容回填已由 UI 处理；这里用原文触发新一轮生成。
     * 产出的助手回复挂到新的版本组（原组保留），旧消息不入库删除。
     */
    fun editAndResend(messageIndex: Int, newText: String) {
        if (_isGenerating.value) return
        if (newText.isBlank()) return
        viewModelScope.launch {
        val conversationId = _currentConversationId.value
        val currentSettings = settings.value

        // 27 轮：新用户消息挂到被编辑用户消息的父下（兄弟分支，上下文隔离）
        val editedDbId = _messages.value.getOrNull(messageIndex)?.dbId ?: 0L
        val editedParent = if (editedDbId > 0) {
            withContext(Dispatchers.IO) { conversationRepository.getMessageById(editedDbId)?.parentId }
        } else {
            null
        }

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
            userParentIdOverride = editedParent,
            versionKeyOf = {
                conversationRepository.getMessages(conversationId)
                    .lastOrNull { it.role == MessageRole.USER }?.id ?: 0L
            },
        )
        }
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
        /** 19 轮：搜索代理返回的带引用结果（非空时注入 system）。 */
        searchDigest: String? = null,
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
        // 18 轮修复：必须把括号规则开关传入拼装管道，否则参数默认 true，开关关了也不生效
            val sp = SystemPromptBuilder.build(
                mode, conversation?.userPrompt, maxChars = maxChars,
                language = currentSettings.language,
                roleplayParenRule = currentSettings.roleplayParenRule,
                historySummary = conversation?.systemPrompt,
            )
        // 环境上下文注入：ds_web 强制开启；其他模式跟随「自动注入环境信息」开关；
        // 21 轮：终极档一律不注入环境信息
        val shouldInject = (mode.injectContext || currentSettings.contextInjectionEnabled) &&
            currentSettings.tokenSavingLevel != 4
        // 19 轮：用户称呼注入——DS 网页版提示词固定单句，不注入称呼
        val nickname = if (mode.modeId == ModeConfig.DS_WEB) "" else currentSettings.userNickname
        val systemText = buildString {
            append(sp.prompt)
            searchDigest?.takeIf { it.isNotBlank() }?.let { digest ->
                append("\n\n")
                append(
                    when (currentSettings.language) {
                        AppLanguage.TRADITIONAL_CHINESE -> "[聯網搜索結果（DeepSeek 代理）]"
                        AppLanguage.ENGLISH -> "[Web Search Results (via DeepSeek)]"
                        else -> "[联网搜索结果（DeepSeek 代理）]"
                    },
                )
                append('\n').append(digest)
            }
            if (shouldInject) {
                append("\n\n[当前环境] ")
                append(contextReminderProvider.buildReminder(currentSettings.language, nickname))
            }
        }
        val withSystem = listOf(ChatMessage(MessageRole.SYSTEM, systemText)) + history
        // 21 轮：省 Token 档位压缩阈值（1→120k 2→50k 3→30k 4→10k）。
        // DS 网页版 / Legacy 豁免档位压缩（终极档除外，强制 10k）；未开档位时跟随全局自动压缩开关。
        val isTierExemptMode = mode.modeId == ModeConfig.DS_WEB || mode.modeId == ModeConfig.LEGACY
        val savingLevel = currentSettings.tokenSavingLevel
        val tierApplies = savingLevel in 1..4 && !(isTierExemptMode && savingLevel != 4)
        val compressLimitK = if (tierApplies) when (savingLevel) {
            1 -> 120_000
            2 -> 50_000
            3 -> 30_000
            else -> 10_000
        } else 0
        val compressed = if (compressLimitK > 0) {
            contextWindowRepository.compress(withSystem, ContextWindow(maxTokens = compressLimitK))
        } else if (currentSettings.autoCompressEnabled) {
            contextWindowRepository.compress(withSystem, ContextWindow(maxTokens = currentSettings.maxContextTokens))
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
        /** 27 轮：用户消息父消息 id 覆盖（编辑重发=被编辑用户消息的父；null=自动取激活链尾）。 */
        userParentIdOverride: Long? = null,
    ) {
        // 19 轮：请求组装挪进 streamInternal（需在会话转正之后执行搜索代理前置阶段）
        streamInternal(
            conversationId, history, currentSettings, userTextForTitle,
            userImages, persistUserMessage, modeId, versionKeyOf, userParentIdOverride,
        )
    }

    /** 截断轻提示事件。 */
    private val _truncatedToast = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val truncatedToast: SharedFlow<Unit> = _truncatedToast.asSharedFlow()

    private fun streamInternal(
        conversationId: String,
        history: List<ChatMessage>,
        currentSettings: AppSettings,
        userTextForTitle: String,
        userImages: List<String> = emptyList(),
        persistUserMessage: Boolean,
        modeId: String = ModeConfig.CUSTOM,
        versionKeyOf: suspend () -> Long = { 0L },
        userParentIdOverride: Long? = null,
    ) {
        // 流式渲染节流：正文很长时每个 chunk 都重建列表 + 重新排版会明显闪烁，
        // 这里限制到约 16fps，结束时再强制渲染一次完整内容。
        val reasoningState = MutableStateFlow("")
        // 联网搜索状态：null = 本轮未开启；0 = 已发起未出结果；N = 累计网页数
        // 19 轮：初始 null，搜索前置阶段结束后按路径写入
        val searchPagesState = MutableStateFlow<Int?>(null)

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            if (persistUserMessage && conversations.value.none { it.id == conversationId }) {
                // 临时空白会话发消息即转正入库（用户层为空也转正——规则 7）。
                // 18 轮修复：必须先带草稿模式建行。原先先 ensureConversation 用实体默认值（custom）建行，
                // 随后 ensureConversationWithMode 对已存在行直接忽略，导致灰色模式重启后漂移成自定义模式。
                conversationRepository.ensureConversationWithMode(
                    conversationId = conversationId,
                    title = "新对话",
                    modeId = _draftModeId.value,
                    userPrompt = _draftUserPrompt.value,
                    temperature = null,
                    topP = null,
                )
            }
            conversationRepository.ensureConversation(conversationId, userTextForTitle.take(MAX_TITLE_LENGTH))
            conversationRepository.updateTitleIfPlaceholder(conversationId, userTextForTitle.take(MAX_TITLE_LENGTH))
            if (persistUserMessage) {
                // 27 轮：分叉树——用户消息父 = 编辑重发指定父，否则当前激活链尾
                val userParent = userParentIdOverride
                    ?: _messages.value.lastOrNull { !it.isStreaming && it.dbId > 0 }?.dbId
                drafts[conversationId] = ""
                settingsRepository.setDraft(conversationId, "")
                conversationRepository.appendMessage(
                    conversationId,
                    ChatMessage(
                        MessageRole.USER,
                        userTextForTitle,
                        images = userImages,
                        parentId = userParent?.takeIf { it > 0 },
                    ),
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

            // —— 19 轮：DeepSeek-Flash 搜索代理前置阶段 ——
            // 路由：当前模型是 DeepSeek → 原生搜索（SSE 事件累加，零改动）；
            // 非 DeepSeek + 搜索开 → 先用已保存的 DeepSeek 配置搜索一次，结果注入 system。
            // 失败降级：不阻断对话，本轮不带搜索结果，经 _error 提示。
            var searchDigest: String? = null
            val cfg = currentConfig.value
            val isDsModel = cfg != null && (
                cfg.provider.equals("deepseek", ignoreCase = true) ||
                    cfg.baseUrl.contains("deepseek", ignoreCase = true)
                )
            if (currentSettings.searchEnabled && !isDsModel) {
                // 先置 0，让消息气泡在搜索期间显示「正在搜索」
                searchPagesState.value = 0
                render(force = true)
                val dsConfig = apiConfigs.value.firstOrNull {
                    it.provider.equals("deepseek", ignoreCase = true)
                }
                if (dsConfig == null) {
                    _error.value = ApiError(ApiError.Kind.UNKNOWN, searchUnavailableHint(currentSettings))
                } else {
                    val question = history.lastOrNull { it.role == MessageRole.USER }?.content
                        ?.takeIf { it.isNotBlank() } ?: userTextForTitle
                    try {
                        var pages = 0
                        val digest = chatRepository.searchProxy(
                            config = dsConfig,
                            question = question,
                            sessionId = conversationId,
                            onPages = { p -> pages += p },
                        )
                        if (digest.isNotBlank()) searchDigest = digest
                        searchPagesState.value = pages
                    } catch (e: Exception) {
                        _error.value = ApiError(ApiError.Kind.UNKNOWN, searchFailedHint(currentSettings))
                    }
                }
                render(force = true)
            } else if (currentSettings.searchEnabled) {
                searchPagesState.value = 0
            }

            val bundle = buildRequestBundle(currentSettings, history, searchDigest)
            if (bundle.truncated) {
                _error.value = null
                _truncatedToast.tryEmit(Unit)
            }

            // 28 轮：流式错误捕获（供重试循环判断）
            var streamError: Throwable? = null
            chatRepository.sendChatStream(
                sessionId = conversationId,
                messages = bundle.messages,
                model = currentConfig.value?.model ?: currentSettings.model,
                // 22 轮：传入生效用户提示词——省钱模式据此把「最多输出 N 字符」变成硬性 max_tokens
                maxTokens = effectiveMaxOutputTokens(
                    currentSettings,
                    modeId,
                    conversations.value.firstOrNull { it.id == conversationId }?.userPrompt,
                ),
                onReasoning = { chunk ->
                    // 回调在 OkHttp 线程，StateFlow 的 update 是线程安全的
                    reasoningState.update { it + chunk }
                    render(force = false)
                },
                searchEnabled = currentSettings.searchEnabled && !tierDisablesFeatures(currentSettings, modeId),
                thinkingEnabled = currentSettings.thinkingEnabled && !tierDisablesFeatures(currentSettings, modeId),
                temperature = bundle.temperature,
                topP = bundle.topP,
                onSearchPages = { count ->
                    // SearchStarted 回调 0（保持累计值不变），SearchCompleted 回调本次网页数（累加）
                    searchPagesState.update { (it ?: 0) + count }
                    render(force = false)
                },
            )
                .catch { e -> streamError = e }
                .collect { chunk ->
                    accumulated += chunk
                    render(force = false)
                }

            // 28 轮：网络 / 429 自动退避重试——失败且可重试（IOException / 限流 / 超时）时从头重发
            if (streamError != null) {
                val cfgAtRetry = currentConfig.value
                val maxRetry = currentSettings.netRetryCount
                var attempt = 0
                while (true) {
                    attempt++
                    val err = streamError
                    val apiErr = ApiError.from(err?.message)
                    val retryable = err is java.io.IOException ||
                        apiErr.kind == ApiError.Kind.RATE_LIMIT ||
                        apiErr.kind == ApiError.Kind.TIMEOUT
                    if (!retryable || attempt > maxRetry) {
                        _error.value = apiErr
                        break
                    }
                    _retryState.value = RetryState(attempt, maxRetry)
                    // 退避：3s → 6s → 9s（封顶 9s）
                    kotlinx.coroutines.delay((3000L * attempt).coerceAtMost(9000L))
                    if (_currentConversationId.value != conversationId) break // 期间切走了会话
                    // 重置已渲染内容，从头重发
                    accumulated = ""
                    reasoningState.value = ""
                    render(force = true)
                    streamError = null
                    // 28 轮：流式错误捕获（供重试循环判断）
            chatRepository.sendChatStream(
                        sessionId = conversationId,
                        messages = bundle.messages,
                        model = currentConfig.value?.model ?: currentSettings.model,
                        maxTokens = effectiveMaxOutputTokens(
                            currentSettings,
                            modeId,
                            conversations.value.firstOrNull { it.id == conversationId }?.userPrompt,
                        ),
                        onReasoning = { chunk ->
                            reasoningState.update { it + chunk }
                            render(force = false)
                        },
                        searchEnabled = currentSettings.searchEnabled && !tierDisablesFeatures(currentSettings, modeId),
                        thinkingEnabled = currentSettings.thinkingEnabled && !tierDisablesFeatures(currentSettings, modeId),
                        temperature = bundle.temperature,
                        topP = bundle.topP,
                        onSearchPages = { count ->
                            searchPagesState.update { (it ?: 0) + count }
                            render(force = false)
                        },
                    )
                        .catch { e -> streamError = e }
                        .collect { chunk ->
                            accumulated += chunk
                            render(force = false)
                        }
                    if (streamError == null) break
                }
            }
            _retryState.value = null

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
                        parentId = vk.takeIf { it > 0 }, // 27 轮：父 = 触发的用户消息
                    ),
                    versionKey = vk,
                )
                savedReplyId = saved.id
                savedVersion = saved.version
                // 27 轮修正：自动朗读（默认关）——回复落库后自动朗读本条
                if (currentSettings.ttsAutoRead && ttsEngineReady()) {
                    toggleTts(saved.id, accumulated)
                }
            } else if (_error.value == null) {
                // 请求成功（可能消耗了 token）但没有任何正文：给出明确原因，而不是留一个空白气泡
                _error.value = ApiError(ApiError.Kind.UNKNOWN, emptyResponseHint(currentSettings))
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

            // 24 轮：首轮回复落库后自动生成简短标题（用户手动改名过的会话跳过；失败保留原标题不再重试）
            // 25 轮：省 Token 最大（3）/ 终极（4）档位禁用自动标题
            if (savedReplyId > 0 && currentSettings.tokenSavingLevel < 3 &&
                conversations.value.firstOrNull { it.id == conversationId }?.titleAuto == true
            ) {
                generateAutoTitle(conversationId, userTextForTitle, accumulated)
            }
        }
    }

    /** 24 轮：正在生成标题的会话集合（防重复触发）。 */
    private val autoTitling = mutableSetOf<String>()

    /**
     * 24 轮：用当前模型为对话生成 ≤12 字标题。
     * 成功 → setAutoTitle（titleAuto 置 0）；失败 → 保留现有标题并同样关闭自动标记（避免每轮重试）。
     * 标题轮次会计入 Token 统计（真实消耗），输出上限 32 token，成本可忽略。
     */
    private fun generateAutoTitle(conversationId: String, userText: String, replyText: String) {
        if (!autoTitling.add(conversationId)) return
        viewModelScope.launch {
            try {
                val titlePrompt = "给下面的对话起一个简短标题：不超过12个字，只输出标题本身，" +
                    "不要引号、句号或任何解释。\n用户：${userText.take(80)}\nAI：${replyText.take(120)}"
                val title = StringBuilder()
                chatRepository.sendChatStream(
                    sessionId = conversationId,
                    messages = listOf(ChatMessage(MessageRole.USER, titlePrompt)),
                    model = currentConfig.value?.model ?: settings.value.model,
                    maxTokens = 32,
                    thinkingEnabled = false,
                    temperature = 0.4f,
                ).collect { chunk -> title.append(chunk) }
                val cleaned = title.toString().trim()
                    .trim('"', '“', '”', '「', '」', '。', '.', '！', '？')
                    .take(24)
                if (cleaned.isNotBlank()) {
                    conversationRepository.setAutoTitle(conversationId, cleaned)
                } else {
                    conversationRepository.setAutoTitle(conversationId, userText.take(12))
                }
            } catch (_: Exception) {
                // 失败降级：保留现有标题（首条用户消息截断），不再自动重试
                runCatching { conversationRepository.setAutoTitle(conversationId, userText.take(12)) }
            } finally {
                autoTitling.remove(conversationId)
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

    /** 25 轮：已有消息的会话 id 集合（侧边栏「编辑提示词」入口显隐）。 */
    val conversationsWithMessages: StateFlow<Set<String>> =
        conversationRepository.observeConversationIdsWithMessages()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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

    /** 28 轮：会话草稿（运行期内存 + 当前会话持久化到 DataStore，冷启动恢复）。 */
    private val drafts = mutableMapOf<String, String>()

    private val _draftRestore = MutableStateFlow<String?>(null)
    val draftRestore: StateFlow<String?> = _draftRestore.asStateFlow()

    fun consumeDraftRestore() {
        _draftRestore.value = null
    }

    init {
        // 28 轮：冷启动恢复上次会话的草稿
        viewModelScope.launch {
            val id = _currentConversationId.value
            if (!drafts.containsKey(id)) {
                drafts[id] = settingsRepository.getDraft(id)
                if (drafts[id]?.isNotEmpty() == true) _draftRestore.value = drafts[id]
            }
        }
    }

    private var draftSaveJob: kotlinx.coroutines.Job? = null

    /** 输入变化时同步草稿（内存 + 500ms 防抖写盘——大退/回收后仍可恢复）。 */
    fun updateDraft(text: String) {
        val id = _currentConversationId.value
        drafts[id] = text
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            settingsRepository.setDraft(id, drafts[id] ?: text)
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        streamJob?.cancel()
        _error.value = null
        // 切走临时会话：草稿直接丢弃（未转正的不留任何数据）
        _draftModeId.value = ModeConfig.CHAT
        _draftUserPrompt.value = null
        val oldId = _currentConversationId.value
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            // 28 轮：持久化切走会话的草稿；恢复目标会话草稿
            settingsRepository.setDraft(oldId, drafts[oldId] ?: "")
            if (!drafts.containsKey(conversationId)) {
                drafts[conversationId] = settingsRepository.getDraft(conversationId)
            }
            _messages.value = loadActivePath(conversationId)
            _draftRestore.value = drafts[conversationId] ?: ""
        }
    }

    /** 分支激活选择：key = 用户分支组的父消息 id / 助手版本组的 versionKey；value = 选中消息 id。 */
    private val branchChoice = mutableMapOf<Long, Long>()

    /**
     * 27 轮：加载会话的「激活路径」——消息分叉树。
     * - 旧数据（parentId 全 null）退化为原线性逻辑（versionKey 组取最新）；
     * - 新数据按树走：用户分支组（编辑重发兄弟）与助手版本组（重新生成）各层选激活分支，
     *   分支间上下文完全隔离（激活路径 = 请求历史）；Token 统计仍按全会话累计（usage 表不动）。
     */
    private suspend fun loadActivePath(conversationId: String): List<UiMessage> {
        val all = conversationRepository.getMessages(conversationId)
        if (all.none { it.parentId != null }) return legacyLinearPath(all)
        val children = all.filter { it.parentId != null }.groupBy { it.parentId!! }
        val latestByKey = HashMap<Long, ChatMessage>()
        all.forEach { m ->
            val k = m.versionKey ?: return@forEach
            val cur = latestByKey[k]
            if (cur == null || m.version > cur.version) latestByKey[k] = m
        }
        val out = mutableListOf<UiMessage>()
        var enteredTree = false
        var lastKey: Long? = null
        for (m in all) {
            if (m.parentId != null) continue
            if (enteredTree) break // 进入树子链后，主线剩余部分（分叉前老链）不再追加
            if (m.role == MessageRole.USER) {
                out += m.toUi()
                lastKey = null
                val kids = children[m.id]
                if (!kids.isNullOrEmpty()) {
                    // 该用户消息下挂有树节点（新回复组 / 编辑分支）→ 进入树
                    out += replyGroupFrom(m.id, all, children)
                    enteredTree = true
                }
            } else {
                val k = m.versionKey
                if (k == null) {
                    out += m.toUi()
                    lastKey = null
                } else if (k != lastKey) {
                    lastKey = k
                    val chosen = branchChoice[k]?.let { id -> all.find { it.id == id } }
                        ?: latestByKey[k]
                        ?: continue
                    val groupSize = all.count { it.versionKey == k }
                    out += chosen.toUi(latestVersion = groupSize)
                    val userKids = children[chosen.id]?.filter { it.role == MessageRole.USER }
                    if (!userKids.isNullOrEmpty()) {
                        out += userBranchFrom(chosen.id, all, children)
                        enteredTree = true
                    }
                }
            }
        }
        return out
    }

    /** 旧数据线性退化：versionKey 组取最新（26 轮前的行为）。 */
    private fun legacyLinearPath(all: List<ChatMessage>): List<UiMessage> {
        val latestById = mutableMapOf<Long, ChatMessage>()
        for (m in all) {
            val k = m.versionKey ?: continue
            val cur = latestById[k]
            if (cur == null || m.version > cur.version) latestById[k] = m
        }
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
                    val maxV = all.count { it.versionKey == m.versionKey }
                    out += latest.toUi(latestVersion = maxV)
                }
            }
        }
        return out
    }

    /** 树递归：用户消息下的回复组（全量按 versionKey，激活选择生效）→ 回复下的用户分支 → 递归。 */
    private fun replyGroupFrom(
        userId: Long,
        all: List<ChatMessage>,
        children: Map<Long, List<ChatMessage>>,
    ): List<UiMessage> {
        val replies = all.filter { it.role == MessageRole.ASSISTANT && it.versionKey == userId }
        if (replies.isEmpty()) return emptyList()
        val chosen = branchChoice[userId]?.let { id -> replies.find { it.id == id } }
            ?: replies.maxByOrNull { it.version }
            ?: replies.first()
        val maxV = replies.maxOfOrNull { it.version } ?: chosen.version
        val out = mutableListOf(chosen.toUi(latestVersion = maxV))
        out += userBranchFrom(chosen.id, all, children)
        return out
    }

    /** 树递归：某消息下的用户兄弟分支组（编辑重发分叉），激活分支继续递归其回复组。 */
    private fun userBranchFrom(
        parentId: Long,
        all: List<ChatMessage>,
        children: Map<Long, List<ChatMessage>>,
    ): List<UiMessage> {
        val users = children[parentId]?.filter { it.role == MessageRole.USER } ?: return emptyList()
        if (users.isEmpty()) return emptyList()
        val activeUser = branchChoice[parentId]?.let { id -> users.find { it.id == id } }
            ?: users.maxByOrNull { it.id }
            ?: users.first()
        val out = mutableListOf(activeUser.toUi())
        out += replyGroupFrom(activeUser.id, all, children)
        return out
    }

    /** 重建激活路径并刷新 UI；[withUsage] = (目标消息, 数据) 用于切换后补 token 行显示。 */
    private suspend fun rebuildActivePath(withUsage: Pair<Int, ChatMessage>? = null) {
        val list = loadActivePath(_currentConversationId.value)
        val final = if (withUsage != null) {
            val (index, msg) = withUsage
            val i = list.indexOfFirst { it.dbId == msg.id }
            if (i >= 0) {
                val usage = msg.usageId?.let { tokenUsageRepository.getUsageById(it) }
                list.toMutableList().also {
                    it[i] = it[i].copy(
                        promptTokens = usage?.promptTokens,
                        completionTokens = usage?.completionTokens,
                    )
                }
            } else {
                list
            }
        } else {
            list
        }
        _messages.value = final
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

    /** 导出会话（format: "md" / "txt" / "json"）。成功后在 exportMessage 回传路径。 */
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    fun exportConversation(format: String) {
        exportConversationBy(_currentConversationId.value, format)
    }

    fun exportConversationBy(conversationId: String, format: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = conversations.value.firstOrNull { it.id == conversationId }
            val title = conversation?.title?.ifBlank { "conversation" } ?: "conversation"
            // 导出只包含每组最新版本的回复（不导出历史版本）
            val all = conversationRepository.getMessages(conversationId)
            val latestByKey = all.filter { it.versionKey != null }
                .groupBy { it.versionKey }
                .mapValues { (_, group) -> group.maxByOrNull { it.version }!! }
            val messages = all.filter { it.versionKey == null || latestByKey[it.versionKey]?.id == it.id }
            val content = when (format) {
                // 22 轮：会话导出 JSON（与模式导出同一格式，导入端通用）
                "json" -> buildConversationJson(conversation, messages)
                else -> buildExportContent(title, messages, markdown = format == "md")
            }
            val fileName = "${sanitizeFileName(title)}_${System.currentTimeMillis()}.$format"
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

    /**
     * 22 轮：会话导出 JSON——复用模式导出（app/llmchat-mode v2）的载荷结构，
     * 字段与「灰色模式按钮 → 导出」完全一致，导入端无需区分来源。
     */
    private fun buildConversationJson(
        conversation: com.bamboomosaic.llmchat.core.domain.Conversation?,
        messages: List<ChatMessage>,
    ): String {
        val modeId = conversation?.modeId ?: "custom"
        val modeName = com.bamboomosaic.llmchat.core.domain.ChatModes.find(
            modeId, settings.value.language,
        ).localized(settings.value.language).modeName
        val obj = org.json.JSONObject()
        obj.put("app", "llmchat-mode")
        obj.put("version", 2)
        obj.put("modeId", modeId)
        obj.put("modeName", modeName)
        obj.put("userPrompt", conversation?.userPrompt?.trim().orEmpty())
        obj.put("roleplayParenRule", settings.value.roleplayParenRule)
        obj.put(
            "conversation",
            org.json.JSONArray().apply {
                messages.forEach { m ->
                    put(
                        org.json.JSONObject()
                            .put("role", m.role.name)
                            .put("text", m.content)
                            .putOpt("reasoning", m.reasoning?.takeIf { it.isNotBlank() }),
                    )
                }
            },
        )
        return obj.toString(2)
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
            drafts.remove(conversationId)
            settingsRepository.setDraft(conversationId, "")
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

    /** 19 轮：用户称呼（其他设置页，留空不注入）。 */
    fun setUserNickname(value: String) {
        viewModelScope.launch { settingsRepository.setUserNickname(value) }
    }

    // ===== 27 轮：语音朗读（DeepSeek TTS）=====

    /** 朗读状态：空闲 / 合成中 / 播放中（携带消息 dbId，用于按钮态）。 */
    sealed interface TtsState {
        data object Idle : TtsState
        data class Loading(val messageId: Long) : TtsState
        data class Playing(val messageId: Long) : TtsState
    }

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    /** 合成失败计数（UI 收集后弹 toast）。 */
    private val _ttsErrorTick = MutableStateFlow(0)
    val ttsErrorTick: StateFlow<Int> = _ttsErrorTick.asStateFlow()

    // ===== 28 轮：网络 / 429 自动重试 =====

    /** 重试状态（非空 = 正在重试）：当前第 attempt 次 / 上限 max。UI 显示横幅。 */
    data class RetryState(val attempt: Int, val max: Int)

    private val _retryState = MutableStateFlow<RetryState?>(null)
    val retryState: StateFlow<RetryState?> = _retryState.asStateFlow()

    /** 最近一次失败的具体原因（null=无；UI 优先展示它，否则用通用文案）。 */
    private val _ttsErrorMessage = MutableStateFlow<String?>(null)
    val ttsErrorMessage: StateFlow<String?> = _ttsErrorMessage.asStateFlow()

    private var audioTrack: android.media.AudioTrack? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var ttsWavFile: java.io.File? = null
    private var ttsSeq = 0

    override fun onCleared() {
        stopTts()
        super.onCleared()
    } // 防竞态：停止/新请求后，旧的在途响应直接丢弃

    /** 点击消息行喇叭：未在播=合成并播放；正在合成/播放同一条=停止。 */
    fun toggleTts(messageId: Long, text: String) {
        android.util.Log.d("LLMChatTTS", "toggleTts id=$messageId len=${text.length}")
        when (val st = _ttsState.value) {
            is TtsState.Loading -> if (st.messageId == messageId) return stopTts()
            is TtsState.Playing -> if (st.messageId == messageId) return stopTts()
            TtsState.Idle -> {}
        }
        stopTts()
        // 29 轮：按引擎取客户端/key/音色/截断——qwen、glm 的 key 来自对应供应商的已保存配置
        val st = settings.value
        val client: com.bamboomosaic.llmchat.core.network.TtsClient
        val key: String
        val voice: String
        val clipLimit: Int
        when (st.ttsProvider) {
            "qwen" -> {
                val cfg = apiConfigs.value.firstOrNull { isQwenConfig(it) && it.apiKey.isNotBlank() }
                if (cfg == null) {
                    return notifyTtsMissing("请先在设置中添加通义千问 API 配置并填写 Key，才能使用千问朗读")
                }
                client = qwenTtsClient
                key = cfg.apiKey
                voice = st.ttsVoiceQwen
                clipLimit = QWEN_TTS_CLIP
            }
            "glm" -> {
                val cfg = apiConfigs.value.firstOrNull { isGlmConfig(it) && it.apiKey.isNotBlank() }
                if (cfg == null) {
                    return notifyTtsMissing("请先在设置中添加智谱 GLM API 配置并填写 Key，才能使用 GLM 朗读")
                }
                client = glmTtsClient
                key = cfg.apiKey
                voice = st.ttsVoiceGlm
                clipLimit = GLM_TTS_CLIP
            }
            else -> {
                val cfg = currentConfig.value
                if (cfg == null || !isDeepSeekConfig(cfg)) {
                    return notifyTtsMissing("请先在设置中选择 DeepSeek API 配置，才能使用 DeepSeek 朗读")
                }
                client = ttsClient
                key = cfg.apiKey
                voice = st.ttsVoice
                clipLimit = DEEPSEEK_TTS_CLIP
            }
        }
        val clipped = text.take(clipLimit)
        val seq = ++ttsSeq
        _ttsState.value = TtsState.Loading(messageId)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                client.synthesize(key, clipped, voice)
            }
            if (seq != ttsSeq) return@launch
            result.fold(
                onSuccess = { audio ->
                    when (audio) {
                        is com.bamboomosaic.llmchat.core.network.TtsAudio.Pcm ->
                            playPcm(messageId, audio.bytes, seq)
                        is com.bamboomosaic.llmchat.core.network.TtsAudio.Wav ->
                            playWav(messageId, audio.bytes, seq)
                    }
                },
                onFailure = {
                    android.util.Log.d("LLMChatTTS", "tts failed: ${it.message}")
                    if (seq == ttsSeq) {
                        _ttsState.value = TtsState.Idle
                        _ttsErrorTick.value++
                    }
                },
            )
        }
    }

    /**
     * 27 轮修正：用 AudioTrack 直接播 PCM（24kHz 16bit mono）。
     * 之前 MediaPlayer 播流式 WAV 因长度头不标准 prepare 失败（status=0x1）。
     */
    private fun playPcm(messageId: Long, pcm: ByteArray, seq: Int) {
        stopTtsLocked()
        val sampleRate = 24000
        val minBuf = android.media.AudioTrack.getMinBufferSize(
            sampleRate,
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = android.media.AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(android.media.AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size))
            .build()
        try {
            val written = track.write(pcm, 0, pcm.size)
            check(written == pcm.size) { "AudioTrack 写入不完整: $written/${pcm.size}" }
            // 播完标记：帧数 = 16bit mono 字节数 / 2
            track.setNotificationMarkerPosition(pcm.size / 2)
            track.setPlaybackPositionUpdateListener(
                object : android.media.AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: android.media.AudioTrack?) {
                        println("[LLMChatTTS] playback completed")
                        if (_ttsState.value is TtsState.Playing) stopTts()
                    }

                    override fun onPeriodicNotification(t: android.media.AudioTrack?) = Unit
                },
            )
            track.play()
            audioTrack = track
            println("[LLMChatTTS] playback started: ${pcm.size} bytes pcm")
            if (seq == ttsSeq) _ttsState.value = TtsState.Playing(messageId)
        } catch (e: Exception) {
            println("[LLMChatTTS] playback FAILED: " + e.javaClass.simpleName + ": " + e.message)
            runCatching { track.release() }
            _ttsErrorMessage.value = e.message
            _ttsState.value = TtsState.Idle
            _ttsErrorTick.value++
        }
    }

    /** 29 轮：TTS 引擎配置缺失时的明确提示（复用朗读失败 toast）。 */
    private fun notifyTtsMissing(hint: String) {
        _ttsErrorMessage.value = hint
        _ttsErrorTick.value++
    }

    /** 29 轮：当前 TTS 引擎是否就绪（对应供应商配置存在且 key 非空）——自动朗读前置条件。 */
    private fun ttsEngineReady(): Boolean = when (settings.value.ttsProvider) {
        "qwen" -> apiConfigs.value.any { isQwenConfig(it) && it.apiKey.isNotBlank() }
        "glm" -> apiConfigs.value.any { isGlmConfig(it) && it.apiKey.isNotBlank() }
        else -> isDeepSeekConfig(currentConfig.value)
    }

    /**
     * 29 轮：播放标准 WAV（Qwen/GLM）——写 cacheDir 临时文件交 MediaPlayer，
     * ttsSeq 防竞态；播完自动释放并清理临时文件。
     */
    private fun playWav(messageId: Long, wav: ByteArray, seq: Int) {
        stopTtsLocked()
        try {
            val file = java.io.File(appContext.cacheDir, "llmchat_tts.wav")
            file.writeBytes(wav)
            val player = android.media.MediaPlayer()
            player.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                println("[LLMChatTTS] wav playback completed")
                runCatching { it.release() }
                runCatching { file.delete() }
                if (_ttsState.value is TtsState.Playing) stopTts()
            }
            player.setOnErrorListener { mp, what, extra ->
                println("[LLMChatTTS] wav playback FAILED: what=$what extra=$extra")
                runCatching { mp.release() }
                runCatching { file.delete() }
                if (seq == ttsSeq) {
                    _ttsErrorMessage.value = "音频播放失败"
                    _ttsState.value = TtsState.Idle
                    _ttsErrorTick.value++
                }
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
            ttsWavFile = file
            println("[LLMChatTTS] wav playback started: ${wav.size} bytes")
            if (seq == ttsSeq) _ttsState.value = TtsState.Playing(messageId)
        } catch (e: Exception) {
            println("[LLMChatTTS] wav playback FAILED: " + e.javaClass.simpleName + ": " + e.message)
            _ttsErrorMessage.value = e.message
            _ttsState.value = TtsState.Idle
            _ttsErrorTick.value++
        }
    }

    /** 停止朗读并释放播放器（后台播完自动停；切换会话不主动打断）。 */
    fun stopTts() {
        ttsSeq++
        stopTtsLocked()
    }

    private fun stopTtsLocked() {
        audioTrack?.let { track ->
            runCatching { track.stop() }
            runCatching { track.release() }
        }
        audioTrack = null
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
        runCatching { ttsWavFile?.delete() }
        ttsWavFile = null
        if (_ttsState.value != TtsState.Idle) _ttsState.value = TtsState.Idle
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTtsEnabled(enabled) }
    }

    fun setTtsVoice(voiceId: String) {
        viewModelScope.launch { settingsRepository.setTtsVoice(voiceId) }
    }

    /** 29 轮：TTS 朗读引擎切换（deepseek / qwen / glm）。 */
    fun setTtsProvider(providerId: String) {
        viewModelScope.launch { settingsRepository.setTtsProvider(providerId) }
    }

    /** 29 轮：通义千问音色。 */
    fun setTtsVoiceQwen(voiceId: String) {
        viewModelScope.launch { settingsRepository.setTtsVoiceQwen(voiceId) }
    }

    /** 29 轮：智谱 GLM 音色。 */
    fun setTtsVoiceGlm(voiceId: String) {
        viewModelScope.launch { settingsRepository.setTtsVoiceGlm(voiceId) }
    }

    /** 27 轮修正：自动朗读开关。 */
    fun setTtsAutoRead(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTtsAutoRead(enabled) }
    }

    /** 28 轮：网络 / 429 自动重试次数（0 = 不重试）。 */
    fun setNetRetryCount(count: Int) {
        viewModelScope.launch { settingsRepository.setNetRetryCount(count.coerceIn(0, 5)) }
    }

    // ===== 29 轮：一键导出 / 导入全量数据 =====

    /** 备份结果提示（一次性，UI 弹 toast 后消费）。 */
    private val _backupEvent = MutableStateFlow<String?>(null)
    val backupEvent: StateFlow<String?> = _backupEvent.asStateFlow()

    /** 导入完成信号（一次性）：true=成功（UI 弹重启确认）；false=失败（原因见 backupEvent）。 */
    private val _importDone = MutableStateFlow<Boolean?>(null)
    val importDone: StateFlow<Boolean?> = _importDone.asStateFlow()

    fun consumeBackupEvent() {
        _backupEvent.value = null
    }

    fun consumeImportDone() {
        _importDone.value = null
    }

    /** 导出全部数据（设置 + API 配置 + 会话 + 消息 + token 统计）到所选 SAF 文件。 */
    fun exportAllData(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = backupManager.export()
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw java.io.IOException("无法写入所选文件")
                }
            }
            _backupEvent.value = result.fold(
                onSuccess = { "备份已导出" },
                onFailure = { "导出失败：${it.message}" },
            )
        }
    }

    /** 导入全量数据（全量替换）；完成经 importDone 通知，UI 据此提示重启生效。 */
    fun importAllData(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = appContext.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw java.io.IOException("无法读取所选文件")
                    backupManager.import(text).getOrThrow()
                }
            }
            result.fold(
                onSuccess = { _importDone.value = true },
                onFailure = {
                    _backupEvent.value = "导入失败：${it.message}"
                    _importDone.value = false
                },
            )
        }
    }

    // ===== 27 轮：会话流程图（平面树）=====

    /**
     * 流程图节点（递归树）：不展示正文，AI 节点标「第 N 次生成」、用户节点标内容摘要；
     * children = 同层兄弟分支（重新生成版本组 / 编辑重发用户分支），点击非激活分支切换。
     */
    data class FlowNodeUi(
        val messageId: Long,
        val isUser: Boolean,
        val label: String,
        val active: Boolean,
        val children: List<FlowNodeUi>,
    )

    private val _flowDiagram = MutableStateFlow<List<FlowNodeUi>?>(null)
    val flowDiagram: StateFlow<List<FlowNodeUi>?> = _flowDiagram.asStateFlow()

    /** 打开流程图面板时构建平面树：老主线（parent==null 的用户消息）作顶层，逐层挂分支。 */
    fun loadFlowDiagram() {
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { conversationRepository.getMessages(_currentConversationId.value) }
            val activeIds = _messages.value.map { it.dbId }.toSet()
            val children = all.filter { it.parentId != null }.groupBy { it.parentId }
            val assistantByKey = all
                .filter { it.role == MessageRole.ASSISTANT && it.versionKey != null }
                .groupBy { it.versionKey!! }

            /** 28 轮修正：折叠 AI 节点——只渲染输入内容（用户节点）；每条分支末端（最后一条输出）保留半宽小方块。 */
            fun buildUserNode(
                m: ChatMessage,
                siblings: Int,
                branchIndex: Int,
            ): FlowNodeUi {
                val base = m.content.replace("\n", " ").trim()
                val prefix = if (siblings > 1) "改$branchIndex " else ""
                val label = if (prefix.isNotEmpty()) (prefix + base).take(11).ifBlank { "[图片]" }
                else base.take(10).ifBlank { "[图片]" }

                val sub = mutableListOf<FlowNodeUi>()
                // 该输入下的每个回复版本：无后续对话 = 分支末端半宽小方块；有后续 = 直接挂后续用户分支（继续递归）
                val group = assistantByKey[m.id].orEmpty()
                group.forEach { a ->
                    val kids = children[a.id].orEmpty().filter { it.role == MessageRole.USER }
                    if (kids.isEmpty()) {
                        // 分支末端：标「结果 N」（N = 该分支的生成序号）
                        sub += FlowNodeUi(
                            messageId = a.id,
                            isUser = false,
                            label = "结果 ${a.version}",
                            active = a.id in activeIds,
                            children = emptyList(),
                        )
                    } else {
                        kids.forEachIndexed { i, k ->
                            sub += buildUserNode(k, kids.size, if (kids.size > 1) i + 1 else 0)
                        }
                    }
                }
                // 输入本身的编辑重发分支（兄弟挂在同一父下，不会出现在 children[m.id]，此处只兜底主线外分支）
                return FlowNodeUi(
                    messageId = m.id,
                    isUser = true,
                    label = label,
                    active = m.id in activeIds,
                    children = sub,
                )
            }

            // 顶层 = 每条主线根输入一棵树（第一轮被编辑重发时为多根横排）
            val rootUsers = all.filter { it.role == MessageRole.USER && it.parentId == null }
            val topNodes = rootUsers.mapIndexed { i, r ->
                buildUserNode(r, rootUsers.size, if (rootUsers.size > 1) i + 1 else 0)
            }
            _flowDiagram.value = topNodes
        }
    }

    /** 关闭流程图面板时清空数据。 */
    fun dismissFlowDiagram() {
        _flowDiagram.value = null
    }

    /** 27 轮：流程图分支点击——用户分支按父组 / 助手按版本组记录激活，重建整条路径。 */
    fun selectBranch(messageId: Long) {
        viewModelScope.launch {
            val msg = withContext(Dispatchers.IO) { conversationRepository.getMessageById(messageId) }
                ?: return@launch
            val key = if (msg.role == MessageRole.USER) {
                msg.parentId ?: return@launch
            } else {
                msg.versionKey ?: return@launch
            }
            branchChoice[key] = messageId
            rebuildActivePath()
            loadFlowDiagram() // 面板高亮同步刷新
        }
    }

    /** 20 轮：聊天页自定义背景图（应用内路径；空串 = 默认背景）。 */
    fun setBackgroundImage(path: String) {
        viewModelScope.launch { settingsRepository.setBackgroundImageUri(path) }
    }

    /** 20 轮：UI 配色预设（default / ocean / forest / ember）。 */
    fun setColorPreset(preset: String) {
        viewModelScope.launch { settingsRepository.setColorPreset(preset) }
    }

    /** 23 轮：跟随系统明暗开关（开启时在浅色/深色两组预设间自动切换）。 */
    fun setFollowSystemTheme(value: Boolean) {
        viewModelScope.launch { settingsRepository.setFollowSystemTheme(value) }
    }

    /** 23 轮：手动压缩摘要字数上限（100~1024）。 */
    fun setCompressSummaryLimit(value: Int) {
        viewModelScope.launch { settingsRepository.setCompressSummaryLimit(value) }
    }

    /** 25 轮：玻璃材质档位（0 关 / 1 半透明 / 2 毛玻璃）。 */
    fun setGlassLevel(value: Int) {
        viewModelScope.launch { settingsRepository.setGlassLevel(value) }
    }

    /** 24 轮：置顶 / 取消置顶会话。 */
    fun togglePinned(conversationId: String) {
        viewModelScope.launch {
            conversations.value.firstOrNull { it.id == conversationId }?.let {
                conversationRepository.setPinned(conversationId, !it.pinned)
            }
        }
    }

    /**
     * 22 轮：取最新发布版 APK 直链后直接跳浏览器下载。
     * 系统下载器在部分机型/网络下无法连 GitHub（一直 0 进度），浏览器下载更可靠。
     * 取直链失败回退浏览器打开 Releases 页手动下载。
     */
    fun downloadLatestApk() {
        showAppToast(
            when (settings.value.language) {
                AppLanguage.TRADITIONAL_CHINESE -> "正在獲取最新版本…"
                AppLanguage.ENGLISH -> "Checking for the latest version…"
                else -> "正在获取最新版本…"
            },
        )
        viewModelScope.launch {
            val url = runCatching { chatRepository.latestApkDownloadUrl() }.getOrNull()
            openInBrowser(
                url?.takeIf { it.isNotBlank() }
                    ?: "https://github.com/BambooMosaic/LLMChatMobile/releases/latest",
            )
        }
    }

    /** 主线程弹 Toast。 */
    private fun showAppToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** 20 轮：浏览器打开链接（下载兜底入口）。 */
    private fun openInBrowser(url: String) {
        runCatching {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url),
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
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

    fun setRoleplayParenRule(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRoleplayParenRule(enabled) }
    }

    /**
     * 实际生效的单次输出上限。
     * 省 Token 档位：1=质量 16384 / 2=中等 8192 / 3=最大 4096；
     * 豁免模式（代码开发 / 创意写作 / Legacy）不受档位限制，回退到全局配置值。
     * 0 或豁免时用 maxOutputTokens（0 = 不限制）。
     */
    /**
     * 21 轮：省 Token 档位下的单次输出上限。
     * - 终极档（4）：所有模式（含 Legacy / DS 网页版）强制 64
     * - 1~3 档：创意写作 / 代码开发 / Legacy / DS 网页版豁免，其余按档位上限
     * - 省钱模式：22 轮起按模板里的「最多输出 N 个字符」设置硬性 max_tokens（此前只是提示词软约束，
     *   模型经常超字；字符近似 token，宁短勿超），并与终极档 / 全局上限取更严格者
     * - 档位上限与用户全局上限取较小值（都设置时以更严格者为准）
     */
    private fun effectiveMaxOutputTokens(
        settings: AppSettings,
        modeId: String,
        userPrompt: String? = null,
    ): Int? {
        if (modeId == ModeConfig.SAVING) {
            val chars = parseSavingMaxChars(userPrompt)
            val caps = listOfNotNull(
                chars,
                64.takeIf { settings.tokenSavingLevel == 4 },
                settings.maxOutputTokens.takeIf { it > 0 },
            )
            return caps.minOrNull()
        }
        val userCap = settings.maxOutputTokens.takeIf { it > 0 }
        fun capped(value: Int): Int = minOf(value, userCap ?: value)
        if (settings.tokenSavingLevel == 4) return capped(64)
        val exempt = modeId == ModeConfig.CODE ||
            modeId == ModeConfig.CREATIVE ||
            modeId == ModeConfig.LEGACY ||
            modeId == ModeConfig.DS_WEB
        if (exempt) return userCap
        return when (settings.tokenSavingLevel) {
            1 -> capped(4096)
            2 -> capped(1024)
            3 -> capped(256)
            else -> userCap
        }
    }

    /** 22 轮：从省钱模式用户层提示词解析「最多输出 N 个字符」的 N（非省钱模板返回 null）。 */
    private fun parseSavingMaxChars(userPrompt: String?): Int? {
        val text = userPrompt?.trim() ?: return null
        if (!text.startsWith("你最多输出")) return null
        return Regex("\\d+").find(text.substringBefore("个字符"))?.value?.toIntOrNull()
            ?.takeIf { it > 0 }
    }

    /** 21 轮：终极档模式门禁提示。 */
    private fun ultimateTierHint(settings: AppSettings): String = when (settings.language) {
        AppLanguage.TRADITIONAL_CHINESE -> "终极档已开启：只能在省钱模式对话中发送消息"
        AppLanguage.ENGLISH -> "Ultimate tier is on — messages are only allowed in Economy mode chats"
        else -> "终极档已开启：只能在省钱模式对话中发送消息"
    }

    /** 21 轮：终极档（所有模式）与 3/最大档（DS 网页版、Legacy 除外）强制关闭联网搜索与深度思考。 */
    private fun tierDisablesFeatures(settings: AppSettings, modeId: String): Boolean =
        settings.tokenSavingLevel == 4 ||
            (settings.tokenSavingLevel == 3 && modeId != ModeConfig.DS_WEB && modeId != ModeConfig.LEGACY)

    /** 21 轮：上下文占用估算（字符数近似，与压缩逻辑同口径）。22 轮起联网搜索内容不进上下文，不再分项统计。 */
    data class ContextUsage(
        val systemChars: Long = 0,
        val messageChars: Long = 0,
        val reasoningChars: Long = 0,
        val capacityTokens: Long = 0,
    ) {
        val usedChars: Long get() = systemChars + messageChars + reasoningChars
        val fraction: Float
            get() = if (capacityTokens <= 0) 0f else (usedChars.toFloat() / capacityTokens).coerceIn(0f, 1f)
    }

    private val _contextUsage = MutableStateFlow(ContextUsage())
    val contextUsage: StateFlow<ContextUsage> = _contextUsage.asStateFlow()

    /** 按当前会话重新估算上下文占用（消息变化与打开面板时调用）。 */
    fun refreshContextUsage() {
        viewModelScope.launch {
            val currentSettings = settings.value
            val history = _messages.value
                .filter { !it.isStreaming }
                .map {
                    ChatMessage(
                        it.role,
                        it.text,
                        reasoning = it.reasoning.takeIf { r -> r.isNotBlank() },
                    )
                }
            val capacity = (currentConfig.value?.maxContextTokens?.takeIf { it > 0 }
                ?: currentSettings.maxContextTokens).toLong()
            if (history.isEmpty()) {
                _contextUsage.value = ContextUsage(capacityTokens = capacity)
                return@launch
            }
            val bundle = runCatching { buildRequestBundle(currentSettings, history) }.getOrNull()
            if (bundle == null) {
                _contextUsage.value = ContextUsage(capacityTokens = capacity)
                return@launch
            }
            var systemChars = 0L
            var messageChars = 0L
            var reasoningChars = 0L
            bundle.messages.forEach { m ->
                if (m.role == MessageRole.SYSTEM) {
                    systemChars += m.content.length.toLong()
                } else {
                    messageChars += m.content.length.toLong()
                    reasoningChars += (m.reasoning?.length ?: 0).toLong()
                }
            }
            _contextUsage.value = ContextUsage(
                systemChars = systemChars,
                messageChars = messageChars,
                reasoningChars = reasoningChars,
                capacityTokens = capacity,
            )
        }
    }

    /** 21 轮：手动压缩系统提示词（按语言）。23 轮：字数上限用户可自定义。 */
    private fun compressSystemPrompt(settings: AppSettings): String = when (settings.language) {
        AppLanguage.TRADITIONAL_CHINESE ->
            "你是对话压缩器。把用户给出的对话历史压缩成简洁摘要：保留关键事实、已做的决定、未完成任务与用户偏好；不超过 ${settings.compressSummaryLimit} 字；只输出摘要本身。"
        AppLanguage.ENGLISH ->
            "You are a conversation compactor. Summarize the provided chat history concisely: keep key facts, decisions, open tasks and user preferences; at most ${settings.compressSummaryLimit} words; output only the summary."
        else ->
            "你是对话压缩器。把用户给出的对话历史压缩成简洁摘要：保留关键事实、已做的决定、未完成任务与用户偏好；不超过 ${settings.compressSummaryLimit} 字；只输出摘要本身。"
    }

    /** 21 轮：手动压缩——用当前模型把较早对话压成摘要存入会话，再物理删除旧消息（保留最近 4 条）。 */
    fun compressContextManually() {
        viewModelScope.launch {
            val id = _currentConversationId.value
            val msgs = conversationRepository.getMessages(id)
            if (msgs.size <= 6) {
                showAppToast(
                    when (settings.value.language) {
                        AppLanguage.TRADITIONAL_CHINESE -> "对话较短，无需压缩"
                        AppLanguage.ENGLISH -> "This chat is too short to compress"
                        else -> "对话较短，无需压缩"
                    },
                )
                return@launch
            }
            val keep = 4
            val old = msgs.dropLast(keep)
            val historyText = old.joinToString("\n") { m ->
                (if (m.role == MessageRole.USER) "用户" else "AI") + "：" + m.content.take(400)
            }.take(6000)
            showAppToast(
                when (settings.value.language) {
                    AppLanguage.TRADITIONAL_CHINESE -> "正在压缩对话…"
                    AppLanguage.ENGLISH -> "Compressing conversation…"
                    else -> "正在压缩对话…"
                },
            )
            val summary = StringBuilder()
            runCatching {
                chatRepository.sendChatStream(
                    sessionId = id,
                    messages = listOf(
                        ChatMessage(MessageRole.SYSTEM, compressSystemPrompt(settings.value)),
                        ChatMessage(MessageRole.USER, historyText),
                    ),
                    model = currentConfig.value?.model ?: settings.value.model,
                    thinkingEnabled = false,
                    maxTokens = 512,
                ).collect { chunk -> summary.append(chunk) }
            }.onFailure {
                showAppToast(
                    when (settings.value.language) {
                        AppLanguage.TRADITIONAL_CHINESE -> "压缩失败，请稍后重试"
                        AppLanguage.ENGLISH -> "Compression failed — try again later"
                        else -> "压缩失败，请稍后重试"
                    },
                )
                return@launch
            }
            val summaryText = summary.toString().trim()
            if (summaryText.isBlank()) {
                showAppToast(
                    when (settings.value.language) {
                        AppLanguage.TRADITIONAL_CHINESE -> "压缩失败，请稍后重试"
                        AppLanguage.ENGLISH -> "Compression failed — try again later"
                        else -> "压缩失败，请稍后重试"
                    },
                )
                return@launch
            }
            // 摘要写入会话独立提示词列（双层架构下已废弃的旧字段，正好复用）
            val prev = conversationRepository.getConversationById(id)?.systemPrompt.orEmpty()
            conversationRepository.setSystemPrompt(
                id,
                (if (prev.isBlank()) "" else "$prev\n") + summaryText.take(4000),
            )
            conversationRepository.deleteMessagesExceptLast(id, keep)
            _messages.value = loadActivePath(id)
            refreshContextUsage()
            showAppToast(
                when (settings.value.language) {
                    AppLanguage.TRADITIONAL_CHINESE -> "压缩完成，较早对话已转为摘要"
                    AppLanguage.ENGLISH -> "Compressed — earlier messages are now a summary"
                    else -> "压缩完成，较早对话已转为摘要"
                },
            )
        }
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
    /** 19 轮：搜索降级提示——没有可用的 DeepSeek 配置。 */
    private fun searchUnavailableHint(settings: AppSettings): String = when (settings.language) {
        AppLanguage.TRADITIONAL_CHINESE -> "未找到 DeepSeek 配置，本轮回答未使用联网搜索"
        AppLanguage.ENGLISH -> "No DeepSeek config found — this reply skipped web search"
        else -> "未找到 DeepSeek 配置，本轮回答未使用联网搜索"
    }

    /** 19 轮：搜索降级提示——搜索调用失败。 */
    private fun searchFailedHint(settings: AppSettings): String = when (settings.language) {
        AppLanguage.TRADITIONAL_CHINESE -> "联网搜索失败，本轮回答未使用搜索结果"
        AppLanguage.ENGLISH -> "Web search failed — this reply was generated without search results"
        else -> "联网搜索失败，本轮回答未使用搜索结果"
    }

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

        /** 29 轮：TTS 截断上限——DeepSeek 4200 字（≈8400 计费字符）、Qwen 600、GLM 1024。 */
        const val DEEPSEEK_TTS_CLIP = 4200
        const val QWEN_TTS_CLIP = 600
        const val GLM_TTS_CLIP = 1024
    }
}
