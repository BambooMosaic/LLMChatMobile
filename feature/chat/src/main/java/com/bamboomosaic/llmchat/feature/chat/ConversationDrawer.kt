package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.TokenTotals
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/** 侧边栏：宽度为屏幕 2/3，顶部标题 + 新对话，列表，底部 Token 统计 + 设置。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationDrawer(
    conversations: List<Conversation>,
    currentId: String,
    globalTotals: TokenTotals,
    onNewConversation: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val s = LocalAppStrings.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val drawerWidth = (screenWidthDp * 2f / 3f).dp

    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var tokenMenuExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(modifier = Modifier.width(drawerWidth)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = s.appName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Button(
                    onClick = onNewConversation,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.newConversation)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
            }

            if (conversations.isEmpty()) {
                Text(
                    text = s.emptyConversations,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(conversations, key = { it.id }) { conversation ->
                    val selected = conversation.id == currentId
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .combinedClickable(
                                onClick = { onSelect(conversation.id) },
                                onLongClick = { renameTarget = conversation },
                            ),
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            HorizontalDivider()

            // 底部固定操作栏：左 Token 统计，右设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { tokenMenuExpanded = true }) {
                        Text("📊 ${s.tokenSummary}")
                    }
                    DropdownMenu(
                        expanded = tokenMenuExpanded,
                        onDismissRequest = { tokenMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("${s.inputTokens}: ${globalTotals.promptTokens}") },
                            onClick = {},
                        )
                        DropdownMenuItem(
                            text = { Text("${s.outputTokens}: ${globalTotals.completionTokens}") },
                            onClick = {},
                        )
                        DropdownMenuItem(
                            text = { Text("${s.totalTokens}: ${globalTotals.total}") },
                            onClick = {},
                        )
                    }
                }
                TextButton(onClick = onOpenSettings) { Text("⚙ ${s.settings}") }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.title,
            onConfirm = { newTitle ->
                onRename(target.id, newTitle)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    var draft by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.renameTitle) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
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
