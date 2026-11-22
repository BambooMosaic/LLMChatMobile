package com.bamboomosaic.llmchat.feature.chat

import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

private enum class SettingsPage { HOME, API, PREFERENCES, APPEARANCE, LANGUAGE, OTHER, ARCHIVED, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    archivedConversations: List<Conversation>,
    apiConfigs: List<ApiConfig>,
    onSaveConfig: (ApiConfig) -> Unit,
    onValidateConfig: suspend (ApiConfig) -> Result<Unit>,
    onDeleteConfig: (Long) -> Unit,
    onSetDefaultConfig: (Long) -> Unit,
    onSetProvider: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetDefaultModeId: (String) -> Unit,
    onSetContextInjectionEnabled: (Boolean) -> Unit,
    onSetTokenSavingLevel: (Int) -> Unit,
    onSetSearchEnabled: (Boolean) -> Unit,
    onSetUserNickname: (String) -> Unit,
    onDownloadLatest: () -> Unit,
    onSetColorPreset: (String) -> Unit,
    onSetBackgroundImage: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetAutoCompressEnabled: (Boolean) -> Unit,
    onRestoreConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    // 20 轮：平板/横屏（宽屏）双栏布局；手机保持单栏
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    var page by remember { mutableStateOf(if (wide) SettingsPage.API else SettingsPage.HOME) }

    val title = when (page) {
        SettingsPage.HOME -> s.settings
        SettingsPage.API -> s.apiConfig
        SettingsPage.APPEARANCE -> s.appearance
        SettingsPage.LANGUAGE -> s.languageSetting
        SettingsPage.PREFERENCES -> s.preferencesSetting
        SettingsPage.OTHER -> s.otherSettings
        SettingsPage.ARCHIVED -> s.archivedConversations
        SettingsPage.ABOUT -> s.about
    }

