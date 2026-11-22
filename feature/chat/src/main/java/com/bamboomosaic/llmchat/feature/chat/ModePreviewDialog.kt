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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bamboomosaic.llmchat.core.domain.AppLanguage
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
