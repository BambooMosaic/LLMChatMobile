package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

private enum class SettingsPage { HOME, API, APPEARANCE, LANGUAGE, OTHER, ARCHIVED, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    archivedConversations: List<Conversation>,
    apiConfigs: List<ApiConfig>,
    onSaveConfig: (ApiConfig) -> Unit,
    onDeleteConfig: (Long) -> Unit,
    onSetDefaultConfig: (Long) -> Unit,
    onSetProvider: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetModel: (String) -> Unit,
    onSetSystemPrompt: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetMaxContextTokens: (Int) -> Unit,
    onSetMaxOutputTokens: (Int) -> Unit,
    onSetAutoCompressEnabled: (Boolean) -> Unit,
    onRestoreConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    var page by remember { mutableStateOf(SettingsPage.HOME) }

    val title = when (page) {
        SettingsPage.HOME -> s.settings
        SettingsPage.API -> s.apiConfig
        SettingsPage.APPEARANCE -> s.appearance
        SettingsPage.LANGUAGE -> s.languageSetting
        SettingsPage.OTHER -> s.otherSettings
        SettingsPage.ARCHIVED -> s.archivedConversations
        SettingsPage.ABOUT -> s.about
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp),
        ) {
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

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    when (page) {
                        SettingsPage.HOME -> HomePage(
                            s = s,
                            onApi = { page = SettingsPage.API },
                            onAppearance = { page = SettingsPage.APPEARANCE },
                            onLanguage = { page = SettingsPage.LANGUAGE },
                            onOther = { page = SettingsPage.OTHER },
                            onArchived = { page = SettingsPage.ARCHIVED },
                            onAbout = { page = SettingsPage.ABOUT },
                        )

                        SettingsPage.API -> ApiConfigListPage(
                            configs = apiConfigs,
                            onSave = onSaveConfig,
                            onDelete = onDeleteConfig,
                            onSetDefault = onSetDefaultConfig,
                        )

                        SettingsPage.APPEARANCE -> AppearancePage(
                            themeMode = settings.themeMode,
                            onSetThemeMode = onSetThemeMode,
                        )

                        SettingsPage.LANGUAGE -> LanguagePage(
                            language = settings.language,
                            onSetLanguage = onSetLanguage,
                        )

                        SettingsPage.OTHER -> OtherPage(
                            settings = settings,
                            onSetSystemPrompt = onSetSystemPrompt,
                            onSetMaxContextTokens = onSetMaxContextTokens,
                            onSetMaxOutputTokens = onSetMaxOutputTokens,
                            onSetAutoCompressEnabled = onSetAutoCompressEnabled,
                        )

                        SettingsPage.ARCHIVED -> ArchivedPage(
                            conversations = archivedConversations,
                            onRestore = onRestoreConversation,
                            onDelete = onDeleteConversation,
                        )

                        SettingsPage.ABOUT -> AboutPage()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    s: com.bamboomosaic.llmchat.feature.chat.i18n.AppStrings,
    onApi: () -> Unit,
    onAppearance: () -> Unit,
    onLanguage: () -> Unit,
    onOther: () -> Unit,
    onArchived: () -> Unit,
    onAbout: () -> Unit,
) {
    MenuRow(s.apiConfig, onApi)
    MenuRow(s.appearance, onAppearance)
    MenuRow(s.languageSetting, onLanguage)
    MenuRow(s.otherSettings, onOther)
    MenuRow(s.archivedConversations, onArchived)
    MenuRow(s.about, onAbout)
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
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
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
    val preset = Providers.find(provider)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(s.editConfig, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(s.supportReasoning, style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = supportReasoning, onCheckedChange = { supportReasoning = it })
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancel) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(
                            ApiConfig(
                                id = initial?.id ?: 0L,
                                name = name.ifBlank { model.ifBlank { "未命名" } },
                                provider = provider,
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                model = model,
                                supportReasoning = supportReasoning,
                                isDefault = initial?.isDefault ?: false,
                            ),
                        )
                    }) { Text(s.save) }
                }
            }
        }
    }
}

/** 其他设置：系统提示词、最大上下文、最大输出、自动压缩上下文。 */
@Composable
private fun OtherPage(
    settings: AppSettings,
    onSetSystemPrompt: (String) -> Unit,
    onSetMaxContextTokens: (Int) -> Unit,
    onSetMaxOutputTokens: (Int) -> Unit,
    onSetAutoCompressEnabled: (Boolean) -> Unit,
) {
    val s = LocalAppStrings.current

    var prompt by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    OutlinedTextField(
        value = prompt,
        onValueChange = { prompt = it; onSetSystemPrompt(it) },
        label = { Text(s.systemPrompt) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))
    var maxContext by remember(settings.maxContextTokens) {
        mutableStateOf(settings.maxContextTokens.toString())
    }
    OutlinedTextField(
        value = maxContext,
        onValueChange = {
            maxContext = it.filter { c -> c.isDigit() }
            maxContext.toIntOrNull()?.let(onSetMaxContextTokens)
        },
        label = { Text(s.maxContext) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))
    var maxOutput by remember(settings.maxOutputTokens) {
        mutableStateOf(settings.maxOutputTokens.toString())
    }
    OutlinedTextField(
        value = maxOutput,
        onValueChange = {
            maxOutput = it.filter { c -> c.isDigit() }
            maxOutput.toIntOrNull()?.let(onSetMaxOutputTokens)
        },
        label = { Text(s.maxOutput) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(8.dp))
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

@Composable
private fun AppearancePage(
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
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
        Spacer(Modifier.height(8.dp))
    }
}

/** 关于二级页面。 */
@Composable
private fun AboutPage() {
    val versionName = rememberVersionName()
    AboutContent(versionName)
}
