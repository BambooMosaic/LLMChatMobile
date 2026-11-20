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
 * - 模式列表（单选，选中显示简介）
 * - 用户层提示词输入框（占位符随模式变化）
 * - 自定义模式额外显示温度/topP 参数
 * - 底部「创建对话」/确认按钮
 */
@Composable
fun ModeEditorDialog(
    initialModeId: String,
    initialUserPrompt: String?,
    /** 提交回调：modeId + 用户提示词 + 自定义参数（非自定义模式为 null）。 */
    onConfirm: (modeId: String, userPrompt: String?, temperature: Float?, topP: Float?) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    var selectedId by remember { mutableStateOf(initialModeId) }
    var userPrompt by remember { mutableStateOf(initialUserPrompt.orEmpty()) }
    var customTemp by remember { mutableStateOf(ChatModes.CUSTOM_DEFAULT_TEMPERATURE.toString()) }
    var customTopP by remember { mutableStateOf(ChatModes.CUSTOM_DEFAULT_TOP_P.toString()) }

    val selected = ChatModes.find(selectedId)

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
                ChatModes.all.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = mode.modeId }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedId == mode.modeId,
                            onClick = { selectedId = mode.modeId },
                        )
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
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    label = { Text(s.modeUserPromptLabel) },
                    placeholder = { Text(selected.userPromptPlaceholder) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

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
                            userPrompt.trim().takeIf { it.isNotBlank() },
                            if (selectedId == ModeConfig.CUSTOM) temp else null,
                            if (selectedId == ModeConfig.CUSTOM) topp else null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.createConversation)
                }
            }
        }
    }
}
