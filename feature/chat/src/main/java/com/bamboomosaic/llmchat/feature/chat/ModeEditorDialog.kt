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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
 * 模式列表为圆角直排单选列表（radio + 标题 + 小白副标题），
 * 角色扮演模式双面板（角色卡表单 / 自由输入），底部按钮按场景区分。
 */
@Composable
fun ModeEditorDialog(
    initialModeId: String,
    initialUserPrompt: String?,
    /** 底部按钮文字：true=创建对话（新建场景），false=确定（编辑场景）。 */
    createMode: Boolean,
    /** 当前应用语言（模式文案三语）。 */
    uiLanguage: com.bamboomosaic.llmchat.core.domain.AppLanguage,
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

    val selected = ChatModes.find(selectedId, uiLanguage)
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
                // 折叠下拉触发框（显示当前选中模式）
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownOpen = !dropdownOpen },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selected.localized(uiLanguage).modeName +
                                    if (selected.modeId == ModeConfig.LEGACY) {
                                        if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) " (not recommended)" else "（不推荐）"
                                    } else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = selected.localized(uiLanguage).description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = if (dropdownOpen) "▾" else "▸",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // 独立弹出菜单（锚定触发框下方，radio + 标题 + 副标题，圆角，宽度与弹窗内容一致）
                androidx.compose.foundation.layout.BoxWithConstraints {
                    val menuWidth = maxWidth
                    androidx.compose.material3.DropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(menuWidth),
                    ) {
                        ChatModes.all.forEach { mode ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.RadioButton(
                                            selected = selectedId == mode.modeId,
                                            onClick = { selectedId = mode.modeId },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                            ),
                                        )
                                        Column {
                                            Text(
                                                text = mode.localized(uiLanguage).modeName +
                                        if (mode.modeId == ModeConfig.LEGACY) {
                                            if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) " (not recommended)" else "（不推荐）"
                                        } else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            Text(
                                                text = mode.localized(uiLanguage).description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedId = mode.modeId
                                    dropdownOpen = false
                                },
                            )
                        }
                    }
                }

                // 用户层输入：allowUserPrompt=false 的模式（ds_web）整块隐藏
                if (selected.allowUserPrompt) {
                // 角色扮演：双面板切换
                if (isRoleplay) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChipCompact(
                            selected = useRoleCard,
                            onClick = { useRoleCard = true },
                            label = if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Use role card" else "使用角色卡",
                        )
                        FilterChipCompact(
                            selected = !useRoleCard,
                            onClick = { useRoleCard = false },
                            label = if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Configure myself" else "我要自己配置",
                        )
                    }
                    if (useRoleCard) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cardName,
                            onValueChange = { cardName = it },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Character name" else "角色名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cardInfo,
                            onValueChange = { cardInfo = it },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Character info" else "角色信息") },
                            placeholder = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Personality, speaking style, backstory, catchphrases..." else "性格、说话风格、背景故事、口头禅等") },
                            minLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = freePrompt,
                            onValueChange = { freePrompt = it },
                            label = { Text(s.modeUserPromptLabel) },
                            placeholder = { Text(selected.localized(uiLanguage).userPromptPlaceholder) },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = freePrompt,
                        onValueChange = { freePrompt = it },
                        label = { Text(s.modeUserPromptLabel) },
                        placeholder = { Text(selected.localized(uiLanguage).userPromptPlaceholder) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                } // allowUserPrompt 收口

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
                    Text(if (createMode) s.createConversation else if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "OK" else "确定")
                }
            }
        }
    }
}

/** 紧凑 FilterChip（角色卡面板切换用）。 */
@Composable
private fun FilterChipCompact(selected: Boolean, onClick: () -> Unit, label: String) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    )
}
