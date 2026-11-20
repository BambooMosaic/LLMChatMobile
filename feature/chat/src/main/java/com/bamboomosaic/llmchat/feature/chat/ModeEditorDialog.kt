package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.ModeConfig
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 模式选择/编辑弹窗（新建对话 与 会话模式修改 共用）。
 * - 模式为折叠下拉菜单（主标题 + 小白副标题），选中后下方显示说明
 * - 角色扮演模式：使用角色卡（表单）/ 我要自己配置（自由输入）双面板切换，数据互相隔离
 * - 底部按钮：新建对话=「创建对话」；工具栏编辑=「确定」
 */
@Composable
fun ModeEditorDialog(
    initialModeId: String,
    initialUserPrompt: String?,
    /** 底部按钮文字：true=创建对话（新建场景），false=确定（编辑场景）。 */
    createMode: Boolean,
    /** 提交回调：modeId + 用户提示词 + 自定义参数（非自定义模式为 null）。 */
    onConfirm: (modeId: String, userPrompt: String?, temperature: Float?, topP: Float?) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    var selectedId by remember { mutableStateOf(initialModeId) }
    var dropdownOpen by remember { mutableStateOf(false) }

    // 两个面板的数据互相隔离：角色卡表单 vs 自由输入
    var cardName by remember { mutableStateOf("") }
    var cardInfo by remember { mutableStateOf("") }
    var freePrompt by remember { mutableStateOf(initialUserPrompt.orEmpty()) }
    var useRoleCard by remember { mutableStateOf(true) }

    var customTemp by remember { mutableStateOf(ChatModes.CUSTOM_DEFAULT_TEMPERATURE.toString()) }
    var customTopP by remember { mutableStateOf(ChatModes.CUSTOM_DEFAULT_TOP_P.toString()) }

    val selected = ChatModes.find(selectedId)
    val isRoleplay = selected.modeId == ModeConfig.ROLEPLAY

    // 最终提交的用户提示词：角色卡拼接 / 自由输入
    val effectiveUserPrompt = when {
        isRoleplay && useRoleCard -> {
            val name = cardName.trim()
            val info = cardInfo.trim()
            if (name.isEmpty() && info.isEmpty()) "" else "角色名：$name\n角色设定：\n$info"
        }
        else -> freePrompt.trim()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(s.modeSelectTitle, style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(12.dp))
                // 折叠下拉选择框
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownOpen = true },
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(selected.modeName + if (selected.modeId == ModeConfig.LEGACY) "（不推荐）" else "",
                             style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = selected.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ChatModes.all.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = mode.modeName + if (mode.modeId == ModeConfig.LEGACY) "（不推荐）" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                selectedId = mode.modeId
                                dropdownOpen = false
                            },
                        )
                    }
                }

                // 角色扮演：双面板切换
                if (isRoleplay) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = useRoleCard,
                            onClick = { useRoleCard = true },
                            label = { Text("使用角色卡") },
                        )
                        FilterChip(
                            selected = !useRoleCard,
                            onClick = { useRoleCard = false },
                            label = { Text("我要自己配置") },
                        )
                    }
                    if (useRoleCard) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cardName,
                            onValueChange = { cardName = it },
                            label = { Text("角色名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cardInfo,
                            onValueChange = { cardInfo = it },
                            label = { Text("角色信息") },
                            placeholder = { Text("性格、说话风格、背景故事、口头禅等") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = freePrompt,
                            onValueChange = { freePrompt = it },
                            label = { Text(s.modeUserPromptLabel) },
                            placeholder = { Text(selected.userPromptPlaceholder) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = freePrompt,
                        onValueChange = { freePrompt = it },
                        label = { Text(s.modeUserPromptLabel) },
                        placeholder = { Text(selected.userPromptPlaceholder) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 自定义模式：温度 / topP 输入
                if (selected.modeId == ModeConfig.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customTemp,
                            onValueChange = { customTemp = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(s.customTemperature) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customTopP,
                            onValueChange = { customTopP = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(s.customTopP) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val temp = customTemp.toFloatOrNull()
                        val topp = customTopP.toFloatOrNull()
                        onConfirm(
                            selectedId,
                            effectiveUserPrompt.takeIf { it.isNotBlank() },
                            if (selectedId == ModeConfig.CUSTOM) temp else null,
                            if (selectedId == ModeConfig.CUSTOM) topp else null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (createMode) s.createConversation else "确定")
                }
            }
        }
    }
}
