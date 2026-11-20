package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings
import kotlinx.coroutines.delay

/**
 * 全屏首次启动引导：5 步，顶部常驻紫色标题，底部统一操作区。
 * 页面横向滑动过渡，元素入场渐移，完成页主按钮呼吸动效。
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    themeMode: ThemeMode,
    language: AppLanguage,
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }
    var direction by remember { mutableStateOf(1) }

    // 第 3 页双状态：guide / deepseek / custom
    var apiState by remember { mutableStateOf(ApiFormState.GUIDE) }
    var apiKey by remember { mutableStateOf("") }
    var customProvider by remember { mutableStateOf(Providers.CUSTOM) }
    var customBaseUrl by remember { mutableStateOf("") }
    var customModel by remember { mutableStateOf("") }
    var providerExpanded by remember { mutableStateOf(false) }

    val validateState by viewModel.validateState.collectAsState()
    val isConfigured by viewModel.isConfigured.collectAsState()
    val toastTick by viewModel.toastTick.collectAsState()

    // 验证结果 Toast（toastTick 保证重复失败也每次弹出）
    val toastText = when (validateState) {
        is ValidateState.Success -> s.configSuccess
        is ValidateState.Error -> (validateState as ValidateState.Error).message
        else -> null
    }
    LaunchedEffect(toastText, toastTick) {
        if (toastText != null) {
            android.widget.Toast.makeText(context, toastText, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun go(next: Int) {
        direction = if (next >= page) 1 else -1
        page = next
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        // 顶部常驻紫色标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 48.dp),
        ) {
            Text(
                text = "LLMChatMobile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 内容区（占满剩余空间）
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (direction > 0) {
                        (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) with
                            (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                    } else {
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) with
                            (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                    }
                },
                label = "onboardingPage",
            ) { targetPage ->
                FadeInContent {
                    when (targetPage) {
                        0 -> WelcomePage(s)
                        1 -> TestNoticePage(s)
                        2 -> ApiConfigPage(
                            s = s,
                            apiState = apiState,
                            apiKey = apiKey,
                            onApiKeyChange = { apiKey = it; viewModel.resetValidation() },
                            customProvider = customProvider,
                            customBaseUrl = customBaseUrl,
                            customModel = customModel,
                            providerExpanded = providerExpanded,
                            onProviderExpandedChange = { providerExpanded = it },
                            onProviderChange = { p ->
                                customProvider = p
                                val preset = Providers.find(p)
                                customBaseUrl = preset.baseUrl
                                customModel = preset.models.firstOrNull().orEmpty()
                                viewModel.resetValidation()
                            },
                            onBaseUrlChange = { customBaseUrl = it; viewModel.resetValidation() },
                            onModelChange = { customModel = it; viewModel.resetValidation() },
                            onModeChange = {
                                apiState = it
                                viewModel.resetValidation()
                            },
                            onGuideMode = { apiState = ApiFormState.GUIDE; viewModel.resetValidation() },
                            validateState = validateState,
                            isConfigured = isConfigured,
                            onValidate = {
                                val cfg = if (apiState == ApiFormState.DEEPSEEK) {
                                    viewModel.deepSeekConfig(apiKey)
                                } else {
                                    ApiConfig(
                                        name = Providers.find(customProvider).displayName,
                                        provider = customProvider,
                                        baseUrl = customBaseUrl.trim(),
                                        apiKey = apiKey.trim(),
                                        model = customModel.trim(),
                                        supportReasoning = Providers.find(customProvider).supportReasoning,
                                    )
                                }
                                viewModel.validateAndSave(cfg)
                            },
                            onNext = { apiState = ApiFormState.GUIDE; go(3) },
                        )
                        3 -> GuidePage(s)
                        4 -> DonePage(s, onFinish = {
                            viewModel.finish()
                            onFinish()
                        })
                    }
                }
            }
        }

        // 底部操作区（统一分层，第 3 页有自己的按钮布局）
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                when (page) {
                    0, 1, 3 -> Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { go(page + 1) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) { Text(s.next) }
                    }
                    2 -> {
                        if (apiState == ApiFormState.GUIDE) {
                            // 指引态：三按钮横排
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TextButton(
                                    onClick = { apiState = ApiFormState.DEEPSEEK },
                                    modifier = Modifier.weight(1f),
                                ) { Text(s.gotApiKey, textAlign = TextAlign.Center) }
                                TextButton(
                                    onClick = { apiState = ApiFormState.CUSTOM },
                                    modifier = Modifier.weight(1f),
                                ) { Text(s.useOtherProvider, textAlign = TextAlign.Center) }
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { viewModel.finish(); onFinish() },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(s.skip) }
                        }
                        // 表单态的按钮在内容区内（需要靠近输入框），这里不重复
                    }
                    // 4 完成页的按钮在内容区（呼吸动效），这里留空
                }
                // 第 3 页起跳过入口（指引态已有独立跳过，这里只给非指引态显示）
                if (page >= 2 && page != 4 && apiState != ApiFormState.GUIDE) {
                    TextButton(
                        onClick = { viewModel.finish(); onFinish() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(s.skip) }
                }
            }
        }
    }
}

enum class ApiFormState { GUIDE, DEEPSEEK, CUSTOM }

/** 元素入场渐移动效包装。 */
@Composable
private fun FadeInContent(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(400), label = "fadeIn")
    val offset by animateFloatAsState(targetValue = if (visible) 0f else 16f, animationSpec = tween(400), label = "slideIn")
    LaunchedEffect(Unit) { visible = true }
    Box(modifier = Modifier.fillMaxSize().alpha(alpha).graphicsLayer { translationY = offset }) {
        content()
    }
}

