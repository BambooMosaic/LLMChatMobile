package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import com.bamboomosaic.llmchat.feature.chat.LocalGlassLevel

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
    /** 25 轮：已有消息的会话 id 集合（有消息时隐藏「编辑提示词」入口）。 */
    hasMessagesIds: Set<String> = emptySet(),
    onOpenTokenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    /** 20 轮：平板/横屏常驻模式，宽度固定且不随屏幕比例变化。 */
    permanent: Boolean = false,
) {
    val s = LocalAppStrings.current
    val screenWidthDp = rememberScreenWidthDp()
    val drawerWidth = if (permanent) 300.dp else (screenWidthDp * 2f / 3f).dp

    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var menuTarget by remember { mutableStateOf<Conversation?>(null) }
    // 32 轮修复：app 常驻跨午夜时「今天」分组不随日期刷新——时间锚点每分钟刷新，触发分组重算
    var groupingNow by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            groupingNow = System.currentTimeMillis()
        }
    }

    ModalDrawerSheet(
        // 桌面端第 4 轮：侧边栏直角（修复圆角外露白底）
        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        modifier = when {
            // 30 轮：毛玻璃档——完全实色（半透明会透出背景差异形成色差）+ 细边框 + 柔和外阴影。
            // 31 轮：阴影仅浅色预设启用——深色（6 个 dark 预设）下黑色阴影晕在深色背景上
            // 形成明显暗带，观感差，深色下跳过（用户实测反馈）
            LocalGlassLevel.current >= 2 -> {
                val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                Modifier.width(drawerWidth)
                    .then(
                        if (darkTheme) {
                            Modifier
                        } else {
                            Modifier.shadow(elevation = 12.dp, shape = MaterialTheme.shapes.large)
                        },
                    )
                    .border(
                        width = 0.7.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.large,
                    )
            }
            // 半透明档：恢复 0.7.3 原样——0.78 透底 + 细边框（无阴影）
            LocalGlassLevel.current == 1 -> Modifier.width(drawerWidth).border(
                width = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            )
            else -> Modifier.width(drawerWidth)
        },
        // 30 轮：毛玻璃档完全实色（半透明透背景必色差）；半透明档走 glassSurfaceColor
        // （0.85 透底，全局统一值；0.7.3 为 0.78，浅色主题下偏透看不清）
        drawerContainerColor = if (LocalGlassLevel.current == 1) {
            glassSurfaceColor()
        } else {
            MaterialTheme.colorScheme.surface
        },
        // 25 轮：玻璃模式 tonal elevation 归零——去掉色调叠加（半透明色不被混合），视觉上无阴影
        drawerTonalElevation = if (LocalGlassLevel.current >= 1) 0.dp
        else androidx.compose.material3.DrawerDefaults.ModalDrawerElevation,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = s.appName,
                    style = MaterialTheme.typography.titleLarge,
                    // 25 轮：显式 onSurface——玻璃半透明模式下 LocalContentColor 解析不稳定会变黑字
                    color = MaterialTheme.colorScheme.onSurface,
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
                // 32 轮：置顶 → 今天 → 7 天内 → 30 天内 → 更早。
                // updatedAt 即最近一轮对话时间（appendVersionedReply 每追加一条消息都会
                // dao.touch 刷新），所以很久之前的会话只要发一条消息就会自动上浮分组；
                // 空会话 updatedAt = 创建时间，按创建时间参与分组；DAO 已按 updatedAt 倒序，
                // 各组 filter 保序即组内倒序。
                val pinnedList = conversations.filter { it.pinned }
                val rest = conversations.filterNot { it.pinned }
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = groupingNow
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis
                val sevenDaysStart = todayStart - 7L * 24 * 60 * 60 * 1000
                val thirtyDaysStart = todayStart - 30L * 24 * 60 * 60 * 1000
                val todayList = rest.filter { it.updatedAt >= todayStart }
                val last7List = rest.filter { it.updatedAt >= sevenDaysStart && it.updatedAt < todayStart }
                val last30List = rest.filter { it.updatedAt >= thirtyDaysStart && it.updatedAt < sevenDaysStart }
                val earlierList = rest.filter { it.updatedAt < thirtyDaysStart }
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
                                .pointerInput(Unit) {
                                    // 桌面端第 3 轮：鼠标右键 = 移动端长按（同一套操作菜单）
                                    awaitEachGesture {
                                        val downEvent = awaitPointerEvent()
                                        if (downEvent.buttons.isSecondaryPressed) {
                                            downEvent.changes.forEach { it.consume() }
                                            menuTarget = conversation
                                        }
                                    }
                                }
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
                            // 25 轮：跟随玻璃档位——半透明容器 + 细边框 + 去投影；29 轮统一走 glassSurfaceColor
                            containerColor = glassSurfaceColor(),
                            shadowElevation = if (LocalGlassLevel.current >= 1) 0.dp else 8.dp,
                            border = if (LocalGlassLevel.current >= 1) {
                                androidx.compose.foundation.BorderStroke(
                                    width = 0.7.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            } else {
                                null
                            },
                        ) {
                            GlassPopupEffect()
                            DropdownMenuItem(
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                text = { Text(s.rename) },
                                onClick = {
                                    renameTarget = conversation
                                    menuTarget = null
                                },
                            )
                            // 24 轮：置顶 / 取消置顶
                            DropdownMenuItem(
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                text = { Text(if (conversation.pinned) s.unpinned else s.pinned) },
                                onClick = {
                                    onTogglePinned(conversation.id)
                                    menuTarget = null
                                },
                            )
                            // 25 轮：已有消息的会话禁止编辑提示词（空会话保留入口）
                            if (conversation.id !in hasMessagesIds) {
                                DropdownMenuItem(
                                    text = { Text(s.editPrompt) },
                                    onClick = {
                                        onEditPrompt(conversation.id)
                                        menuTarget = null
                                    },
                                )
                            }
                            DropdownMenuItem(
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                text = { Text(s.export) },
                                onClick = {
                                    onExport(conversation.id)
                                    menuTarget = null
                                },
                            )
                            // 23 轮：会话内搜索——过滤当前会话消息并高亮关键词
                            DropdownMenuItem(
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                text = { Text(s.searchInConversation) },
                                onClick = {
                                    onSearch(conversation.id)
                                    menuTarget = null
                                },
                            )
                            DropdownMenuItem(
                                colors = androidx.compose.material3.MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
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
                if (todayList.isNotEmpty()) {
                    item(key = "header-today") { SectionHeader(s.sectionToday) }
                    items(todayList, key = { it.id }) { conversationRow(it) }
                }
                if (last7List.isNotEmpty()) {
                    item(key = "header-7d") { SectionHeader(s.section7Days) }
                    items(last7List, key = { it.id }) { conversationRow(it) }
                }
                if (last30List.isNotEmpty()) {
                    item(key = "header-30d") { SectionHeader(s.section30Days) }
                    items(last30List, key = { it.id }) { conversationRow(it) }
                }
                if (earlierList.isNotEmpty()) {
                    item(key = "header-earlier") { SectionHeader(s.sectionEarlier) }
                    items(earlierList, key = { it.id }) { conversationRow(it) }
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
                    // 29 轮修正：柱状图图标（跟随配色预设主色），替代 📊 emoji
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TokenStatsIcon(Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.tokenSummary)
                    }
                }
                TextButton(onClick = onOpenSettings) { Text("⚙ ${s.settings}") }
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
    // 30 轮：换 GlassAlertDialog——卡片内轻毛玻璃，窗口外完全透明
    GlassAlertDialog(
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

/** 29 轮：Token 统计柱状图小图标（跟随配色预设主色）。 */
@Composable
private fun TokenStatsIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val barW = size.width / 5f
        val gap = barW * 0.75f
        listOf(0.45f, 0.7f, 1f).forEachIndexed { i, frac ->
            val barH = size.height * frac
            drawRoundRect(
                color = color,
                topLeft = Offset(gap / 2 + i * (barW + gap), size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 3f),
            )
        }
    }
}
