package com.escape.ui.feature.chat

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.escape.ui.core.domain.Conversation
import com.escape.ui.core.domain.MessageRole
import kotlinx.coroutines.launch

private val MessageSpacing = 12.dp
private val TurnSpacing = 24.dp
private val MaxContentWidth = 720.dp
private val BubbleRadius = 8.dp
private val InputRadius = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()
    val totalTokens by viewModel.sessionTotalTokens.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentId by viewModel.currentConversationId.collectAsState()

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
                onNewConversation = {
                    viewModel.newConversation()
                    scope.launch { drawerState.close() }
                },
                onSelect = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = { scope.launch { drawerState.open() } }) {
                        Text("☰")
                    }
                },
                title = { Text("Chat") },
                actions = { TextButton(onClick = { showSettings = true }) { Text("Settings") } },
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
                        text = "请求失败：$error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                text = "Tokens: $totalTokens",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Message") },
                    shape = RoundedCornerShape(InputRadius),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                Button(onClick = {
                    viewModel.sendUserMessage(input)
                    input = ""
                }) {
                    Text("Send")
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            systemPrompt = settings.systemPrompt,
            model = settings.model,
            baseUrl = settings.baseUrl,
            apiKey = settings.apiKey,
            onSave = { prompt, model, baseUrl, apiKey ->
                viewModel.setSystemPrompt(prompt)
                viewModel.setModel(model)
                viewModel.setBaseUrl(baseUrl)
                viewModel.setApiKey(apiKey)
            },
            onDismiss = { showSettings = false },
        )
    }
}

/** 侧边栏：顶部标题 + 开启新对话 + 历史会话列表。 */
@Composable
private fun ConversationDrawer(
    conversations: List<Conversation>,
    currentId: String,
    onNewConversation: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LLMChat01",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(
                onClick = onNewConversation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开启新对话")
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
        }
        LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(conversations, key = { it.id }) { conversation ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = conversation.title,
                            maxLines = 1,
                        )
                    },
                    selected = conversation.id == currentId,
                    onClick = { onSelect(conversation.id) },
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: UiMessage) {
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
        Surface(
            shape = RoundedCornerShape(BubbleRadius),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier.widthIn(max = MaxContentWidth),
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

@Composable
private fun SettingsDialog(
    systemPrompt: String,
    model: String,
    baseUrl: String,
    apiKey: String,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var promptDraft by remember { mutableStateOf(systemPrompt) }
    var modelDraft by remember { mutableStateOf(model) }
    var baseUrlDraft by remember { mutableStateOf(baseUrl) }
    var apiKeyDraft by remember { mutableStateOf(apiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                OutlinedTextField(
                    value = baseUrlDraft,
                    onValueChange = { baseUrlDraft = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelDraft,
                    onValueChange = { modelDraft = it },
                    label = { Text("Model") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    label = { Text("System prompt") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(promptDraft, modelDraft, baseUrlDraft, apiKeyDraft)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
