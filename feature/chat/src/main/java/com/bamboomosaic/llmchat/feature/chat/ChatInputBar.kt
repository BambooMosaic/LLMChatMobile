package com.bamboomosaic.llmchat.feature.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** DeepSeek 风格底部输入栏：大圆角容器、多行自适应、文件上传、图片输入、深度思考、模型切换、圆形发送。 */
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
    visionSupported: Boolean,
    pendingImages: List<String>,
    onImagesChange: (List<String>) -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var readError by remember { mutableStateOf<String?>(null) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var uploadMenuExpanded by remember { mutableStateOf(false) }
    var visionHint by remember { mutableStateOf(false) }

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

    // 图片选择：单选一张图片，读成 base64
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val base64 = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    }
                }.getOrNull()
            }
            if (base64.isNullOrBlank()) {
                readError = s.requestFailed
            } else {
                readError = null
                onImagesChange(listOf(base64))
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        // 外层大圆角容器：包裹上层输入框 + 下层功能栏
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ===== 上层：输入框区域（中灰，比下层浅），顶部大圆角 =====
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = 14.dp, bottomEnd = 14.dp,
                    ),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    // 用 Box 让发送按钮浮在输入框内右侧，不挤压文字区宽度
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = input,
                                onValueChange = onInputChange,
                                placeholder = {
                                    Text(s.inputPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                maxLines = 6,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { onSend() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                // 右侧用 modifier 预留给悬浮发送按钮
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(end = 48.dp),
                            )

                            // 已选图片缩略图（带删除）
                            if (pendingImages.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    pendingImages.forEachIndexed { index, base64 ->
                                        Box {
                                            DecodedImage(
                                                base64 = base64,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .clickable {
                                                        onImagesChange(pendingImages.filterIndexed { i, _ -> i != index })
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "×",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 发送按钮：内嵌在输入框区域右侧（生成中变红色停止）
                        FilledIconButton(
                            onClick = if (isGenerating) onStop else onSend,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(40.dp),
                            colors = if (isGenerating) {
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                )
                            } else {
                                androidx.compose.material3.IconButtonDefaults.filledIconButtonColors()
                            },
                        ) {
                            Text(
                                text = if (isGenerating) "■" else "↑",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }

                // ===== 下层：功能操作栏（深灰），三项单行均匀排布 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // 1. 模型按钮
                    Box {
                        TextButton(
                            onClick = { modelMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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

                    // 2. 深度思考开关（沿用 FilterChip 边框样式）
                    FilterChip(
                        selected = thinkingEnabled,
                        onClick = { onToggleThinking(!thinkingEnabled) },
                        enabled = thinkingSupported,
                        label = { Text(s.thinking) },
                    )

                    // 3. 上传按钮（沿用 📎 emoji，点击弹出菜单选图片/文件）
                    Box {
                        TextButton(
                            onClick = { uploadMenuExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text("📎", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(
                            expanded = uploadMenuExpanded,
                            onDismissRequest = { uploadMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(s.uploadImage) },
                                enabled = visionSupported,
                                onClick = {
                                    uploadMenuExpanded = false
                                    imagePicker.launch("image/*")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(s.attachFile) },
                                onClick = {
                                    uploadMenuExpanded = false
                                    filePicker.launch(arrayOf("text/*", "application/json"))
                                },
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
        if (visionHint) {
            LaunchedEffect(Unit) {
                android.widget.Toast.makeText(context, s.visionUnsupported, android.widget.Toast.LENGTH_SHORT).show()
                visionHint = false
            }
        }
    }
}

/** 把 base64 图片解码并展示为缩略图。 */
@Composable
private fun DecodedImage(base64: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64) {
        runCatching {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
