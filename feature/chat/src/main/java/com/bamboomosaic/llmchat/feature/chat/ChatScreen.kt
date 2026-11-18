package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bamboomosaic.llmchat.core.domain.MessageRole
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlinx.coroutines.launch

private val MessageSpacing = 12.dp
private val TurnSpacing = 24.dp
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

    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = conversations,
                currentId = currentId,
                globalTotals = globalTotals,
                onNewConversation = {
                    viewModel.newConversation()
                    scope.launch { drawerState.close() }
                },
                onSelect = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onRename = { id, title -> viewModel.renameConversation(id, title) },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    showSettings = true
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = { scope.launch { drawerState.open() } }) { Text("☰") }
                },
                title = {
                    Text(
                        text = currentTitle.ifBlank { s.appName },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    Text(
                        text = s.sessionTokensText(sessionTotals.total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(messages) { index, msg ->
                    val isNewTurn = msg.role == MessageRole.USER && index > 0
                    Spacer(Modifier.height(if (isNewTurn) TurnSpacing else if (index > 0) MessageSpacing else 0.dp))
                    MessageBubble(message = msg)
                }
            }

            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${s.requestFailed}：$error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            ChatInputBar(
                input = input,
                onInputChange = { input = it },
                thinkingEnabled = settings.thinkingEnabled,
                onToggleThinking = viewModel::setThinkingEnabled,
                onSend = {
                    viewModel.sendUserMessage(input)
                    input = ""
                },
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onSetProvider = viewModel::setProvider,
            onSetBaseUrl = viewModel::setBaseUrl,
            onSetApiKey = viewModel::setApiKey,
            onSetModel = viewModel::setModel,
            onSetSystemPrompt = viewModel::setSystemPrompt,
            onSetThemeMode = viewModel::setThemeMode,
            onSetLanguage = viewModel::setLanguage,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun MessageBubble(message: UiMessage) {
    val s = LocalAppStrings.current
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
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.isStreaming && message.text.isEmpty()) {
                        SkeletonBubble()
                    } else {
                        Text(
                            text = message.text + if (message.isStreaming) " ▌" else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
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