    // 20 轮：页面内容统一抽出（按参数渲染，切页动画的双帧需要各自渲染各自的目标页）
    val pageContent: @Composable (SettingsPage) -> Unit = { p ->
        when (p) {
            SettingsPage.HOME -> HomePage(
                s = s,
                selected = if (wide) p else null,
                onApi = { page = SettingsPage.API },
                onAppearance = { page = SettingsPage.APPEARANCE },
                onLanguage = { page = SettingsPage.LANGUAGE },
                onPreferences = { page = SettingsPage.PREFERENCES },
                onOther = { page = SettingsPage.OTHER },
                onArchived = { page = SettingsPage.ARCHIVED },
                onAbout = { page = SettingsPage.ABOUT },
            )

            SettingsPage.API -> ApiConfigListPage(
                configs = apiConfigs,
                onSave = onSaveConfig,
                onValidateConfig = onValidateConfig,
                onDelete = onDeleteConfig,
                onSetDefault = onSetDefaultConfig,
            )

            SettingsPage.PREFERENCES -> PreferencesPage(
                settings = settings,
                onSetDefaultModeId = onSetDefaultModeId,
                onSetContextInjectionEnabled = onSetContextInjectionEnabled,
                onSetUserNickname = onSetUserNickname,
            )

                        SettingsPage.APPEARANCE -> AppearancePage(
                            themeMode = settings.themeMode,
                            onSetThemeMode = onSetThemeMode,
                            colorPreset = settings.colorPreset,
                            onSetColorPreset = onSetColorPreset,
                            backgroundImageUri = settings.backgroundImageUri,
                            onSetBackgroundImage = onSetBackgroundImage,
                        )

            SettingsPage.LANGUAGE -> LanguagePage(
                language = settings.language,
                onSetLanguage = onSetLanguage,
            )

            SettingsPage.OTHER -> OtherPage(
                settings = settings,
                onSetTokenSavingLevel = onSetTokenSavingLevel,
                onSetSearchEnabled = onSetSearchEnabled,
                onSetAutoCompressEnabled = onSetAutoCompressEnabled,
            )

            SettingsPage.ARCHIVED -> ArchivedPage(
                conversations = archivedConversations,
                onRestore = onRestoreConversation,
                onDelete = onDeleteConversation,
            )

            SettingsPage.ABOUT -> AboutPage(onDownloadLatest = onDownloadLatest)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            // 20 轮：高度固定为屏幕 75%——页面切换不再因内容高度变化而抖动
            modifier = Modifier
                .fillMaxWidth(if (wide) 0.85f else 0.94f)
                .fillMaxHeight(0.75f),
        ) {
            if (wide) {
                // 宽屏双栏：左侧一级菜单常驻，右侧二级内容
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = s.settings,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                        TextButton(onClick = onDismiss) { Text(s.cancel) }
                        Spacer(Modifier.width(8.dp))
                    }
                    HorizontalDivider()
                    Row(Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .width(210.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                        ) {
                            HomePage(
                                s = s,
                                selected = page.takeIf { it != SettingsPage.HOME },
                                onApi = { page = SettingsPage.API },
                                onAppearance = { page = SettingsPage.APPEARANCE },
                                onLanguage = { page = SettingsPage.LANGUAGE },
                                onPreferences = { page = SettingsPage.PREFERENCES },
                                onOther = { page = SettingsPage.OTHER },
                                onArchived = { page = SettingsPage.ARCHIVED },
                                onAbout = { page = SettingsPage.ABOUT },
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            pageContent(page)
                        }
                    }
                }
            } else {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        if (page != SettingsPage.HOME) {
                            TextButton(onClick = { page = SettingsPage.HOME }) { Text("←") }
                        } else {
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        // 二级页面只保留左上角「←」返回，不再显示「取消」，避免出现两个返回入口
                        if (page == SettingsPage.HOME) {
                            TextButton(onClick = onDismiss) { Text(s.cancel) }
                        } else {
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    HorizontalDivider()

                    // 20 轮：页面切换动画——新页从右滑入，旧页原地快速淡出（避免滑动残影）
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            val forward = targetState != SettingsPage.HOME && initialState == SettingsPage.HOME
                            if (forward) {
                                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                                    fadeOut(androidx.compose.animation.core.tween(durationMillis = 110))
                            } else {
                                (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                    fadeOut(androidx.compose.animation.core.tween(durationMillis = 110))
                            }
                        },
                        label = "settingsPage",
                    ) { target ->
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            pageContent(target)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings,
    selected: SettingsPage? = null,
    onApi: () -> Unit,
    onAppearance: () -> Unit,
    onLanguage: () -> Unit,
    onPreferences: () -> Unit,
    onOther: () -> Unit,
    onArchived: () -> Unit,
    onAbout: () -> Unit,
) {
    MenuRow(s.apiConfig, onApi, selected == SettingsPage.API)
    MenuRow(s.appearance, onAppearance, selected == SettingsPage.APPEARANCE)
    MenuRow(s.languageSetting, onLanguage, selected == SettingsPage.LANGUAGE)
    MenuRow(s.preferencesSetting, onPreferences, selected == SettingsPage.PREFERENCES)
    MenuRow(s.otherSettings, onOther, selected == SettingsPage.OTHER)
    MenuRow(s.archivedConversations, onArchived, selected == SettingsPage.ARCHIVED)
    MenuRow(s.about, onAbout, selected == SettingsPage.ABOUT)
}

/** 已归档会话：点右侧「恢复」放回侧边栏，或「删除」永久删除（二次确认）。 */
@Composable
private fun ArchivedPage(
    conversations: List<Conversation>,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }

    if (conversations.isEmpty()) {
        Text(
            text = s.noArchivedConversations,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }
    conversations.forEach { conversation ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onRestore(conversation.id) }) { Text(s.restore) }
            TextButton(onClick = { deleteTarget = conversation }) {
                Text(s.delete, color = MaterialTheme.colorScheme.error)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteConfirmTitle) },
            text = { Text(target.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text(s.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) }
            },
        )
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit, selected: Boolean = false) {
    val interaction = androidx.compose.foundation.interaction.MutableInteractionSource()
    Row(
        modifier = Modifier
            .pressScale(interaction, 0.985f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleLarge)
    }
}

/** API 配置列表页：新增 / 编辑 / 删除 / 设为默认。 */
@Composable
private fun ApiConfigListPage(
    configs: List<ApiConfig>,
    onSave: (ApiConfig) -> Unit,
    onValidateConfig: suspend (ApiConfig) -> Result<Unit>,
    onDelete: (Long) -> Unit,
    onSetDefault: (Long) -> Unit,
) {
    val s = LocalAppStrings.current
    var editing by remember { mutableStateOf<ApiConfig?>(null) }
    var adding by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<ApiConfig?>(null) }
    var deleteTarget by remember { mutableStateOf<ApiConfig?>(null) }

    Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
        Text(s.addConfig)
    }

    Spacer(Modifier.height(12.dp))

    configs.forEach { config ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (config.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = s.defaultLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = config.model,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { menuTarget = config }) { Text("⋯") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    if (configs.isEmpty()) {
        Text(
            text = s.noConfig,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }

    // 每项的操作菜单
    menuTarget?.let { config ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { menuTarget = null },
        ) {
            DropdownMenuItem(
                text = { Text(s.edit) },
                onClick = { editing = config; menuTarget = null },
            )
            if (!config.isDefault) {
                DropdownMenuItem(
                    text = { Text(s.setDefault) },
                    onClick = { onSetDefault(config.id); menuTarget = null },
                )
            }
            DropdownMenuItem(
                text = { Text(s.delete, color = MaterialTheme.colorScheme.error) },
                onClick = { deleteTarget = config; menuTarget = null },
            )
        }
    }

    // 新增 / 编辑弹窗
    if (adding || editing != null) {
        ConfigEditDialog(
            initial = editing,
            onValidateConfig = onValidateConfig,
            onSave = { draft ->
                onSave(draft)
                adding = false
                editing = null
            },
            onDismiss = {
                adding = false
                editing = null
            },
        )
    }

    // 删除确认
    deleteTarget?.let { config ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteConfirmTitle) },
            text = { Text(config.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(config.id)
                    deleteTarget = null
                }) { Text(s.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) }
            },
        )
    }
}

