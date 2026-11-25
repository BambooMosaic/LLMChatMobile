package com.bamboomosaic.llmchat.feature.chat

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.ChatModes
import com.bamboomosaic.llmchat.core.domain.ModeConfig
import com.bamboomosaic.llmchat.feature.chat.i18n.LocalAppStrings

/**
 * 模式选择/编辑弹窗（新建对话 与 会话模式修改 共用）。
 * 模式列表为圆角直排单选列表（radio + 标题 + 小白副标题），
 * 角色扮演模式双面板（角色卡表单 / 自由输入），底部按钮按场景区分。
 */
@Composable
fun ModeEditorDialog(
    initialModeId: String,
    initialUserPrompt: String?,
    /** 底部按钮文字：true=创建对话（新建场景），false=确定（编辑场景）。 */
    createMode: Boolean,
    /** 当前应用语言（模式文案三语）。 */
    uiLanguage: com.bamboomosaic.llmchat.core.domain.AppLanguage,
    /** 角色扮演模式：括号描写规则开关当前值。 */
    roleplayParenRule: Boolean = true,
    /** 21 轮：省 Token 档位当前值（省钱模式字符区间随终极档锁定）。 */
    tokenSavingLevel: Int = 0,
    /** 括号规则开关变化回调。 */
    onRoleplayParenRuleChange: (Boolean) -> Unit = {},
    onConfirm: (modeId: String, userPrompt: String?, temperature: Float?, topP: Float?, importedMessages: List<ImportedMessage>?) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalAppStrings.current
    var selectedId by remember { mutableStateOf(initialModeId) }
    var dropdownOpen by remember { mutableStateOf(false) }

    // 29 轮：模板库浏览状态——showTemplates=开关；templatePreview=非空时为 Markdown 预览页
    var showTemplates by remember { mutableStateOf(false) }
    var templatePreview by remember { mutableStateOf<PromptTemplateLibrary.Template?>(null) }

    // 两个面板的数据互相隔离：角色卡表单 vs 自由输入
    // 18 轮修复：重开弹窗时从已保存的用户层提示词回显角色卡（保存格式：角色名：X\n角色设定：\nY）
    val initialPrompt = initialUserPrompt?.trim().orEmpty()
    val parsedCard = remember(initialPrompt) { parseRoleCard(initialPrompt) }
    val hasSavedCard = parsedCard.first.isNotEmpty() || parsedCard.second.isNotEmpty()
    var cardName by remember { mutableStateOf(parsedCard.first) }
    var cardInfo by remember { mutableStateOf(parsedCard.second) }
    var freePrompt by remember { mutableStateOf(if (hasSavedCard) "" else initialPrompt) }
    var useRoleCard by remember { mutableStateOf(initialPrompt.isEmpty() || hasSavedCard) }

    var customTemp by remember { mutableStateOf(ChatModes.CUSTOM_DEFAULT_TEMPERATURE.toString()) }
    var customTopP by remember { mutableStateOf(ChatModes.CUSTOM_DEFAULT_TOP_P.toString()) }
    // 19 轮：导入的模式 JSON 中携带的对话记录，确定时一并还原
    var importedMessages by remember { mutableStateOf<List<ImportedMessage>?>(null) }
    // 21 轮：提问模式 / 省钱模式面板状态（从已保存提示词解析回显）
    val parsedAsk = remember(initialPrompt) { parseAskPrompt(initialPrompt) }
    val parsedSaving = remember(initialPrompt) { parseSavingPrompt(initialPrompt) }
    var askTopic by remember { mutableStateOf(parsedAsk?.first ?: "") }
    var askDetail by remember { mutableStateOf(parsedAsk?.second ?: 1) }
    var savingMaxChars by remember { mutableStateOf(parsedSaving?.first?.toString() ?: "256") }
    var savingExtra by remember { mutableStateOf(parsedSaving?.second ?: "") }

    val selected = ChatModes.find(selectedId, uiLanguage)
    val isRoleplay = selected.modeId == ModeConfig.ROLEPLAY
    val isAsk = selected.modeId == ModeConfig.ASK
    val isSaving = selected.modeId == ModeConfig.SAVING

    // 最终提交的用户提示词：角色卡拼接 / 提问模板 / 省钱模板 / 自由输入
    val effectiveUserPrompt = when {
        isRoleplay && useRoleCard -> {
            val name = cardName.trim()
            val info = cardInfo.trim()
            if (name.isEmpty() && info.isEmpty()) "" else "角色名：$name\n角色设定：\n$info"
        }
        selected.modeId == ModeConfig.ASK -> {
            val t = askTopic.trim()
            if (t.isEmpty()) "" else composeAskPrompt(t, askDetail)
        }
        selected.modeId == ModeConfig.SAVING -> {
            val n = (savingMaxChars.toIntOrNull() ?: 256)
                .coerceIn(16, if (tokenSavingLevel == 4) 64 else 1024)
            composeSavingPrompt(n, savingExtra)
        }
        else -> freePrompt.trim()
    }

    // —— 19 轮：单模式配置导入（导出入口在「模式详情」只读预览弹窗） ——
    val context = LocalContext.current

    /** 解析导入 JSON：合法则还原进编辑器（落为自定义模式），返回是否成功。 */
    fun applyImportPayload(text: String): Boolean {
        val obj = runCatching { org.json.JSONObject(text) }.getOrNull() ?: return false
        if (obj.optString("app") != "llmchat-mode") return false
        selectedId = ModeConfig.CUSTOM
        val up = obj.optString("userPrompt")
        val card = parseRoleCard(up)
        if (card.first.isNotEmpty() || card.second.isNotEmpty()) {
            cardName = card.first
            cardInfo = card.second
            useRoleCard = true
            freePrompt = ""
        } else {
            useRoleCard = false
            freePrompt = up
        }
        if (obj.has("roleplayParenRule")) {
            onRoleplayParenRuleChange(obj.optBoolean("roleplayParenRule", roleplayParenRule))
        }
        obj.optString("temperature").toFloatOrNull()?.let { customTemp = it.toString() }
        obj.optString("top_p").toFloatOrNull()?.let { customTopP = it.toString() }
        importedMessages = obj.optJSONArray("conversation")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    add(
                        ImportedMessage(
                            roleName = m.optString("role"),
                            text = m.optString("text"),
                            reasoning = m.optString("reasoning").takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            }
        }
        return true
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull().orEmpty()
        if (!applyImportPayload(text)) {
            Toast.makeText(context, s.modeImportFailed, Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        // 30 轮：禁用全屏窗口——blurBehind 模糊只落在窗口（卡片）矩形内，不再全屏雾化
    ) {
        // 27 轮：平板/宽屏两列式——左列模式直排列表（竖屏折叠菜单的展开形态），右列补充上下文/角色卡 + 确认按钮；
        // 窄屏维持原单列折叠交互
        val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
        GlassDialogShell(
            modifier = Modifier
                .fillMaxWidth()
                .wideDialogMax(760.dp),
                // 30 轮：全屏窗口时代的 padding(16) 已不需要——窗口本身收缩为卡片矩形
        ) {
            // 面板与确认按钮抽成局部 Composable，单列/两列共用同一份实现
            // 29 轮：模板入口改为四个池模式（聊天/办公/写作/开发）都显示，按当前模式加载专属池
            val showTemplateEntry = selected.allowUserPrompt &&
                PromptTemplateLibrary.hasPool(selectedId) &&
                !isAsk && !isSaving && !(isRoleplay && useRoleCard)

            // 29 轮：模板库浏览视图（当前模式的模板列表 → Markdown 全文预览 → 应用填入用户层提示词）
            val templateBrowser: @Composable () -> Unit = {
                val pool = PromptTemplateLibrary.poolFor(selectedId).orEmpty()
                val preview = templatePreview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.TextButton(onClick = {
                        if (preview != null) templatePreview = null else showTemplates = false
                    }) { Text("←") }
                    Text(
                        text = preview?.title ?: s.promptTemplateLibrary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (preview == null) {
                    // 列表页：当前模式的专属模板池
                    pool.forEach { tpl ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { templatePreview = tpl },
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(tpl.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = tpl.markdown.removePrefix("# ${tpl.title}").trim().take(60),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    // 预览页：Markdown 全文渲染，确认后应用（填入用户层提示词输入框）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        MarkdownText(content = preview.markdown, enableHorizontalScroll = false)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.TextButton(onClick = {
                            templatePreview = null
                            showTemplates = false
                        }) { Text(s.cancel) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                freePrompt = preview.markdown
                                templatePreview = null
                                showTemplates = false
                            },
                        ) { Text(s.save) }
                    }
                }
            }

            val panelsContent: @Composable () -> Unit = {
                // 27 轮：模板库入口行（仅自由输入场景）
                if (showTemplateEntry) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        androidx.compose.material3.TextButton(onClick = {
                            templatePreview = null
                            showTemplates = true
                        }) {
                            Text(s.promptTemplateLibrary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                // 用户层输入：allowUserPrompt=false 的模式（ds_web）整块隐藏
                if (selected.allowUserPrompt) {
                // 角色扮演：双面板切换
                if (isRoleplay) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChipCompact(
                            selected = useRoleCard,
                            onClick = { useRoleCard = true },
                            label = if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Use role card" else "使用角色卡",
                        )
                        FilterChipCompact(
                            selected = !useRoleCard,
                            onClick = { useRoleCard = false },
                            label = if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Configure myself" else "我要自己配置",
                        )
                    }
                    // 17 轮：括号描写规则开关（仅角色扮演模式显示，默认开，DataStore 持久化）
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            text = s.roleplayParenLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.Switch(
                            checked = roleplayParenRule,
                            onCheckedChange = { onRoleplayParenRuleChange(it) },
                        )
                    }
                    if (useRoleCard) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cardName,
                            onValueChange = { cardName = it },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Character name" else "角色名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cardInfo,
                            onValueChange = { cardInfo = it },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Character info" else "角色信息") },
                            placeholder = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Personality, speaking style, backstory, catchphrases..." else "性格、说话风格、背景故事、口头禅等") },
                            minLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = freePrompt,
                            onValueChange = { freePrompt = it },
                            label = { Text(s.modeUserPromptLabel) },
                            placeholder = { Text(selected.localized(uiLanguage).userPromptPlaceholder) },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    } else if (isAsk) {
                        // 21 轮：提问模式——主题 + 理由详细程度滑块（不允许自由提示词）
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = askTopic,
                            onValueChange = { askTopic = it },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Question topic" else "提问主题") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Reasoning detail" else "回答理由详细程度",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LevelSlider(
                            levels = listOf(
                                if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Off" else "禁用",
                                if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Brief" else "简单",
                                if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Detailed" else "详细",
                            ),
                            selectedIndex = askDetail,
                            onSelect = { askDetail = it },
                        )
                    } else if (isSaving) {
                        // 21 轮：省钱模式——最大输出字符（终极档锁定 16~64）+ 补充信息（≤256 字）
                        val hiLimit = if (tokenSavingLevel == 4) 64 else 1024
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = savingMaxChars,
                            onValueChange = { savingMaxChars = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Max output characters (16~$hiLimit)" else "最多输出字符（16~$hiLimit）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = savingExtra,
                            onValueChange = { savingExtra = it.take(256) },
                            label = { Text(if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "Extra info (optional, max 256 chars)" else "补充信息（可选，最多256字）") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = freePrompt,
                            onValueChange = { freePrompt = it },
                            label = { Text(s.modeUserPromptLabel) },
                            placeholder = { Text(selected.localized(uiLanguage).userPromptPlaceholder) },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                } // allowUserPrompt 收口

                // 自定义模式：温度 / topP 输入
                if (selected.modeId == ModeConfig.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customTemp,
                            onValueChange = { customTemp = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(s.customTemperature) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customTopP,
                            onValueChange = { customTopP = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(s.customTopP) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            val confirmButton: @Composable () -> Unit = {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val temp = customTemp.toFloatOrNull()
                        val topp = customTopP.toFloatOrNull()
                        onConfirm(
                            selectedId,
                            effectiveUserPrompt.takeIf { it.isNotBlank() },
                            if (selectedId == ModeConfig.CUSTOM) temp else null,
                            if (selectedId == ModeConfig.CUSTOM) topp else null,
                            importedMessages,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (createMode) s.createConversation else if (uiLanguage == com.bamboomosaic.llmchat.core.domain.AppLanguage.ENGLISH) "OK" else "确定")
                }
            }

            if (!wide) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    if (showTemplates) {
                        templateBrowser()
                    } else {
                    Text(s.modeSelectTitle, style = MaterialTheme.typography.titleMedium)
                    // 19 轮：导入模式配置（含对话记录），还原为自定义模式；仅新建对话场景显示
                    if (createMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            androidx.compose.material3.TextButton(onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }) {
                                Text(s.modeImport, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    // 折叠下拉触发框（显示当前选中模式）
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownOpen = !dropdownOpen },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    modeNameOf(selected, uiLanguage),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = selected.localized(uiLanguage).description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = if (dropdownOpen) "▾" else "▸",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // 独立弹出菜单（锚定触发框下方，radio + 标题 + 副标题，圆角，宽度与弹窗内容一致）
                    androidx.compose.foundation.layout.BoxWithConstraints {
                        val menuWidth = maxWidth
                        androidx.compose.material3.DropdownMenu(
                            expanded = dropdownOpen,
                            onDismissRequest = { dropdownOpen = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(menuWidth),
                        ) {
                            ChatModes.all.forEach { mode ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.material3.RadioButton(
                                                selected = selectedId == mode.modeId,
                                                onClick = { selectedId = mode.modeId },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = MaterialTheme.colorScheme.primary,
                                                ),
                                            )
                                            Column {
                                                Text(
                                                    text = modeNameOf(mode, uiLanguage),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                Text(
                                                    text = mode.localized(uiLanguage).description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedId = mode.modeId
                                        dropdownOpen = false
                                    },
                                )
                            }
                        }
                    }

                    panelsContent()
                    confirmButton()
                    }
                }
            } else {
                // 27 轮：宽屏两列——左列模式列表（不折叠），右列面板 + 确认
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            s.modeSelectTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        // 导入入口仅新建对话场景显示（与窄屏一致）
                        if (createMode) {
                            androidx.compose.material3.TextButton(onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }) {
                                Text(s.modeImport, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.heightIn(max = 440.dp)) {
                        // 左列：全部模式直排（radio + 名称 + 副标题），选中即切换右列面板
                        Column(
                            modifier = Modifier
                                .width(264.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            ChatModes.all.forEach { mode ->
                                ModeListRow(
                                    selected = selectedId == mode.modeId,
                                    label = modeNameOf(mode, uiLanguage),
                                    description = mode.localized(uiLanguage).description,
                                    onSelect = { selectedId = mode.modeId },
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        // 右列：补充上下文 / 角色卡 / 自定义参数 + 确认按钮
                        // 27 轮：模板库浏览时右列整体切换为模板选择
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            if (showTemplates) {
                                templateBrowser()
                            } else {
                                panelsContent()
                                confirmButton()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 紧凑 FilterChip（角色卡面板切换用）。 */
@Composable
private fun FilterChipCompact(selected: Boolean, onClick: () -> Unit, label: String) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    )
}

/** 27 轮：模式显示名（Legacy 标注不推荐；窄屏折叠菜单与宽屏左列共用）。 */
private fun modeNameOf(mode: ModeConfig, uiLanguage: AppLanguage): String =
    mode.localized(uiLanguage).modeName + if (mode.modeId == ModeConfig.LEGACY) {
        if (uiLanguage == AppLanguage.ENGLISH) " (not recommended)" else "（不推荐）"
    } else ""

/**
 * 27 轮：宽屏左列的模式行——竖屏折叠菜单的展开形态（radio + 名称 + 副标题），点整行即选中。
 */
@Composable
private fun ModeListRow(
    selected: Boolean,
    label: String,
    description: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 解析已保存的角色卡文本（保存格式：角色名：X\n角色设定：\nY）。
 * 非该格式（自由输入的历史提示词）返回空对，由调用方回退到自由输入面板。
 */
internal fun parseRoleCard(text: String): Pair<String, String> {
    if (!text.startsWith("角色名：")) return "" to ""
    val namePrefix = "角色名："
    val infoMarker = "角色设定："
    val firstNl = text.indexOf('\n')
    if (firstNl < 0) return text.removePrefix(namePrefix).trim() to ""
    val name = text.substring(namePrefix.length, firstNl).trim()
    val markerIdx = text.indexOf(infoMarker, firstNl)
    val info = if (markerIdx >= 0) text.substring(markerIdx + infoMarker.length).trim() else ""
    return name to info
}

/** 19 轮：导入的模式 JSON 中的一条对话记录（role 取 MessageRole.name）。 */
data class ImportedMessage(
    val roleName: String,
    val text: String,
    val reasoning: String?,
)

/** 21 轮：拼装提问模式的用户层提示词（detail：0 禁用 / 1 简要 / 2 详细）。 */
private fun composeAskPrompt(topic: String, detail: Int): String = when (detail) {
    1 -> "提问主题：$topic\n回答要求：先给答案，并简要说明理由。"
    2 -> "提问主题：$topic\n回答要求：先给答案，再详细展开理由与依据。"
    else -> "提问主题：$topic"
}

/** 21 轮：解析提问模式提示词（非提问格式返回 null）。 */
private fun parseAskPrompt(text: String): Pair<String, Int>? {
    if (!text.startsWith("提问主题：")) return null
    val topic = text.removePrefix("提问主题：").substringBefore('\n').trim()
    val detail = when {
        text.contains("详细展开理由") -> 2
        text.contains("简要说明理由") -> 1
        else -> 0
    }
    return topic to detail
}

/** 21 轮：拼装省钱模式单层提示词模板。22 轮加严措辞（N 同时会成为硬性 max_tokens 上限）。 */
private fun composeSavingPrompt(maxChars: Int, extra: String): String =
    "你最多输出 $maxChars 个字符。这是硬性红线，不是建议：宁可少说、拆成多条，也绝不能超字；" +
        "回答直击要点，不要开场白、客套、免责声明和总结性废话。" +
        (if (extra.isNotBlank()) "\n用户补充：${extra.trim()}" else "")

/** 21 轮：解析省钱模式模板（返回 字符数 to 补充信息；非省钱格式返回 null）。 */
private fun parseSavingPrompt(text: String): Pair<Int, String>? {
    if (!text.startsWith("你最多输出")) return null
    val chars = Regex("\\d+").find(text.substringBefore("个字符"))?.value?.toIntOrNull() ?: return null
    val extra = text.substringAfter("用户补充：", "").trim()
    return chars to extra
}
