package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.ChatMessage
import com.bamboomosaic.llmchat.core.domain.ModeConfig
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 18 轮新增：灰色模式（首条消息后锁定置灰的模式按钮）只读预览弹窗。
 * 展示当前模式绑定的用户层提示词与角色卡内容（角色扮演模式才有角色卡）；
 * 支持复制全文，禁止编辑与保存。
 */
@Composable
fun ModePreviewDialog(
    modeId: String,
    modeName: String,
    userPrompt: String?,
    uiLanguage: AppLanguage,
    /** 19 轮：当前会话已完成的消息（导出对话记录用）。 */
    messages: List<ChatMessage> = emptyList(),
    /** 19 轮：括号开关当前值（导出用）。 */
    roleplayParenRule: Boolean = true,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current
    val isRoleplay = modeId == ModeConfig.ROLEPLAY
    // 角色扮演：解析保存格式「角色名：X\n角色设定：\nY」，分小节展示
    val parsedCard = remember(userPrompt) { parseRoleCard(userPrompt?.trim().orEmpty()) }
    val hasCard = isRoleplay && (parsedCard.first.isNotEmpty() || parsedCard.second.isNotEmpty())
    val copyText = if (hasCard) {
        "角色名：${parsedCard.first}\n角色设定：\n${parsedCard.second}"
    } else {
        userPrompt?.trim().orEmpty()
    }
    var copied by remember { mutableStateOf(false) }

    // —— 19 轮：导出当前模式配置（用户层提示词/角色卡 + 括号开关 + 对话记录） ——
    val screenScope = rememberCoroutineScope()
    var pendingExportJson by remember { mutableStateOf("") }
    fun launchExport() {
        screenScope.launch {
            val path = DesktopFilePicker.pickSavePath("导出模式", "llmchat-mode.json") ?: return@launch
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { java.io.File(path).writeText(pendingExportJson) }
            }
        }
    }

    fun buildExportPayload(): String {
        val obj = org.json.JSONObject()
        obj.put("app", "llmchat-mode")
        obj.put("version", 2)
        obj.put("modeId", modeId)
        obj.put("modeName", modeName)
        obj.put("userPrompt", copyText)
        obj.put("roleplayParenRule", roleplayParenRule)
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

    // 30 轮：禁用全屏窗口——blurBehind 模糊只落在窗口（卡片）矩形内，不再全屏雾化
    Dialog(onDismissRequest = onDismiss) {
        GlassDialogShell(
            modifier = Modifier
                .fillMaxWidth()
                .wideDialogMax(520.dp),
                // 30 轮：全屏窗口时代的 padding(16) 已不需要——窗口本身收缩为卡片矩形
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = "${s.modePreviewTitle} · $modeName",
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(Modifier.height(12.dp))
                if (hasCard) {
                    Text(
                        text = s.modePreviewRoleCard,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("角色名：${parsedCard.first}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(parsedCard.second.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = s.modePreviewUserLayer,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(copyText.ifEmpty { "—" }, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(copyText))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (copied) s.modePreviewCopied else s.modePreviewCopy)
                }
                Spacer(Modifier.height(8.dp))
                // 19 轮：导出模式配置（含对话记录），保存为 JSON 文件
                OutlinedButton(
                    onClick = {
                        pendingExportJson = buildExportPayload()
                        launchExport()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.modeExport)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiLanguage == AppLanguage.ENGLISH) "Close" else "关闭")
                }
            }
        }
    }
}