/** 配置编辑弹窗：保留供应商联动逻辑。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigEditDialog(
    initial: ApiConfig?,
    onSave: (ApiConfig) -> Unit,
    onValidateConfig: suspend (ApiConfig) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    // 新建配置默认 DeepSeek + deepseek-flash（新手友好）；编辑已有配置时用原值
    val defaultProvider = "deepseek"
    val defaultPreset = Providers.find(defaultProvider)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var provider by remember { mutableStateOf(initial?.provider ?: defaultProvider) }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: defaultPreset.baseUrl) }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "deepseek-flash") }
    var supportReasoning by remember {
        mutableStateOf(initial?.supportReasoning ?: defaultPreset.supportReasoning)
    }

    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    // 高级选项：留空 = 不限（用全局默认）
    var advExpanded by remember { mutableStateOf(false) }
    var maxContext by remember(initial) {
        mutableStateOf(initial?.maxContextTokens?.toString().orEmpty())
    }
    var maxOutput by remember(initial) {
        mutableStateOf(initial?.maxOutputTokens?.toString().orEmpty())
    }
    val preset = Providers.find(provider)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun buildConfig() = ApiConfig(
        id = initial?.id ?: 0L,
        name = name.ifBlank { model.ifBlank { "未命名" } },
        provider = provider,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        supportReasoning = supportReasoning,
        isDefault = initial?.isDefault ?: false,
        maxContextTokens = maxContext.toIntOrNull(),
        maxOutputTokens = maxOutput.toIntOrNull(),
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp),
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(s.editConfig, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(4.dp))
                // 表单区域可滚动
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.configName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = it },
                ) {
                    OutlinedTextField(
                        value = if (preset.id == Providers.CUSTOM) s.custom else preset.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.provider) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                    ) {
                        Providers.all.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(if (p.id == Providers.CUSTOM) s.custom else p.displayName) },
                                onClick = {
                                    provider = p.id
                                    if (p.baseUrl.isNotEmpty()) baseUrl = p.baseUrl
                                    if (p.models.isNotEmpty()) model = p.models.first()
                                    supportReasoning = p.supportReasoning
                                    providerExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(s.baseUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(s.apiKey) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                if (preset.models.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = model,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(s.model) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false },
                        ) {
                            preset.models.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = { model = m; modelExpanded = false },
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(s.modelHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 高级选项：支持思考 + 最大上下文/最大输出（仅对本配置生效）
                TextButton(onClick = { advExpanded = !advExpanded }) {
                    Text((if (advExpanded) "▾ " else "▸ ") + s.advancedOptions)
                }
                if (advExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(s.supportReasoning, style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = supportReasoning, onCheckedChange = { supportReasoning = it })
                    }
                    OutlinedTextField(
                        value = maxContext,
                        onValueChange = { maxContext = it.filter { c -> c.isDigit() } },
                        label = { Text(s.maxContext) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = maxOutput,
                        onValueChange = { maxOutput = it.filter { c -> c.isDigit() } },
                        label = { Text(s.maxOutput) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                } // 滚动表单区结束

                // 底部固定操作区（阴影悬浮，始终可见）
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    // 验证配置：发极短测试请求，成功自动保存，失败提示具体错误（与引导页一致）
                    if (validating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    validating = true
                                    val result = onValidateConfig(buildConfig())
                                    validating = false
                                    val msg = result.fold(
                                        onSuccess = { s.configSuccess },
                                        onFailure = { e ->
                                            e.message?.takeIf { it.isNotBlank() } ?: s.configFail
                                        },
                                    )
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    if (result.isSuccess) {
                                        onSave(buildConfig())
                                        onDismiss()
                                    }
                                }
                            },
                        ) { Text(s.verifyConfig) }
                    }
                    TextButton(onClick = onDismiss) { Text(s.cancel) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(buildConfig()) }) { Text(s.save) }
                    }
                }
            }
        }
    }
}

/** 其他设置：联网搜索、省 Token 档位、自动压缩上下文。 */
@Composable
private fun OtherPage(
    settings: AppSettings,
    onSetTokenSavingLevel: (Int) -> Unit,
    onSetSearchEnabled: (Boolean) -> Unit,
    onSetAutoCompressEnabled: (Boolean) -> Unit,
) {
    val s = LocalAppStrings.current

    Spacer(Modifier.height(16.dp))
    // 19 轮：联网搜索开关（DeepSeek 原生；其他模型经 DeepSeek-Flash 代理）
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(s.searchSettingLabel, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = settings.searchEnabled, onCheckedChange = { onSetSearchEnabled(it) })
    }
    Text(
        text = s.searchProxyHint,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(16.dp))
    // 21 轮：省 Token 档位改为 5 档可滑动滑块（终极档警示特效）
    Text(s.tokenSavingLabel, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    val savingLevels = listOf(
        s.tokenSavingOff,
        s.tokenSavingQuality,
        s.tokenSavingMedium,
        s.tokenSavingMax,
        s.tokenSavingUltimate,
    )
    val ultimateActive = settings.tokenSavingLevel == 4
    LevelSlider(
        levels = savingLevels,
        selectedIndex = settings.tokenSavingLevel,
        onSelect = { onSetTokenSavingLevel(it) },
        activeColor = if (ultimateActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        trackColor = if (ultimateActive) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = if (ultimateActive) s.tokenSavingUltimateWarning
        else listOf(s.tokenSavingOffHint, s.tokenSavingQualityHint, s.tokenSavingMediumHint, s.tokenSavingMaxHint)
            .getOrNull(settings.tokenSavingLevel).orEmpty(),
        style = MaterialTheme.typography.labelSmall,
        color = if (ultimateActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(s.autoCompress, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = settings.autoCompressEnabled,
            onCheckedChange = onSetAutoCompressEnabled,
        )
    }
}

/** 偏好设置：环境注入开关、用户称呼、新对话默认模式。 */
@Composable
private fun PreferencesPage(
    settings: AppSettings,
    onSetDefaultModeId: (String) -> Unit,
    onSetContextInjectionEnabled: (Boolean) -> Unit,
    onSetUserNickname: (String) -> Unit,
) {
    val s = LocalAppStrings.current

    // 自动注入环境信息开关（ds_web 强制开启，无视此开关）
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(s.contextInjectionLabel, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = settings.contextInjectionEnabled, onCheckedChange = { onSetContextInjectionEnabled(it) })
    }
    Text(
        text = s.contextInjectionHint,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))
    // 19 轮：用户称呼（可选，DS 网页版模式不注入）
    OutlinedTextField(
        value = settings.userNickname,
        onValueChange = { onSetUserNickname(it) },
        label = { Text(s.userNicknameLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(16.dp))
    // 新对话默认模式：折叠下拉菜单（与新建对话弹窗 UI 一致）
    Spacer(Modifier.height(4.dp))
    Text(s.defaultModeLabel, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    var modeDropdownOpen by remember { mutableStateOf(false) }
    val currentDefaultMode = ChatModes.all.firstOrNull { it.modeId == settings.defaultModeId }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { modeDropdownOpen = true },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = settings.defaultModeId.takeIf { it != "off" }
                        ?.let { id -> ChatModes.find(id, settings.language).localized(settings.language).modeName }
                        ?: s.defaultModeOff,
                    style = MaterialTheme.typography.bodyMedium,
                )
                currentDefaultMode?.let {
                    Text(
                        text = it.localized(settings.language).description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (modeDropdownOpen) "▾" else "▸",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    androidx.compose.foundation.layout.BoxWithConstraints {
        val menuWidth = maxWidth
        DropdownMenu(
            expanded = modeDropdownOpen,
            onDismissRequest = { modeDropdownOpen = false },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(menuWidth),
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(s.defaultModeOff, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "新建时手动选择模式",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = {
                    onSetDefaultModeId("off")
                    modeDropdownOpen = false
                },
            )
            ChatModes.all.forEach { mode ->
                val loc = mode.localized(settings.language)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = loc.modeName + if (mode.modeId == com.bamboomosaic.llmchat.core.domain.ModeConfig.LEGACY) {
                                    if (settings.language == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) " (not recommended)" else "（不推荐）"
                                } else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = loc.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSetDefaultModeId(mode.modeId)
                        modeDropdownOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AppearancePage(
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    colorPreset: String,
    onSetColorPreset: (String) -> Unit,
    backgroundImageUri: String,
    onSetBackgroundImage: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    val options = listOf(
        ThemeMode.SYSTEM to s.followSystem,
        ThemeMode.LIGHT to s.lightMode,
        ThemeMode.DARK to s.darkMode,
    )
    options.forEach { (mode, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSetThemeMode(mode) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = themeMode == mode, onClick = { onSetThemeMode(mode) })
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }

    Spacer(Modifier.height(16.dp))
    // 21 轮：UI 配色预设（按当前深浅分组，避开主紫色系）
    Text(s.colorPresetLabel, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    val isLightGroup = themeMode == ThemeMode.LIGHT ||
        (themeMode == ThemeMode.SYSTEM && !androidx.compose.foundation.isSystemInDarkTheme())
    val presets = if (isLightGroup) {
        listOf(
            Triple("default", s.presetDefault, Color(0xFFF2F2F2)),
            Triple("blue", s.presetBlue, Color(0xFFDCEAFB)),
            Triple("green", s.presetGreen, Color(0xFFD8F0EB)),
            Triple("warm", s.presetWarm, Color(0xFFF5E9D5)),
        )
    } else {
        listOf(
            Triple("default", s.presetDefault, Color(0xFF272727)),
            Triple("blue", s.presetBlue, Color(0xFF16213A)),
            Triple("green", s.presetGreen, Color(0xFF16241C)),
            Triple("warm", s.presetWarm, Color(0xFF2A2016)),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { (id, label, swatch) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSetColorPreset(id) }
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(swatch)
                        .border(
                            width = if (colorPreset == id) 2.dp else 1.dp,
                            color = if (colorPreset == id) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp),
                        ),
                )
                Spacer(Modifier.height(2.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    // 21 轮：聊天背景图（复制到应用私有目录持久化；清除即恢复默认背景）
    Text(s.backgroundLabel, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    val context = androidx.compose.ui.platform.LocalContext.current
    val pickImage = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val dest = java.io.File(context.filesDir, "chat_background.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                onSetBackgroundImage(dest.absolutePath)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Button(
            onClick = { pickImage.launch("image/*") },
        ) {
            Text(s.pickImage)
        }
        if (backgroundImageUri.isNotBlank()) {
            androidx.compose.material3.OutlinedButton(onClick = { onSetBackgroundImage("") }) {
                Text(s.clearImage)
            }
        }
    }
}

@Composable
private fun LanguagePage(
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit,
) {
    val s = LocalAppStrings.current
    val options = listOf(
        AppLanguage.SIMPLIFIED_CHINESE to s.simplifiedChinese,
        AppLanguage.TRADITIONAL_CHINESE to s.traditionalChinese,
        AppLanguage.ENGLISH to s.english,
    )
    options.forEach { (lang, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSetLanguage(lang) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = language == lang, onClick = { onSetLanguage(lang) })
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** 从 PackageInfo 读取 versionName，禁止硬编码；去掉 alpha 后面的序号（如 0.1.0-alpha01 → 0.1.0-alpha）。 */
@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        val raw = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.1.0"
        // 形如 "0.1.0-alpha01" / "0.1.0-beta02" 时去掉 alpha/beta 后的数字
        raw.replace(Regex("(alpha|beta|rc)(\\d+)$", RegexOption.IGNORE_CASE)) { m ->
            m.groupValues[1]
        }
    }
}

/** 关于内容（关于页与欢迎弹窗共用）。 */
@Composable
private fun AboutContent(versionName: String) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    var showCredits by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Text(s.appName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${s.versionLabel}: $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = s.testNotice,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${s.developer}: BambooMosaic",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = s.githubRepo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/BambooMosaic/LLMChatMobile"),
                )
                runCatching { context.startActivity(intent) }
            },
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { showCredits = true }) {
            Text(s.creditsButton)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showCredits) {
        CreditsDialog(onDismiss = { showCredits = false })
    }
}

/** 关于二级页面。 */
@Composable
private fun AboutPage(onDownloadLatest: () -> Unit = {}) {
    val versionName = rememberVersionName()
    val s = LocalAppStrings.current
    Column {
        AboutContent(versionName)
        Spacer(Modifier.height(12.dp))
        // 20 轮：下载最新安装包——后台下载最新发布版 APK，失败回退浏览器下载页
        Button(
            onClick = onDownloadLatest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(s.aboutDownload)
        }
    }
}

/** 开发者名单弹窗。 */
@Composable
private fun CreditsDialog(onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(s.creditsTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val entries = listOf(
                    s.creditsDev to "BambooMosaic",
                    s.creditsPlan to "BambooMosaic",
                    s.creditsMainCode to "DeepSeek-Flash；DeepSeek-V4-pro；GLM-5.3-Flash；Hy4",
                    s.creditsImpl to "DeepSeek-Flash；DeepSeek-V4-pro；GLM-5.3-Flash",
                    s.creditsUiDesign to "BambooMosaic；DeepSeek-Flash",
                    s.creditsMentor to "DeepSeek网页版",
                    s.creditsGithub to "BambooMosaic",
                )
                entries.forEach { (role, names) ->
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = names,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(4.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("OK")
                }
            }
        }
    }
}
