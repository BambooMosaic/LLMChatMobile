package com.bamboomosaic.llmchat.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.Conversation
import com.bamboomosaic.llmchat.core.domain.Providers
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

private enum class SettingsPage { HOME, API, APPEARANCE, LANGUAGE, OTHER, ARCHIVED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettings,
    archivedConversations: List<Conversation>,
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
                    TextButton(onClick = onDismiss) { Text(s.cancel) }
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
                        )

                        SettingsPage.API -> ApiPage(
                            settings = settings,
                            onSetProvider = onSetProvider,
                            onSetBaseUrl = onSetBaseUrl,
                            onSetApiKey = onSetApiKey,
                            onSetModel = onSetModel,
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
                        )
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
) {
    MenuRow(s.apiConfig, onApi)
    MenuRow(s.appearance, onAppearance)
    MenuRow(s.languageSetting, onLanguage)
    MenuRow(s.otherSettings, onOther)
    MenuRow(s.archivedConversations, onArchived)
}

/** 已归档会话：点右侧「恢复」放回侧边栏。 */
@Composable
private fun ArchivedPage(
    conversations: List<Conversation>,
    onRestore: (String) -> Unit,
) {
    val s = LocalAppStrings.current
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onRestore(conversation.id) }) { Text(s.restore) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiPage(
    settings: AppSettings,
    onSetProvider: (String) -> Unit,
    onSetBaseUrl: (String) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetModel: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    val preset = Providers.find(settings.providerId)
    val providerLabel = if (preset.id == Providers.CUSTOM) s.custom else preset.displayName

    // 供应商下拉
    ExposedDropdownMenuBox(
        expanded = providerExpanded,
        onExpandedChange = { providerExpanded = it },
    ) {
        OutlinedTextField(
            value = providerLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(s.provider) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = providerExpanded,
            onDismissRequest = { providerExpanded = false },
        ) {
            Providers.all.forEach { p ->
                DropdownMenuItem(
                    text = { Text(if (p.id == Providers.CUSTOM) s.custom else p.displayName) },
                    onClick = {
                        onSetProvider(p.id)
                        if (p.baseUrl.isNotEmpty()) onSetBaseUrl(p.baseUrl)
                        if (p.models.isNotEmpty()) onSetModel(p.models.first())
                        providerExpanded = false
                    },
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    var baseUrl by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    OutlinedTextField(
        value = baseUrl,
        onValueChange = { baseUrl = it; onSetBaseUrl(it) },
        label = { Text(s.baseUrl) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it; onSetApiKey(it) },
        label = { Text(s.apiKey) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))
    // 预设供应商：模型下拉；自定义/Ollama：手动输入
    if (preset.models.isNotEmpty()) {
        var model by remember(settings.model) { mutableStateOf(settings.model) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false },
            ) {
                preset.models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m) },
                        onClick = {
                            model = m
                            onSetModel(m)
                            modelExpanded = false
                        },
                    )
                }
            }
        }
    } else {
        var model by remember(settings.model) { mutableStateOf(settings.model) }
        OutlinedTextField(
            value = model,
            onValueChange = { model = it; onSetModel(it) },
            label = { Text(s.modelHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(12.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (themeMode == ThemeMode.DARK) s.darkMode else s.lightMode,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = themeMode == ThemeMode.DARK,
            onCheckedChange = { onSetThemeMode(if (it) ThemeMode.DARK else ThemeMode.LIGHT) },
        )
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