@Composable
private fun WelcomePage(s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconCircular(modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(32.dp))
        Text(s.welcomeTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(s.welcomeSubtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TestNoticePage(s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(s.about, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(20.dp))
        Text(s.testNotice, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("${s.developer}: BambooMosaic", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/BambooMosaic/LLMChatMobile"),
            )
            context.startActivity(intent)
        }) { Text(s.githubRepo) }
    }
}

@Composable
private fun GuidePage(s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(s.quickGuideTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
        MarkdownText(content = s.guideMarkdown, enableHorizontalScroll = false)
    }
}

@Composable
private fun DonePage(s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings, onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1.06f else 1f,
        animationSpec = tween(900),
        label = "breathe",
    )
    LaunchedEffect(Unit) {
        while (true) {
            visible = !visible
            delay(900)
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(s.allDone, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onFinish() },
            contentAlignment = Alignment.Center,
        ) {
            // Canvas 自绘箭头，保证在圆内绝对居中（文字字形基线会导致偏移）
            val arrowColor = MaterialTheme.colorScheme.onPrimary
            Canvas(modifier = Modifier.size(48.dp)) {
                val stroke = 5.dp.toPx()
                val y = size.height / 2f
                drawLine(
                    color = arrowColor,
                    start = Offset(size.width * 0.18f, y),
                    end = Offset(size.width * 0.74f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                val head = Path().apply {
                    moveTo(size.width * 0.58f, y - size.height * 0.20f)
                    lineTo(size.width * 0.80f, y)
                    lineTo(size.width * 0.58f, y + size.height * 0.20f)
                }
                drawPath(
                    path = head,
                    color = arrowColor,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/** 第 3 页：API 配置（指引态 / DeepSeek 快捷表单 / 自定义表单）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiConfigPage(
    s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings,
    apiState: ApiFormState,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    customProvider: String,
    customBaseUrl: String,
    customModel: String,
    providerExpanded: Boolean,
    onProviderExpandedChange: (Boolean) -> Unit,
    onProviderChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onModeChange: (ApiFormState) -> Unit,
    onGuideMode: () -> Unit,
    validateState: ValidateState,
    isConfigured: Boolean,
    onValidate: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        when (apiState) {
            ApiFormState.GUIDE -> {
                Text(s.configApiTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(16.dp))
                Text(s.apiGuideText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://platform.deepseek.com"),
                    )
                    context.startActivity(intent)
                }) { Text(s.goDeepSeek) }
                Spacer(Modifier.height(8.dp))
                Text(s.skipHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ApiFormState.DEEPSEEK -> {
                // 顶部返回箭头
                TextButton(onClick = onGuideMode) { Text("← ${s.backToGuide}") }
                Spacer(Modifier.height(8.dp))
                Text("DeepSeek", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("deepseek-flash", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text(s.apiKey) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                FormButtons(
                    s = s,
                    validateState = validateState,
                    isConfigured = isConfigured,
                    onValidate = onValidate,
                    onNext = onNext,
                )
            }
            ApiFormState.CUSTOM -> {
                TextButton(onClick = onGuideMode) { Text("← ${s.backToGuide}") }
                Spacer(Modifier.height(8.dp))
                Text(s.useOtherProvider, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(16.dp))
                // 供应商下拉
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { onProviderExpandedChange(it) },
                ) {
                    OutlinedTextField(
                        value = customProvider,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.provider) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { onProviderExpandedChange(false) },
                    ) {
                        Providers.all.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.displayName) },
                                onClick = {
                                    onProviderChange(preset.id)
                                    onProviderExpandedChange(false)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customBaseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text(s.baseUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customModel,
                    onValueChange = onModelChange,
                    label = { Text(s.model) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text(s.apiKey) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                FormButtons(
                    s = s,
                    validateState = validateState,
                    isConfigured = isConfigured,
                    onValidate = onValidate,
                    onNext = onNext,
                )
            }
        }
    }
}

@Composable
private fun FormButtons(
    s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings,
    validateState: ValidateState,
    isConfigured: Boolean,
    onValidate: () -> Unit,
    onNext: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onValidate, modifier = Modifier.weight(1f).height(48.dp), enabled = validateState !is ValidateState.Validating) {
            if (validateState is ValidateState.Validating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(s.verifyConfig)
            }
        }
        Button(onClick = onNext, modifier = Modifier.weight(1f).height(48.dp), enabled = isConfigured) {
            Text(s.next)
        }
    }
}
