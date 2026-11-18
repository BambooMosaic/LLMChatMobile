package com.bamboomosaic.llmchat.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** DeepSeek 风格底部输入栏：大圆角容器、多行自适应、文件上传、深度思考、模型切换、圆形发送。 */
@Composable
fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    thinkingEnabled: Boolean,
    thinkingSupported: Boolean = true,
    onToggleThinking: (Boolean) -> Unit,
    currentModel: String,
    modelConfigs: List<ApiConfig>,
    onSelectModel: (Long) -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit,
    onSend: () -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var readError by remember { mutableStateOf<String?>(null) }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                readError = s.requestFailed
            } else {
                readError = null
                onInputChange(if (input.isBlank()) text else "$input\n$text")
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(s.inputPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    maxLines = 6,
                    // 键盘回车/发送键直接发送，不必去点被键盘挡住的圆形按钮
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 左侧：模型切换 + 深度思考（模型在深度思考左边）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            // 模型名按钮，点击展开配置列表
                            TextButton(
                                onClick = { modelMenuExpanded = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 0.dp,
                                ),
                            ) {
                                Text(
                                    text = currentModel.ifBlank { "—" },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            DropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false },
                            ) {
                                modelConfigs.forEach { config ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = config.name + "（" + config.model + "）",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        onClick = {
                                            onSelectModel(config.id)
                                            modelMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        FilterChip(
                            selected = thinkingEnabled,
                            onClick = { onToggleThinking(!thinkingEnabled) },
                            enabled = thinkingSupported,
                            label = { Text(s.thinking) },
                        )
                    }
                    // 右侧：上传文件 + 发送（上传按钮紧贴发送左边，参考 DeepSeek 布局）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { filePicker.launch(arrayOf("text/*", "application/json")) },
                        ) {
                            Text("📎", style = MaterialTheme.typography.titleMedium)
                        }
                        FilledIconButton(
                            onClick = if (isGenerating) onStop else onSend,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            colors = if (isGenerating) {
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                )
                            } else {
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors()
                            },
                        ) {
                            // 生成中显示「停止」方块，否则显示发送箭头
                            Text(
                                text = if (isGenerating) "■" else "↑",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
        readError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}
