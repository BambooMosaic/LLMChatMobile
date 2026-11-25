package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlinx.coroutines.launch

private val MessageSpacing = 12.dp
private val TurnSpacing = 24.dp
private const val BOTTOM_SCROLL_OFFSET = 100_000
private val MaxContentWidth = 720.dp
private val BubbleRadius = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val s = LocalAppStrings.current
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentId by viewModel.currentConversationId.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val sessionTotals by viewModel.sessionTotals.collectAsState()
    val globalTotals by viewModel.globalTotals.collectAsState()
    val dailyTokenStats by viewModel.dailyTokenStats.collectAsState()
    val hourlyTokenStats by viewModel.hourlyTokenStats.collectAsState()
    val tokenStatsDays by viewModel.tokenStatsDays.collectAsState()
    val archivedConversations by viewModel.archivedConversations.collectAsState()
    val apiConfigs by viewModel.apiConfigs.collectAsState()
    val currentConfig by viewModel.currentConfig.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val pendingImages by viewModel.pendingImages.collectAsState()

    var input by remember { mutableStateOf("") }
    val currentConversation = conversations.firstOrNull { it.id == currentId }
    val effectiveModeId by viewModel.effectiveModeId.collectAsState()
    val effectiveUserPrompt by viewModel.effectiveUserPrompt.collectAsState()
    val screenContext = LocalContext.current
    val screenStrings = LocalAppStrings.current
    // 新建对话模式弹窗：默认模式为「禁用」时先弹窗选择
    var showNewChatModeDialog by remember { mutableStateOf(false) }
    // 会话模式编辑弹窗（工具栏模式按钮）
    var showModeEditor by remember { mutableStateOf(false) }
    // 18 轮新增：灰色模式（锁定态）只读预览弹窗
    var showModePreview by remember { mutableStateOf(false) }
    // 截断轻提示
    LaunchedEffect(Unit) {
        viewModel.truncatedToast.collect {
            android.widget.Toast.makeText(
                screenContext, screenStrings.promptTruncated,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    // 长按编辑：VM 发出待编辑草稿 → 回填输入框
    val editDraft by viewModel.editDraft.collectAsState()
    LaunchedEffect(editDraft) {
        editDraft?.let {
            input = it
            viewModel.consumeEditDraft()
        }
    }
    var showSettings by remember { mutableStateOf(false) }
    var showTokenStats by remember { mutableStateOf(false) }
    var exportTarget by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 流式生成时跟随到底部；用户手动往上翻看历史时不打扰。
    // 切换/打开会话后的第一次渲染先强制滚到最新。
    val lastTextLength = messages.lastOrNull()?.text?.length ?: 0
    val reasoningLength = messages.lastOrNull()?.reasoning?.length ?: 0
    var autoScrolledOnce by remember(currentId) { mutableStateOf(false) }
    // 记录用户上滑脱离底部的状态：一旦离开底部，自动滚动彻底停用，
    // 直到用户重新滑回底部附近才恢复（修复长对话回看被拉回的抖动）。
    var userScrolledUp by remember(currentId) { mutableStateOf(false) }
    // 用户手势滚动监听：滑动方向向上（远离底部）时标记脱离底部
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .collect { (scrolling, _) ->
                if (scrolling) {
                    val visible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (visible < messages.size - 2) userScrolledUp = true
                    // 用户主动滑回底部附近时解除锁定
                    if (visible >= messages.size) userScrolledUp = false
                }
            }
    }
    LaunchedEffect(messages.size, lastTextLength, reasoningLength) {
        if (messages.isEmpty()) return@LaunchedEffect
        // 用户手势滚动进行中：绝不强制滚动
        if (listState.isScrollInProgress) return@LaunchedEffect
        // 用户已上滑脱离底部：彻底停用自动滚动（流式增量也不跟随）
        if (userScrolledUp) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        // 锚点在 messages.size 位置，接近它即视为在底部
        val nearBottom = visible >= messages.size - 2
        if (!autoScrolledOnce) {
            // 首次进入会话：直接跳到最新（长历史动画过慢）。
            // 单条消息可能极长（思考 + 正文），高度需要时间测量。
            // 等待布局稳定后一次性滚动；期间若用户开始滑动则放弃。
            withFrameNanos { }
            withFrameNanos { }
            if (listState.isScrollInProgress) return@LaunchedEffect
            listState.scrollToItem(messages.size, BOTTOM_SCROLL_OFFSET)
            autoScrolledOnce = true
        } else if (nearBottom) {
            // 20 轮：流式跟随改为平滑动画——高频触发时新动画从当前位置继续滑向底部，观感顺滑
            listState.animateScrollToItem(messages.size, BOTTOM_SCROLL_OFFSET)
        }
    }

    // 27 轮：语音朗读状态 + 失败 toast（tick>0 时弹一次；优先展示具体原因如余额不足）
    val ttsState by viewModel.ttsState.collectAsState()
    val ttsErrorTick by viewModel.ttsErrorTick.collectAsState()
    val ttsErrorMessage by viewModel.ttsErrorMessage.collectAsState()
    LaunchedEffect(ttsErrorTick) {
        if (ttsErrorTick > 0) {
            val msg = ttsErrorMessage?.takeIf { it.isNotBlank() } ?: s.ttsFailed
            android.widget.Toast.makeText(screenContext, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // 27 轮：会话流程图面板
    var showFlowPanel by remember { mutableStateOf(false) }

    // 21 轮：上下文占用（环形按钮 + 统计面板）
    val contextUsage by viewModel.contextUsage.collectAsState()
    var showContextPanel by remember { mutableStateOf(false) }
    var showCompressConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(messages.size, currentId) { viewModel.refreshContextUsage() }

    // 25 轮：玻璃档位（抽屉 scrim 用）
    val glassLevel = settings.glassLevel

    // 25 轮：已有消息的会话集合（编辑提示词入口显隐）
    val hasMessagesIds by viewModel.conversationsWithMessages.collectAsState()

    // 23 轮：会话内搜索（入口：侧边栏会话长按菜单）——过滤当前会话消息并高亮关键词
    var chatSearchActive by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }

    // 20 轮：平板/横屏（宽屏）侧边栏常驻展开，与聊天内容并排；手机保持抽屉交互
    val wideScreen = LocalConfiguration.current.screenWidthDp >= 600
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }
    val drawerContent: @Composable (Boolean) -> Unit = { permanent ->
        ConversationDrawer(
            conversations = conversations,
            currentId = currentId,
            permanent = permanent,
            onNewConversation = {
                closeDrawer()
                if (settings.defaultModeId == "off") {
                    // 禁用：弹模式选择弹窗
                    showNewChatModeDialog = true
                } else {
                    viewModel.newConversation()
                }
            },
            onSelect = { id ->
                viewModel.selectConversation(id)
                closeDrawer()
            },
            onRename = { id, title -> viewModel.renameConversation(id, title) },
            onArchive = { id -> viewModel.archiveConversation(id) },
            onEditPrompt = { id ->
                viewModel.selectConversation(id)
                closeDrawer()
                showModeEditor = true
            },
            onExport = { id ->
                exportTarget = id
                closeDrawer()
            },
            onTogglePinned = viewModel::togglePinned,
            hasMessagesIds = hasMessagesIds,
            // 23 轮：会话内搜索——切到目标会话并唤起搜索栏
            onSearch = { id ->
                viewModel.selectConversation(id)
                chatSearchActive = true
                chatSearchQuery = ""
                closeDrawer()
            },
            onOpenTokenStats = {
                closeDrawer()
                showTokenStats = true
            },
            onOpenSettings = {
                closeDrawer()
                showSettings = true
            },
        )
    }
    val chatBody: @Composable (Boolean) -> Unit = { permanentDrawer ->
        // 必须自己绘制背景：否则内容区透明会露出窗口背景（浅色主题下是白色）
        // imePadding：Android 15 强制 edge-to-edge 后窗口不再 resize，不加会被键盘挡住输入栏/发送按钮
        // 21 轮：支持自定义背景图（缩放居中 + 可读性遮罩），未设置时用默认纯色
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
        ) {
            if (settings.backgroundImageUri.isNotBlank()) {
                val bgBitmap = remember(settings.backgroundImageUri) {
                    runCatching { android.graphics.BitmapFactory.decodeFile(settings.backgroundImageUri) }
                        .getOrNull()
                }
                if (bgBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bgBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    if (!permanentDrawer) {
                        TextButton(onClick = { scope.launch { drawerState.open() } }) { Text("☰") }
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }
                },
                title = {
                    Text(
                        text = currentTitle.ifBlank { s.appName },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    // 27 轮修正：顶部只留流程图入口（环形统计移到输入栏发送键左侧，Token 文本删除——点环形即可看）
                    // 图标 = 对齐的 T 形分叉树（参考用户手绘形式重画，居中对称）
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(6.dp)
                            .clickable {
                                showFlowPanel = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val fork = MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val stroke = 1.5.dp.toPx()
                            val boxW = w * 0.26f
                            val boxH = h * 0.24f
                            val r = CornerRadius(2.5.dp.toPx())
                            val lineStyle = Stroke(width = stroke)

                            // 顶部节点：居中
                            val topX = (w - boxW) / 2f
                            val topY = h * 0.08f
                            drawRoundRect(fork, Offset(topX, topY), Size(boxW, boxH), r, style = lineStyle)
                            val topCX = w / 2f
                            val topBottom = topY + boxH

                            // 两个子节点：左右对称
                            val botY = h * 0.66f
                            val leftCX = w * 0.25f
                            val rightCX = w * 0.75f
                            val midY = (topBottom + botY) / 2f

                            // 连线：顶节点底 → 竖到 midY → 横到两端 → 竖到子节点顶（正交 T 形）
                            drawLine(fork, Offset(topCX, topBottom), Offset(topCX, midY), strokeWidth = stroke)
                            drawLine(fork, Offset(leftCX, midY), Offset(rightCX, midY), strokeWidth = stroke)
                            drawLine(fork, Offset(leftCX, midY), Offset(leftCX, botY), strokeWidth = stroke)
                            drawLine(fork, Offset(rightCX, midY), Offset(rightCX, botY), strokeWidth = stroke)

                            drawRoundRect(fork, Offset(leftCX - boxW / 2, botY), Size(boxW, boxH), r, style = lineStyle)
                            drawRoundRect(fork, Offset(rightCX - boxW / 2, botY), Size(boxW, boxH), r, style = lineStyle)
                        }
                    }
                },
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // 23 轮：会话内搜索栏（侧边栏长按会话唤起）——实时过滤 + 关键词高亮
            if (chatSearchActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = chatSearchQuery,
                        onValueChange = { chatSearchQuery = it },
                        placeholder = { Text(s.searchHint, style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        chatSearchActive = false
                        chatSearchQuery = ""
                    }) { Text("×", style = MaterialTheme.typography.titleLarge) }
                }
            }
            // 搜索模式：过滤当前会话消息（不跨会话）；正常模式：全部消息
            val displayMessages = if (chatSearchActive && chatSearchQuery.isNotBlank()) {
                messages.filter { it.text.contains(chatSearchQuery, ignoreCase = true) }
            } else {
                messages
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (chatSearchActive && chatSearchQuery.isNotBlank() && displayMessages.isEmpty()) {
                    item(key = "search-empty") {
                        Text(
                            text = s.searchEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                itemsIndexed(
                    displayMessages,
                    key = { itemIndex, msg ->
                        // 稳定 key：已持久化用 dbId+version；流式中的消息（dbId=0）在列表尾部，用 role+序号区分
                        // 搜索过滤模式下前缀区分，避免与正常模式的同 key 冲突
                        val base = if (msg.dbId > 0) "m-${msg.dbId}-v${msg.version}" else "live-$itemIndex-${msg.role}"
                        if (chatSearchActive && chatSearchQuery.isNotBlank()) "search-$base" else base
                    },
                ) { index, msg ->
                    if (chatSearchActive && chatSearchQuery.isNotBlank()) {
                        // 搜索结果：轻量纯文本渲染 + 关键词高亮（不做 Markdown 解析）
                        Spacer(Modifier.height(MessageSpacing))
                        SearchMessageItem(text = msg.text, query = chatSearchQuery, role = msg.role)
                    } else {
                    val isNewTurn = msg.role == MessageRole.USER && index > 0
                    Spacer(Modifier.height(if (isNewTurn) TurnSpacing else if (index > 0) MessageSpacing else 0.dp))
                    MessageBubble(
                        message = msg,
                        onToggleReasoning = { viewModel.toggleReasoning(index) },
                        // 最后一条助手消息且非流式中：显示「重新生成」
                        showRegenerate = msg.role == MessageRole.ASSISTANT &&
                            !msg.isStreaming &&
                            index == messages.lastIndex &&
                            messages.lastOrNull()?.role == MessageRole.ASSISTANT,
                        onRegenerate = viewModel::regenerateLast,
                        onCopy = { viewModel.copyMessage(index) },
                        onEdit = if (msg.role == MessageRole.USER && !msg.isStreaming) {
                            { viewModel.startEditMessage(index) }
                        } else {
                            null
                        },
                        onSwitchVersion = { delta -> viewModel.switchVersion(index, delta) },
                        // 27 轮：朗读按钮（设置开启 + DeepSeek 配置 + 完成文本的助手消息）
                        showTts = settings.ttsEnabled &&
                            msg.role == MessageRole.ASSISTANT &&
                            !msg.isStreaming &&
                            msg.text.isNotBlank() &&
                            isDeepSeekConfig(currentConfig),
                        ttsBusy = when (val ts = ttsState) {
                            is ChatViewModel.TtsState.Loading -> if (ts.messageId == msg.dbId) 1 else 0
                            is ChatViewModel.TtsState.Playing -> if (ts.messageId == msg.dbId) 2 else 0
                            else -> 0
                        },
                        onToggleTts = { viewModel.toggleTts(msg.dbId, msg.text) },
                    )
                    }
                }
                // 底部锚点：scrollToItem 只能把某项"顶部"对齐视口，而单条消息可能很长
                // （思考内容 + 正文），所以额外放一个空项作为真正的末尾参照。
                item(key = "bottom-anchor") {
                    Spacer(Modifier.height(1.dp))
                }
            }

            if (error != null) {
                val err = error!! // 委托属性不能 smart cast，显式断言（17 轮已知坑）
                val guide = when (err.kind) {
                    com.bamboomosaic.llmchat.core.domain.ApiError.Kind.AUTH -> s.errGuideAuth
                    com.bamboomosaic.llmchat.core.domain.ApiError.Kind.INSUFFICIENT_BALANCE -> s.errGuideBalance
                    com.bamboomosaic.llmchat.core.domain.ApiError.Kind.RATE_LIMIT -> s.errGuideRateLimit
                    com.bamboomosaic.llmchat.core.domain.ApiError.Kind.TIMEOUT -> s.errGuideTimeout
                    com.bamboomosaic.llmchat.core.domain.ApiError.Kind.NETWORK -> s.errGuideNetwork
                    com.bamboomosaic.llmchat.core.domain.ApiError.Kind.UNKNOWN -> s.errGuideUnknown
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        // 17 轮：错误码 + 对应解决指引（同一气泡内换行追加）
                        text = "${s.requestFailed}：${err.raw}\n$guide",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // 模型切换 + 深度思考已整合进输入栏底栏
            ChatInputBar(
                input = input,
                onInputChange = { input = it },
                thinkingEnabled = settings.thinkingEnabled,
                thinkingSupported = currentConfig?.supportReasoning ?: false,
                onToggleThinking = viewModel::setThinkingEnabled,
                searchEnabled = settings.searchEnabled,
                // 19 轮：搜索对所有模型开放（非 DeepSeek 模型经 DeepSeek-Flash 代理）
                searchSupported = true,
                onToggleSearch = viewModel::setSearchEnabled,
                currentModel = currentConfig?.model?.ifBlank { currentConfig?.name } ?: "—",
                currentConfigId = currentConfig?.id ?: 0,
                modelConfigs = apiConfigs,
                onSelectModel = viewModel::selectConfig,
                onOpenApiSettings = { showSettings = true },
                // 正式会话读库；临时会话读内存草稿
                modeName = com.bamboomosaic.llmchat.core.domain.ChatModes.find(
                    effectiveModeId, settings.language,
                ).localized(settings.language).modeName,
                modeLocked = messages.any { it.role == MessageRole.ASSISTANT && it.dbId > 0 },
                // 18 轮：未锁定打开模式编辑弹窗；锁定（灰色模式）改为只读预览弹窗
                onOpenModeEditor = {
                    val locked = messages.any { m -> m.role == MessageRole.ASSISTANT && m.dbId > 0 }
                    if (locked) showModePreview = true else showModeEditor = true
                },
                contextFraction = contextUsage.fraction,
            onOpenContextPanel = {
                viewModel.refreshContextUsage()
                showContextPanel = true
            },
            isGenerating = isGenerating,
                visionSupported = Providers.supportsVision(currentConfig?.model ?: ""),
                pendingImages = pendingImages,
                onImagesChange = viewModel::setPendingImages,
                onStop = viewModel::stopGeneration,
                onSend = {
                    viewModel.sendUserMessage(input)
                    input = ""
                },
            )
            }
        }
    }

    if (wideScreen) {
        // 20 轮：宽屏侧边栏常驻，聊天内容在右侧
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(300.dp)) { drawerContent(true) }
            Box(modifier = Modifier.weight(1f)) { chatBody(true) }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { drawerContent(false) },
            // 25 轮：玻璃模式去掉打开抽屉时的黑色遮罩（视觉"阴影"的来源）
            scrimColor = if (glassLevel >= 1) androidx.compose.ui.graphics.Color.Transparent
            else androidx.compose.material3.DrawerDefaults.scrimColor,
        ) {
            Box(modifier = Modifier.fillMaxSize()) { chatBody(false) }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            archivedConversations = archivedConversations,
            apiConfigs = apiConfigs,
            onSaveConfig = viewModel::saveConfig,
            onValidateConfig = viewModel::validateConfigRaw,
            onDeleteConfig = viewModel::deleteConfig,
            onSetDefaultConfig = viewModel::setDefaultConfig,
            onSetProvider = viewModel::setProvider,
            onSetBaseUrl = viewModel::setBaseUrl,
            onSetApiKey = viewModel::setApiKey,
            onSetModel = viewModel::setModel,
            onSetDefaultModeId = viewModel::setDefaultModeId,
            onSetContextInjectionEnabled = viewModel::setContextInjectionEnabled,
            onSetTokenSavingLevel = viewModel::setTokenSavingLevel,
            onSetSearchEnabled = viewModel::setSearchEnabled,
            onSetUserNickname = viewModel::setUserNickname,
            onDownloadLatest = viewModel::downloadLatestApk,
            onSetColorPreset = viewModel::setColorPreset,
            onSetFollowSystemTheme = viewModel::setFollowSystemTheme,
            onSetCompressSummaryLimit = viewModel::setCompressSummaryLimit,
            onSetBackgroundImage = viewModel::setBackgroundImage,
            onSetLanguage = viewModel::setLanguage,
            onSetAutoCompressEnabled = viewModel::setAutoCompressEnabled,
            onSetGlassLevel = viewModel::setGlassLevel,
            onSetTtsEnabled = viewModel::setTtsEnabled,
            onSetTtsVoice = viewModel::setTtsVoice,
            onSetTtsAutoRead = viewModel::setTtsAutoRead,
            onRestoreConversation = viewModel::restoreConversation,
            onDeleteConversation = viewModel::deleteConversationPermanently,
            onDismiss = { showSettings = false },
        )
    }

    // 新建对话：模式选择弹窗
    if (showNewChatModeDialog) {
        ModeEditorDialog(
            initialModeId = com.bamboomosaic.llmchat.core.domain.ModeConfig.CHAT,
            initialUserPrompt = null,
            createMode = true,
            uiLanguage = settings.language,
            roleplayParenRule = settings.roleplayParenRule,
            tokenSavingLevel = settings.tokenSavingLevel,
            onRoleplayParenRuleChange = viewModel::setRoleplayParenRule,
            onConfirm = { modeId, userPrompt, temp, topp, imported ->
                viewModel.newConversation()
                viewModel.createConversationWithMode(modeId, userPrompt, temp, topp)
                imported?.takeIf { it.isNotEmpty() }?.let { list ->
                    // 19 轮：导入的模式 JSON 携带对话记录，一并还原进新对话
                    viewModel.importConversationMessages(
                        modeId,
                        list.mapNotNull { m ->
                            runCatching { MessageRole.valueOf(m.roleName) }.getOrNull()?.let { role ->
                                ChatMessage(role, m.text, reasoning = m.reasoning)
                            }
                        },
                    )
                }
                showNewChatModeDialog = false
            },
            onDismiss = { showNewChatModeDialog = false },
        )
    }

    // 会话模式编辑弹窗（工具栏模式按钮）
    if (showModeEditor) {
        val cur = currentConversation
        ModeEditorDialog(
            initialModeId = effectiveModeId,
            initialUserPrompt = effectiveUserPrompt,
            createMode = false,
            uiLanguage = settings.language,
            roleplayParenRule = settings.roleplayParenRule,
            tokenSavingLevel = settings.tokenSavingLevel,
            onRoleplayParenRuleChange = viewModel::setRoleplayParenRule,
            onConfirm = { modeId, userPrompt, temp, topp, _ ->
                viewModel.updateConversationMode(modeId, userPrompt, temp, topp)
                showModeEditor = false
            },
            onDismiss = { showModeEditor = false },
        )
    }

    // 18 轮新增：灰色模式（锁定态）只读预览弹窗
    if (showModePreview) {
        ModePreviewDialog(
            modeId = effectiveModeId,
            modeName = com.bamboomosaic.llmchat.core.domain.ChatModes.find(
                effectiveModeId, settings.language,
            ).localized(settings.language).modeName,
            userPrompt = effectiveUserPrompt,
            uiLanguage = settings.language,
            messages = messages
                .filter { !it.isStreaming && it.text.isNotBlank() }
                .map { ChatMessage(it.role, it.text, reasoning = it.reasoning.takeIf { r -> r.isNotBlank() }) },
            roleplayParenRule = settings.roleplayParenRule,
            onDismiss = { showModePreview = false },
        )
    }

    // 21 轮：上下文统计面板
    if (showContextPanel) {
        ContextPanelDialog(
            usage = contextUsage,
            sessionTotals = sessionTotals,
            onManualCompress = {
                showContextPanel = false
                showCompressConfirm = true
            },
            onDismiss = { showContextPanel = false },
        )
    }

    // 27 轮：会话流程图面板（窗口式竖版时间线 + 分支切换）
    if (showFlowPanel) {
        val flowTurns by viewModel.flowDiagram.collectAsState()
        androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadFlowDiagram() }
        FlowPanelDialog(
            nodes = flowTurns ?: emptyList(),
            onSelectBranch = viewModel::selectBranch,
            onDismiss = {
                viewModel.dismissFlowDiagram()
                showFlowPanel = false
            },
        )
    }

    // 21 轮：手动压缩确认弹窗（摘要替换旧消息，不可逆）
    // 23 轮：升级为预览确认——展示将保留/压缩的条数，待压缩消息过少时禁用确认
    if (showCompressConfirm) {
        val toCompress = (messages.count { !it.isStreaming } - 4).coerceAtLeast(0)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCompressConfirm = false },
            title = { Text(s.manualCompressButton) },
            text = {
                Text(text = s.compressPreviewBody(toCompress, settings.compressSummaryLimit))
            },
            confirmButton = {
                TextButton(
                    enabled = toCompress > 0,
                    onClick = {
                        showCompressConfirm = false
                        viewModel.compressContextManually()
                    },
                ) {
                    Text(
                        s.compressConfirmBtn,
                        color = if (toCompress > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompressConfirm = false }) {
                    Text(s.cancel)
                }
            },
        )
    }

    if (showTokenStats) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTokenStats = false }) {
            TokenStatsScreen(
                totals = globalTotals,
                dailyStats = dailyTokenStats,
                hourlyStats = hourlyTokenStats,
                days = tokenStatsDays,
                onSetDays = viewModel::setTokenStatsDays,
                onDismiss = { showTokenStats = false },
                // 23 轮：金额折算（本次 = 当前会话 / 累计 = 全应用，按当前模型单价）
                sessionTotals = sessionTotals,
                currentModel = currentConfig?.model ?: "",
            )
        }
    }

    // 会话独立提示词编辑弹窗

    // 导出格式选择弹窗
    exportTarget?.let { conversationId ->
        if (conversationId == currentId) {
            ExportFormatDialog(
                onSelect = { format ->
                    viewModel.exportConversation(format)
                    exportTarget = null
                },
                onDismiss = { exportTarget = null },
            )
        } else {
            // 导出非当前会话：先切换到该会话再导出
            viewModel.selectConversation(conversationId)
            exportTarget = null
            // 等切换完成后导出（简化：直接导出，因为 getMessages 按 id 读库，不依赖 UI 状态）
            viewModel.exportConversationBy(conversationId, format = "md")
        }
    }

    // 导出结果提示
    val exportMsg by viewModel.exportMessage.collectAsState()
    val exportContext = LocalContext.current
    LaunchedEffect(exportMsg) {
        val msg = exportMsg ?: return@LaunchedEffect
        android.widget.Toast.makeText(
            exportContext,
            msg,
            android.widget.Toast.LENGTH_LONG,
        ).show()
        viewModel.clearExportMessage()
    }
}

