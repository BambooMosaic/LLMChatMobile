package com.bamboomosaic.llmchat.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import com.bamboomosaic.llmchat.core.domain.ApiConfig
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.Providers

/**
 * 桌面端功能对话框（桌面端第 2 轮，可用级 UI）：
 * - ConfigDialog：API 配置列表 / 新增 / 编辑 / 删除 / 默认 / 切换（与手机端配置语义一致）
 * - SettingsDialog：基础设置项（语言 / 称呼 / 上下文注入 / 思考与搜索默认 / 省 Token 档位 / 压缩 / 上下文上限）
 */

@Composable
fun ConfigDialog(vm: DesktopChatViewModel, onDismiss: () -> Unit) {
    val configs by vm.configs.collectAsState()
    val currentConfig by vm.currentConfig.collectAsState()
    var editing by remember { mutableStateOf<ApiConfig?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API 配置") },
        text = {
            val form = editing
            if (form != null) {
                ConfigForm(
                    initial = form,
                    onSave = { saved ->
                        vm.scope.launch { vm.configRepository.save(saved) }
                        editing = null
                    },
                    onCancel = { editing = null },
                )
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (configs.isEmpty()) {
                        Text(
                            "还没有配置，点下方「新增配置」开始",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    configs.forEach { c ->
                        val isCurrent = currentConfig?.id == c.id
                        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                (if (c.isDefault) "[默认] " else "") + c.name +
                                    if (isCurrent) "（使用中）" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${c.provider} · ${c.model}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { vm.scope.launch { vm.configRepository.select(c.id) } }) {
                                    Text(if (isCurrent) "使用中" else "使用")
                                }
                                TextButton(onClick = { vm.scope.launch { vm.configRepository.setDefault(c.id) } }) {
                                    Text("设默认")
                                }
                                TextButton(onClick = { editing = c }) { Text("编辑") }
                                TextButton(onClick = { vm.scope.launch { vm.configRepository.delete(c.id) } }) {
                                    Text("删除")
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            editing = ApiConfig(name = "", provider = Providers.CUSTOM, baseUrl = "", apiKey = "", model = "")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("新增配置") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/** 单条配置的表单（新增/编辑共用）：供应商预设自动填充 URL 与默认模型。 */
@Composable
private fun ConfigForm(
    initial: ApiConfig,
    onSave: (ApiConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var providerId by remember { mutableStateOf(initial.provider) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var model by remember { mutableStateOf(initial.model) }
    var supportReasoning by remember { mutableStateOf(initial.supportReasoning) }

    val preset = Providers.all.firstOrNull { it.id == providerId }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("配置名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SimpleDropdown(
            label = "供应商",
            value = preset?.displayName ?: providerId,
            options = Providers.all.map { it.displayName },
            modifier = Modifier.fillMaxWidth(),
            onSelect = { i ->
                val p = Providers.all[i]
                providerId = p.id
                if (p.id != Providers.CUSTOM) {
                    if (baseUrl.isBlank()) baseUrl = p.baseUrl
                    if (model.isBlank()) model = p.models.firstOrNull() ?: ""
                }
            },
        )
        if (providerId == Providers.CUSTOM) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API 地址（含版本段，如 https://host/v1）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("模型") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (preset != null && preset.models.isNotEmpty()) {
            SimpleDropdown(
                label = "预设模型",
                value = model,
                options = preset.models,
                modifier = Modifier.fillMaxWidth(),
                onSelect = { model = preset.models[it] },
            )
        }
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API 密钥") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = supportReasoning, onCheckedChange = { supportReasoning = it })
            Text("支持深度思考", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.ifBlank { model.ifBlank { "配置" } },
                            provider = providerId,
                            baseUrl = baseUrl.trim(),
                            apiKey = apiKey.trim(),
                            model = model.trim(),
                            supportReasoning = supportReasoning,
                        ),
                    )
                },
            ) { Text("保存") }
            TextButton(onClick = onCancel) { Text("取消") }
        }
    }
}

/** 基础设置面板：保存即生效（直接写 SettingsStore，两端同一套持久化）。 */
@Composable
fun SettingsDialog(vm: DesktopChatViewModel, onDismiss: () -> Unit) {
    val settings by vm.settings.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置（基础项）") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fun set(block: suspend () -> Unit) = vm.scope.launch { block() }

                SimpleDropdown(
                    label = "界面语言",
                    value = when (settings.language) {
                        AppLanguage.SIMPLIFIED_CHINESE -> "简体中文"
                        AppLanguage.TRADITIONAL_CHINESE -> "繁體中文"
                        AppLanguage.ENGLISH -> "English"
                    },
                    options = listOf("简体中文", "繁體中文", "English"),
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = { i ->
                        set {
                            vm.settingsRepository.setLanguage(
                                when (i) {
                                    1 -> AppLanguage.TRADITIONAL_CHINESE
                                    2 -> AppLanguage.ENGLISH
                                    else -> AppLanguage.SIMPLIFIED_CHINESE
                                },
                            )
                        }
                    },
                )
                OutlinedTextField(
                    value = settings.userNickname,
                    onValueChange = { vm.scope.launch { vm.settingsRepository.setUserNickname(it) } },
                    label = { Text("用户称呼（AI 如何称呼你）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SwitchRow("自动注入环境信息（时间/地区等）", settings.contextInjectionEnabled) {
                    set { vm.settingsRepository.setContextInjectionEnabled(it) }
                }
                SwitchRow("深度思考默认开启", settings.thinkingEnabled) {
                    set { vm.settingsRepository.setThinkingEnabled(it) }
                }
                SwitchRow("联网搜索默认开启", settings.searchEnabled) {
                    set { vm.settingsRepository.setSearchEnabled(it) }
                }
                SimpleDropdown(
                    label = "省 Token 档位",
                    value = when (settings.tokenSavingLevel) {
                        0 -> "不启用"
                        1 -> "1 档（压缩线 120k）"
                        2 -> "2 档（压缩线 50k）"
                        3 -> "3 档（压缩线 30k）"
                        else -> "4 终极档（10k）"
                    },
                    options = listOf(
                        "不启用", "1 档（压缩线 120k）", "2 档（压缩线 50k）", "3 档（压缩线 30k）", "4 终极档（10k）",
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = { i -> set { vm.settingsRepository.setTokenSavingLevel(i) } },
                )
                SwitchRow("自动压缩历史（无档位时）", settings.autoCompressEnabled) {
                    set { vm.settingsRepository.setAutoCompressEnabled(it) }
                }
                OutlinedTextField(
                    value = settings.maxContextTokens.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> set { vm.settingsRepository.setMaxContextTokens(v) } } },
                    label = { Text("最大上下文（tokens）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = settings.maxOutputTokens.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> set { vm.settingsRepository.setMaxOutputTokens(v) } } },
                    label = { Text("最大输出（tokens）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "说明：主题 / 语音朗读 / 导入导出等高级项将随后续轮次移植",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
