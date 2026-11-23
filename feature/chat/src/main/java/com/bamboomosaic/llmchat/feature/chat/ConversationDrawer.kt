package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import com.bamboomosaic.llmchat.feature.chat.LocalExperimentalGlass

/** 侧边栏：宽度为屏幕 2/3，顶部标题 + 新对话，列表，底部 Token 统计 + 设置。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationDrawer(
    conversations: List<Conversation>,
    currentId: String,
    onNewConversation: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onArchive: (String) -> Unit,
    onEditPrompt: (String) -> Unit,
    onExport: (String) -> Unit,
    /** 23 轮：会话内搜索（长按菜单入口）。 */
    onSearch: (String) -> Unit = {},
    /** 24 轮：置顶 / 取消置顶。 */
    onTogglePinned: (String) -> Unit = {},
    onOpenTokenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    /** 20 轮：平板/横屏常驻模式，宽度固定且不随屏幕比例变化。 */
    permanent: Boolean = false,
) {
    val s = LocalAppStrings.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val drawerWidth = if (permanent) 300.dp else (screenWidthDp * 2f / 3f).dp

    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var menuTarget by remember { mutableStateOf<Conversation?>(null) }

    ModalDrawerSheet(
        modifier = if (LocalExperimentalGlass.current) {
            // 24 轮：毛玻璃模式抽屉加细边框（无系统阴影可依赖）
            Modifier.width(drawerWidth).border(
                width = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            )
        } else {
            Modifier.width(drawerWidth)
        },
        drawerContainerColor = if (LocalExperimentalGlass.current) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
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
                // 24 轮：置顶组 + 普通组（DAO 已按 pinned DESC, updatedAt DESC 排序）
                val pinnedList = conversations.filter { it.pinned }
                val normalList = conversations.filter { !it.pinned }
                val conversationRow: @Composable (Conversation) -> Unit = { conversation ->
                    val selected = conversation.id == currentId
                    Box {
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
                                    // 长按弹出操作菜单：重命名 / 归档
                                    onLongClick = { menuTarget = conversation },
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
                        DropdownMenu(
                            expanded = menuTarget?.id == conversation.id,
                            onDismissRequest = { menuTarget = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text(s.rename) },
                                onClick = {
                                    renameTarget = conversation
                                    menuTarget = null
                                },
                            )
                            // 24 轮：置顶 / 取消置顶
                            DropdownMenuItem(
                                text = { Text(if (conversation.pinned) s.unpinned else s.pinned) },
                                onClick = {
                                    onTogglePinned(conversation.id)
                                    menuTarget = null
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(s.editPrompt) },
                                onClick = {
                                    onEditPrompt(conversation.id)
                                    menuTarget = null
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(s.export) },
                                onClick = {
                                    onExport(conversation.id)
                                    menuTarget = null
                                },
                            )
                            // 23 轮：会话内搜索——过滤当前会话消息并高亮关键词
                            DropdownMenuItem(
                                text = { Text(s.searchInConversation) },
                                onClick = {
                                    onSearch(conversation.id)
                                    menuTarget = null
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(s.archive) },
                                onClick = {
                                    onArchive(conversation.id)
                                    menuTarget = null
                                },
                            )
                        }
                    }
                }
                if (pinnedList.isNotEmpty()) {
                    item(key = "header-pinned") { SectionHeader(s.sectionPinned) }
                    items(pinnedList, key = { "p-" + it.id }) { conversationRow(it) }
                }
                if (normalList.isNotEmpty()) {
                    item(key = "header-all") { SectionHeader(s.sectionAll) }
                    items(normalList, key = { it.id }) { conversationRow(it) }
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
                TextButton(onClick = onOpenTokenStats) {
                    Text("📊 ${s.tokenSummary}")
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


/** 24 轮：侧边栏分组小标题。 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
    )
}