/** 会话独立提示词编辑弹窗。 */
@Composable
private fun PromptEditDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.editPrompt) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(s.editPromptHint) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) { Text(s.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        },
    )
}

/** 导出格式选择弹窗（Markdown / 纯文本 / JSON）。 */
@Composable
private fun ExportFormatDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.export) },
        text = {
            Column {
                androidx.compose.material3.TextButton(
                    onClick = { onSelect("md") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.exportMarkdown)
                }
                androidx.compose.material3.TextButton(
                    onClick = { onSelect("txt") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.exportPlainText)
                }
                // 22 轮：会话导出 JSON（与模式导出格式一致，含模式信息与对话记录）
                androidx.compose.material3.TextButton(
                    onClick = { onSelect("json") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.exportJson)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        },
    )
}

/** 首次启动欢迎弹窗：不可点空白/返回键关闭，只能点「我已知晓」。 */
@Composable
private fun FirstLaunchDialog(onConfirm: () -> Unit) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val versionName = remember {
        val raw = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.1.0"
        // 去掉 alpha/beta/rc 后面的序号（0.1.0-alpha01 → 0.1.0-alpha）
        raw.replace(Regex("(alpha|beta|rc)(\\d+)$", RegexOption.IGNORE_CASE)) { m ->
            m.groupValues[1]
        }
    }

    AlertDialog(
        onDismissRequest = { /* 空实现：不可通过点空白关闭 */ },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(s.iKnow) }
        },
        title = { Text(s.appName, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${s.versionLabel}: $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.testNotice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${s.developer}: BambooMosaic",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "https://github.com/BambooMosaic/LLMChatMobile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/** 消息气泡：支持长按菜单（复制全文/编辑）、多版本切换标识。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: UiMessage,
    onToggleReasoning: () -> Unit,
    showRegenerate: Boolean = false,
    onRegenerate: () -> Unit = {},
    onCopy: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    onSwitchVersion: (Int) -> Unit = {},
    /** 27 轮：朗读按钮可见性（外部判断 DeepSeek / 开关 / 角色 / 非流式）。 */
    showTts: Boolean = false,
    /** 0=空闲 1=合成中 2=播放中（仅本条消息的态）。 */
    ttsBusy: Int = 0,
    onToggleTts: () -> Unit = {},
) {
    val s = LocalAppStrings.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var menuDismissed by remember { mutableStateOf(false) }
    val isUser = message.role == MessageRole.USER
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (showMenu) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(s.copy) },
                onClick = {
                    clipboard.setText(AnnotatedString(message.text))
                    android.widget.Toast.makeText(context, s.copied, android.widget.Toast.LENGTH_SHORT).show()
                    showMenu = false
                },
            )
            // 27 轮修正：朗读入口改到长按菜单（气泡下方按钮命中区太小）
            if (showTts) {
                DropdownMenuItem(
                    text = { Text(if (ttsBusy == 2) s.ttsMenuStop else s.ttsMenuSpeak) },
                    onClick = {
                        onToggleTts()
                        showMenu = false
                    },
                )
            }
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text(s.edit) },
                    onClick = {
                        showMenu = false
                        onEdit()
                    },
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = MaxContentWidth),
        ) {
            Surface(
                shape = RoundedCornerShape(BubbleRadius),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.combinedClickable(
                    onClick = onToggleReasoning,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    },
                ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // 联网搜索状态（思考区域上方）：搜索中 → 已搜索 N 个网页
                    val pages = message.searchPages
                    if (message.isStreaming && pages != null) {
                        Text(
                            text = if (pages > 0) s.searchedPages(pages) else s.searching,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // 思考内容：小一号半透明字，放在正文前面，可点击折叠
                    if (message.reasoning.isNotBlank()) {
                        ReasoningSection(
                            text = message.reasoning,
                            collapsed = message.reasoningCollapsed,
                            onToggle = onToggleReasoning,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (message.isStreaming && message.text.isEmpty()) {
                        SkeletonBubble()
                    } else if (isUser) {
                        // 用户消息：先展示图片（可点击放大），再展示文字
                        message.images.forEach { base64 ->
                            ImageMessageContent(base64 = base64)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else if (message.isStreaming) {
                        // 流式期间用纯文本：Markdown 每帧重新解析全文是长文闪烁的主因
                        Text(
                            text = message.text + " ▍",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        // 生成结束后再按 Markdown 渲染（代码块自带语言标签 + 复制按钮）
                        MarkdownText(content = message.text)
                    }

                    // 联网搜索完成标记：缩小字号、降低透明度，固定在消息末尾
                    if (!message.isStreaming && message.searchPages != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = s.searchDone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            // 多版本切换标识（右上角：‹ 2/3 ›），仅多版本助手消息显示
            if (message.latestVersion > 1 && !message.isStreaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    TextButton(
                        onClick = { onSwitchVersion(-1) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(24.dp),
                    ) { Text("‹") }
                    Text(
                        text = "${message.version}/${message.latestVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { onSwitchVersion(1) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(24.dp),
                    ) { Text("›") }
                }
            }
            val p = message.promptTokens
            val c = message.completionTokens
            if (p != null && c != null) {
                Text(
                    text = s.thisRound(p, c, p + c),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                )
            }
            // 重新生成入口（仅最后一条已完成助手消息显示）
            if (showRegenerate) {
                TextButton(
                    onClick = onRegenerate,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(s.regenerate, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** 深度思考内容：标题行可点击折叠，正文小一号、半透明。 */
@Composable
private fun ReasoningSection(
    text: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    val s = LocalAppStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 2.dp),
        ) {
            Text(
                text = if (collapsed) "▸" else "▾",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = s.thinking,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        if (!collapsed) {
            Spacer(Modifier.height(4.dp))
            // 左侧竖线 + 半透明小字，视觉上从正文里区分开
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .heightIn(min = 16.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 简单骨架占位：两条浅色圆角条，保持加载态布局高度稳定。 */
@Composable
private fun SkeletonBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
        )
    }
}

/** 聊天历史中的图片：展示缩略图，点击放大查看。 */
@Composable
private fun ImageMessageContent(base64: String) {
    var showFull by remember { mutableStateOf(false) }
    val bitmap = remember(base64) {
        runCatching {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
    // 解码期间显示等尺寸占位，避免 item 高度从 0 跳到 160dp 造成列表二次测量抖动
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
        )
        return
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showFull = true },
    )

    if (showFull) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFull = false },
            confirmButton = {},
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

/** 联网搜索仅 DeepSeek 支持服务端工具 deepseek_web_search，据此启用搜索开关。 */
internal fun isDeepSeekConfig(config: com.bamboomosaic.llmchat.core.domain.ApiConfig?): Boolean =
    config != null && (
        config.provider.equals("deepseek", ignoreCase = true) ||
            config.baseUrl.contains("deepseek", ignoreCase = true)
        )

/**
 * 23 轮：会话内搜索结果条目——轻量纯文本（不做 Markdown 解析），关键词高亮。
 */
@Composable
private fun SearchMessageItem(text: String, query: String, role: MessageRole) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = when (role) {
                    MessageRole.USER -> "👤"
                    MessageRole.ASSISTANT -> "🤖"
                    else -> "⚙"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildHighlightedText(text, query),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** 23 轮：命中关键词加高亮背景的 AnnotatedString（大小写不敏感）。 */
private fun buildHighlightedText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val highlight = androidx.compose.ui.text.SpanStyle(
        background = androidx.compose.ui.graphics.Color(0x66FFD54F),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
    )
    return androidx.compose.ui.text.buildAnnotatedString {
        var cursor = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        while (cursor < text.length) {
            val hit = lowerText.indexOf(lowerQuery, cursor)
            if (hit < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, hit))
            pushStyle(highlight)
            append(text.substring(hit, hit + query.length))
            pop()
            cursor = hit + query.length
        }
    }
}
