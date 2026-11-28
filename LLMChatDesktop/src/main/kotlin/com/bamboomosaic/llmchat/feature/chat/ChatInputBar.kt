package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import com.bamboomosaic.llmchat.feature.chat.LocalGlassLevel
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
    searchEnabled: Boolean = false,
    searchSupported: Boolean = false,
    onToggleSearch: (Boolean) -> Unit = {},
    currentModel: String,
    currentConfigId: Long = 0,
    modelConfigs: List<ApiConfig>,
    onSelectModel: (Long) -> Unit,
    onOpenApiSettings: () -> Unit = {},
    modeName: String? = null,
    modeLocked: Boolean = false,
    onOpenModeEditor: () -> Unit = {},
    isGenerating: Boolean,
    visionSupported: Boolean,
    pendingImages: List<String>,
    onImagesChange: (List<String>) -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
    /** 27 轮修正：上下文占用比例（发送键左侧缩小环形）。 */
    contextFraction: Float = 0f,
    /** 27 轮修正：点击环形打开统计面板。 */
    onOpenContextPanel: () -> Unit = {},
    /** 27 轮修正：平板/宽屏时环形改到下层功能栏搜索按钮左侧。 */
    wideScreen: Boolean = false,
) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var readError by remember { mutableStateOf<String?>(null) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var uploadMenuExpanded by remember { mutableStateOf(false) }
    var visionHint by remember { mutableStateOf(false) }

    // 桌面端：文件对话框选文本文件（模式/提示词导入）
    fun pickAndReadTextFile() {
        scope.launch {
            val path = DesktopFilePicker.pickOpenPath("选择文本文件") ?: return@launch
            val text = withContext(Dispatchers.IO) {
                runCatching { java.io.File(path).readText() }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                readError = s.requestFailed
            } else {
                readError = null
                onInputChange(if (input.isBlank()) text else "$input\n$text")
            }
        }
    }

    // 29 轮：图片选择（桌面端：文件对话框，单次一张；上限 4 张逻辑保留）
    fun pickImages() {
        scope.launch {
            val room = MAX_PENDING_IMAGES - pendingImages.size
            if (room <= 0) {
                DesktopToast.show(s.imageLimitHint)
                return@launch
            }
            val path = DesktopFilePicker.pickImagePath("选择图片") ?: return@launch
            val encoded = withContext(Dispatchers.IO) {
                listOfNotNull(compressImageToBase64(path))
            }
            if (encoded.isEmpty()) {
                readError = s.requestFailed
            } else {
                readError = null
                onImagesChange((pendingImages + encoded).distinct().take(MAX_PENDING_IMAGES))
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        // 外层大圆角容器：包裹上层输入框 + 下层功能栏
        Surface(
            shape = RoundedCornerShape(24.dp),
            // 24 轮：实验性毛玻璃——输入栏半透明模拟（同窗口无真模糊）
            color = if (LocalGlassLevel.current >= 1) {
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
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
                            // 已选图片缩略图（带删除），放在输入框上方
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

                            TextField(
                                value = input,
                                onValueChange = onInputChange,
                                placeholder = {
                                    Text(s.inputPlaceholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                maxLines = 6,
                                // 31 轮：键盘右下角恢复默认换行（与补充提示词等多行输入一致），发送仅通过发送按钮
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                // 默认三行文本高度，内容增多时向上扩展（有上限）
                                // 桌面端快捷键：Ctrl+Enter 发送（Enter 保持换行）
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 84.dp, max = 165.dp)
                                    .padding(end = 52.dp)
                                    .onPreviewKeyEvent { e ->
                                        val isEnter = e.key == Key.Enter || e.key == Key.NumPadEnter
                                        if (isEnter && e.type == KeyEventType.KeyDown &&
                                            e.isCtrlPressed
                                        ) {
                                            onSend()
                                            true
                                        } else {
                                            false
                                        }
                                    },
                            )
                        }

                        // 27 轮修正：环形上下文统计（发送键左侧，直径=发送键 50%；>80% 橙、>95% 红）——宽屏时改在下层功能栏
                        if (!wideScreen) {
                        val ringTrack = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        val ringPrimary = when {
                            contextFraction >= 0.95f -> androidx.compose.ui.graphics.Color(0xFFE57373)
                            contextFraction >= 0.80f -> androidx.compose.ui.graphics.Color(0xFFFFB300)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 56.dp, bottom = 12.dp)
                                .size(20.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .clickable { onOpenContextPanel() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = ringTrack,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                                )
                                drawArc(
                                    color = ringPrimary,
                                    startAngle = -90f,
                                    sweepAngle = 360f * contextFraction,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                        } // !wideScreen

                        // 发送按钮：锚定在输入区最后一行右侧（BottomEnd），不随文字增加上移
                        // 20 轮：按压缩放反馈
                        val sendInteraction = androidx.compose.foundation.interaction.MutableInteractionSource()
                        FilledIconButton(
                            onClick = if (isGenerating) onStop else onSend,
                            shape = CircleShape,
                            interactionSource = sendInteraction,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .pressScale(sendInteraction, 0.92f)
                            .padding(end = 2.dp, bottom = 2.dp)
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
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                }

                // ===== 下层：功能操作栏——模型最左，功能按钮整体右对齐 =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. 模型按钮（最左）
                    Box {
                        TextButton(
                            onClick = { modelMenuExpanded = true },
                            modifier = Modifier.height(24.dp),
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
                            shape = RoundedCornerShape(16.dp),
                            containerColor = glassSurfaceColor(),
                            shadowElevation = if (LocalGlassLevel.current >= 1) 0.dp else 8.dp,
                            modifier = Modifier.widthIn(max = rememberScreenWidthDp().dp / 2), // 半屏宽
                        ) {
                            GlassPopupEffect()
                            // 双行条目：上行=模型名+价格标签，下行=配置名
                            modelConfigs.forEach { config ->
                                val tier = priceTierOf(config.model)
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (config.id == currentConfigId) "✓ " else "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Text(
                                                    text = config.model,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (tier != null) PriceTierLabel(tier, s)
                                            }
                                            Text(
                                                text = config.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectModel(config.id)
                                        modelMenuExpanded = false
                                    },
                                )
                            }
                            // 底部固定：跳转设置页 API 配置
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = s.configCustomModel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                onClick = {
                                    modelMenuExpanded = false
                                    onOpenApiSettings()
                                },
                            )
                        }
                    }

                    // 2. 模式按钮（显示当前会话模式名；首条消息后置灰，18 轮起点击改为只读预览）
                    Box {
                        TextButton(
                            onClick = onOpenModeEditor,
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (modeLocked) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = modeName ?: s.modeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // 27 轮修正：宽屏（平板）时环形上下文统计放在搜索按钮左侧
                    if (wideScreen) {
                        val ringTrack = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        val ringPrimary = when {
                            contextFraction >= 0.95f -> androidx.compose.ui.graphics.Color(0xFFE57373)
                            contextFraction >= 0.80f -> androidx.compose.ui.graphics.Color(0xFFFFB300)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .clickable { onOpenContextPanel() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = ringTrack,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                                )
                                drawArc(
                                    color = ringPrimary,
                                    startAngle = -90f,
                                    sweepAngle = 360f * contextFraction,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    // 3. 联网搜索开关（仅 DeepSeek 支持时可用）
                    FilterChip(
                        selected = searchEnabled,
                        onClick = { onToggleSearch(!searchEnabled) },
                        enabled = searchSupported,
                        modifier = Modifier,
                        label = { Text(s.searchShort, style = MaterialTheme.typography.labelSmall) },
                    )

                    Spacer(Modifier.width(2.dp))

                    // 4. 深度思考开关
                    FilterChip(
                        selected = thinkingEnabled,
                        onClick = { onToggleThinking(!thinkingEnabled) },
                        enabled = thinkingSupported,
                        modifier = Modifier,
                        label = { Text(s.thinkingShort, style = MaterialTheme.typography.labelSmall) },
                    )

                    Spacer(Modifier.width(2.dp))

                    // 5. 上传按钮（沿用 📎 emoji，点击弹出菜单选图片/文件）
                    Box {
                        TextButton(
                            onClick = { uploadMenuExpanded = true },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            // 29 轮修正：加号图标（跟随配色预设主色），替代 📎 emoji
                            Text(
                                text = "+",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        DropdownMenu(
                            expanded = uploadMenuExpanded,
                            onDismissRequest = { uploadMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = glassSurfaceColor(),
                            shadowElevation = if (LocalGlassLevel.current >= 1) 0.dp else 8.dp,
                        ) {
                            GlassPopupEffect()
                            DropdownMenuItem(
                                text = { Text(s.uploadImage) },
                                enabled = visionSupported,
                                onClick = {
                                    uploadMenuExpanded = false
                                    pickImages()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(s.attachFile) },
                                onClick = {
                                    uploadMenuExpanded = false
                                    pickAndReadTextFile()
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
                DesktopToast.show(s.visionUnsupported)
                visionHint = false
            }
        }
    }
}

/** 29 轮：单条消息最多附带图片数。 */
private const val MAX_PENDING_IMAGES = 4

/**
 * 29 轮：选图压缩——最长边压到 1600px（inSampleSize 粗压 + 精确缩放），JPEG 80 转 base64。
 * 多图时请求体不再失控；配合请求侧统一的 image/jpeg data URI 也更准确。
 */
private fun compressImageToBase64(path: String): String? = runCatching {
    val original = javax.imageio.ImageIO.read(java.io.File(path)) ?: return null
    // 最长边 1600 等比缩放（JPEG 80）——与手机端压缩策略一致
    val maxSide = 1600
    val w = original.width
    val h = original.height
    val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h))
    val nw = (w * scale).toInt().coerceAtLeast(1)
    val nh = (h * scale).toInt().coerceAtLeast(1)
    val scaled = if (nw != w || nh != h) {
        java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_RGB).also { img ->
            val g = img.createGraphics()
            g.drawImage(original, 0, 0, nw, nh, null)
            g.dispose()
        }
    } else {
        original
    }
    val out = java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(scaled, "jpeg", out)
    java.util.Base64.getEncoder().encodeToString(out.toByteArray())
}.getOrNull()

/** 把 base64 图片解码并展示为缩略图。 */
@Composable
private fun DecodedImage(base64: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64) {
        runCatching {
            val bytes = java.util.Base64.getDecoder().decode(base64)
            decodeImageBitmap(bytes)
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
        )
    }
}


/** 模型价格档位（按模型名精确匹配；未收录的模型不显示标签）。 */
private enum class PriceTier { CHEAP, MEDIUM, HIGH, VERY_HIGH }

private fun priceTierOf(model: String): PriceTier? {
    val m = model.trim().lowercase()
    return when (m) {
        "deepseek-flash", "glm-5.3-flash", "qwen-turbo" -> PriceTier.CHEAP
        "deepseek-v4-pro", "glm-5.3", "qwen-plus", "gpt-6-luna" -> PriceTier.MEDIUM
        "gpt-6.1-sol" -> PriceTier.HIGH
        "gpt-6-astra" -> PriceTier.VERY_HIGH
        else -> null
    }
}

/** 价格档位标签：带档位底色的小圆角文字（深浅主题用固定深底浅字保证对比度）。 */
@Composable
private fun PriceTierLabel(tier: PriceTier, s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings) {
    val (bg, fg) = when (tier) {
        PriceTier.CHEAP -> Color(0xFF2E7D32) to Color.White
        PriceTier.MEDIUM -> Color(0xFFF9A825) to Color(0xFF1C1B1F)
        PriceTier.HIGH -> Color(0xFFEF6C00) to Color.White
        PriceTier.VERY_HIGH -> Color(0xFFC62828) to Color.White
    }
    val label = when (tier) {
        PriceTier.CHEAP -> s.priceCheap
        PriceTier.MEDIUM -> s.priceMedium
        PriceTier.HIGH -> s.priceHigh
        PriceTier.VERY_HIGH -> s.priceVeryHigh
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** 桌面端字节图解码：ImageIO（JPEG/PNG/WebP）→ toComposeImageBitmap（CMP 公开 AWT 互操作）。 */
private fun decodeImageBitmap(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap? =
    runCatching { bytes.decodeToImageBitmap() }.getOrNull()
