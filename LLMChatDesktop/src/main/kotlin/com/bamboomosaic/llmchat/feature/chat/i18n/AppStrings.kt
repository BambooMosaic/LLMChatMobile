package com.bamboomosaic.llmchat.feature.chat.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.bamboomosaic.llmchat.core.domain.AppLanguage


/**
 * 桌面端改写版：手机端为 data class（246 字段），JVM 类校验拒绝 >255 参数的方法签名
 * （ART 无此限制，手机不受影响）。改为 interface + 三个语言 object（属性直初始化），
 * s.xxx 使用点与函数成员零改动。
 */
interface AppStrings {
    val appName: String
    val newConversation: String
    val settings: String
    val emptyConversations: String
    val inputPlaceholder: String
    val send: String
    val thinking: String
    val attachFile: String
    val sessionTokens: String
    val tokenSummary: String
    val inputTokens: String
    val outputTokens: String
    val totalTokens: String
    val thisRoundFormat: String
    val rename: String
    val renameTitle: String
    val cancel: String
    val save: String
    val back: String
    val apiConfig: String
    val appearance: String
    val languageSetting: String
    val provider: String
    val custom: String
    val baseUrl: String
    val apiKey: String
    val model: String
    val modelHint: String
    val systemPrompt: String
    val darkMode: String
    val lightMode: String
    val simplifiedChinese: String
    val traditionalChinese: String
    val english: String
    val requestFailed: String
    val loading: String
    val expandMenu: String
    val otherSettings: String
    val maxContext: String
    val maxOutput: String
    val autoCompress: String
    val copy: String
    val exportCrashLogs: String
    val quote: String
    val copied: String
    val archive: String
    val archivedConversations: String
    val noArchivedConversations: String
    val tutorialLabel: String
    val promptTemplateLibrary: String
    val flowTitle: String
    val flowEmpty: String
    val flowTapToSwitch: String
    val ttsSection: String
    val ttsVoiceLabel: String
    val ttsVoiceStella: String
    val ttsVoiceTide: String
    val ttsFailed: String
    val ttsAutoRead: String
    val ttsCostHint: String
    val ttsEngineLabel: String
    val ttsEngineDeepseek: String
    val ttsEngineQwen: String
    val ttsEngineGlm: String
    val ttsHintQwen: String
    val ttsHintGlm: String
    val ttsMenuSpeak: String
    val ttsMenuStop: String
    val netRetryLabel: String
    val netRetryHint: String
    val exportAllData: String
    val importAllData: String
    val backupHint: String
    val importConfirmTitle: String
    val importConfirmBody: String
    val importConfirmConfirm: String
    val importRestartTitle: String
    val importRestartBody: String
    val importRestartButton: String
    val netRetryBannerFormat: String
    val restore: String
    val delete: String
    val deleteConfirmTitle: String
    val addConfig: String
    val edit: String
    val setDefault: String
    val defaultLabel: String
    val noConfig: String
    val editConfig: String
    val configName: String
    val supportReasoning: String
    val about: String
    val versionLabel: String
    val developer: String
    val githubRepo: String
    val iKnow: String
    val testNotice: String
    val regenerate: String
    val editPrompt: String
    val editPromptHint: String
    val export: String
    val exportMarkdown: String
    val exportPlainText: String
    val exportSuccess: String
    val followSystem: String
    val visionUnsupported: String
    val upload: String
    val uploadImage: String
    val imageLimitHint: String
    val last1Day: String
    val last7Days: String
    val welcomeTitle: String
    val welcomeSubtitle: String
    val next: String
    val skip: String
    val configApiTitle: String
    val apiGuideText: String
    val goDeepSeek: String
    val gotApiKey: String
    val useOtherProvider: String
    val verifyConfig: String
    val configSuccess: String
    val configFail: String
    val quickGuideTitle: String
    val guideMarkdown: String
    val allDone: String
    val backToGuide: String
    val skipHint: String
    val searchWeb: String
    val searching: String
    val searchDone: String
    val configCustomModel: String
    val priceCheap: String
    val priceMedium: String
    val priceHigh: String
    val priceVeryHigh: String
    val searchedPagesFormat: String
    val defaultModeLabel: String
    val defaultModeOff: String
    val advancedOptions: String
    val newConversationTitle: String
    val createConversation: String
    val modeUserPromptLabel: String
    val customTemperature: String
    val customTopP: String
    val modeLocked: String
    val promptTruncated: String
    val modeLabel: String
    val modeSelectTitle: String
    val searchShort: String
    val thinkingShort: String
    val errGuideAuth: String
    val errGuideBalance: String
    val errGuideRateLimit: String
    val errGuideTimeout: String
    val errGuideNetwork: String
    val errGuideUnknown: String
    val roleplayParenLabel: String
    val modePreviewTitle: String
    val modePreviewUserLayer: String
    val modePreviewRoleCard: String
    val modePreviewCopy: String
    val modePreviewCopied: String
    val searchSettingLabel: String
    val searchProxyHint: String
    val userNicknameLabel: String
    val modeExport: String
    val modeImport: String
    val modeImportFailed: String
    val aboutDownload: String
    val preferencesSetting: String
    val tokenSavingUltimate: String
    val tokenSavingUltimateWarning: String
    val tokenCacheHit: String
    val colorPresetLabel: String
    val exportJson: String
    val presetLightGroup: String
    val presetDarkGroup: String
    val presetLightDefault: String
    val presetLightBlue: String
    val presetLightGreen: String
    val presetLightYellow: String
    val presetLightGray: String
    val presetLightPink: String
    val presetDarkDefault: String
    val presetDarkBlue: String
    val presetDarkGreen: String
    val presetDarkBrown: String
    val presetDarkBlack: String
    val presetDarkRed: String
    val backgroundLabel: String
    val pickImage: String
    val clearImage: String
    val contextStatsTitle: String
    val contextCapacity: String
    val contextUsed: String
    val segMessages: String
    val segReasoning: String
    val segSearch: String
    val segSystem: String
    val manualCompressButton: String
    val tokenSavingLabel: String
    val tokenSavingOff: String
    val tokenSavingOffHint: String
    val tokenSavingQuality: String
    val tokenSavingQualityHint: String
    val tokenSavingMedium: String
    val tokenSavingMediumHint: String
    val tokenSavingMax: String
    val tokenSavingMaxHint: String
    val creditsButton: String
    val creditsTitle: String
    val creditsDev: String
    val creditsPlan: String
    val creditsMainCode: String
    val creditsImpl: String
    val creditsUiDesign: String
    val creditsMentor: String
    val creditsGithub: String
    val contextInjectionLabel: String
    val contextInjectionHint: String
    val regionLabel: String
    val regionPlaceholder: String
    val costThisLabel: String
    val costTotalLabel: String
    val searchInConversation: String
    val searchHint: String
    val searchEmpty: String
    val compressPreviewBodyFormat: String
    val compressConfirmBtn: String
    val followSystemHint: String
    val compressLimitLabel: String
    val compressLimitHint: String
    val pinned: String
    val unpinned: String
    val sectionPinned: String
    val sectionAll: String
    val sectionToday: String
    val section7Days: String
    val section30Days: String
    val sectionEarlier: String
    /** 25 轮：玻璃材质三档滑块（外观设置）。 */
    val glassLabel: String
    val glassLevelOff: String
    val glassLevelTranslucent: String
    val glassLevelFrosted: String
    val glassHint: String

    fun thisRound(prompt: Long, completion: Long, total: Long): String =
        String.format(thisRoundFormat, total, prompt, completion)

    fun sessionTokensText(total: Long): String = "$sessionTokens: $total"

    fun netRetryBanner(attempt: Int, max: Int): String =
        String.format(netRetryBannerFormat, attempt, max)

    fun searchedPages(n: Int): String = String.format(searchedPagesFormat, n)

    /** 23 轮：金额折算摘要行（金额已含货币符号）。 */
    fun costSummary(formattedThis: String, formattedTotal: String): String =
        "$costThisLabel $formattedThis / $costTotalLabel $formattedTotal"

    /** 23 轮：压缩预览正文。 */
    fun compressPreviewBody(toCompress: Int, summaryLimit: Int): String =
        String.format(compressPreviewBodyFormat, toCompress, summaryLimit)
}

internal object ZhCNStrings : AppStrings {
    override val appName = "LLMChatMobile"
    override val newConversation = "开启新对话"
    override val settings = "设置"
    override val emptyConversations = "暂无对话记录"
    override val inputPlaceholder = "给 AI 发送消息"
    override val send = "发送"
    override val thinking = "深度思考"
    override val attachFile = "上传文件"
    override val sessionTokens = "Token"
    override val tokenSummary = "Token 统计"
    override val inputTokens = "输入 Token"
    override val outputTokens = "输出 Token"
    override val totalTokens = "总消耗"
    override val thisRoundFormat = "本轮消耗：%1\$d Token（输入 %2\$d + 输出 %3\$d）"
    override val rename = "重命名"
    override val renameTitle = "重命名对话"
    override val cancel = "取消"
    override val save = "保存"
    override val back = "返回"
    override val apiConfig = "API 配置"
    override val appearance = "外观设置"
    override val languageSetting = "语言设置"
    override val provider = "供应商"
    override val custom = "自定义"
    override val baseUrl = "Base URL"
    override val apiKey = "API Key"
    override val model = "模型"
    override val modelHint = "模型名"
    override val systemPrompt = "系统提示词"
    override val darkMode = "深色模式"
    override val lightMode = "浅色模式"
    override val simplifiedChinese = "简体中文"
    override val traditionalChinese = "繁體中文"
    override val english = "English (US)"
    override val requestFailed = "请求失败"
    override val loading = "加载中"
    override val expandMenu = "打开菜单"
    override val otherSettings = "其他设置"
    override val maxContext = "最大上下文 Token"
    override val maxOutput = "最大输出 Token"
    override val autoCompress = "自动压缩上下文"
    override val copy = "复制"
    override val exportCrashLogs = "导出崩溃日志"
    override val quote = "引用"
    override val copied = "已复制"
    override val archive = "归档"
    override val archivedConversations = "恢复对话"
    override val noArchivedConversations = "暂无归档对话"
    override val tutorialLabel = "使用教程"
    override val promptTemplateLibrary = "提示词模板库"
    override val flowTitle = "会话流程图"
    override val flowEmpty = "还没有消息，发送第一条后这里会显示对话流程。"
    override val flowTapToSwitch = "点击切换到这版"
    override val ttsSection = "语音朗读"
    override val ttsVoiceLabel = "音色"
    override val ttsVoiceStella = "海星（女声）"
    override val ttsVoiceTide = "暗潮（男声）"
    override val ttsFailed = "语音合成失败，请稍后再试"
    override val ttsAutoRead = "自动朗读新回复"
    override val ttsCostHint = "自动朗读价格非常昂贵，余额消耗速度极快，谨慎开启"
    override val ttsEngineLabel = "朗读引擎"
    override val ttsEngineDeepseek = "DeepSeek"
    override val ttsEngineQwen = "通义千问"
    override val ttsEngineGlm = "智谱 GLM"
    override val ttsHintQwen = "千问朗读自动使用「通义千问」配置的 API Key，单次最长 600 字"
    override val ttsHintGlm = "GLM 朗读自动使用「智谱 GLM」配置的 API Key，单次最长 1024 字"
    override val ttsMenuSpeak = "朗读这条回复"
    override val ttsMenuStop = "停止朗读"
    override val netRetryLabel = "网络重试次数（0~5，0=不重试）"
    override val netRetryHint = "网络波动或限流时自动重试，次数可调；超过上限后正常报错"
    override val exportAllData = "导出全部数据"
    override val importAllData = "导入全部数据"
    override val backupHint = "备份包含全部设置、API 配置、会话与消息、token 统计；API Key 不会导出，导入后需重新填写"
    override val importConfirmTitle = "确认导入？"
    override val importConfirmBody = "导入将清空并替换现有全部数据（会话、配置、设置），且无法撤销。建议先导出当前数据留底。"
    override val importConfirmConfirm = "导入并替换"
    override val importRestartTitle = "导入完成"
    override val importRestartBody = "数据已恢复。重启应用后生效。"
    override val importRestartButton = "立即重启"
    override val netRetryBannerFormat = "网络异常，正在重试（第 %1\$d 次 / 最多 %2\$d 次）…"
    override val restore = "恢复"
    override val delete = "删除"
    override val deleteConfirmTitle = "确认永久删除？"
    override val addConfig = "新增配置"
    override val edit = "编辑"
    override val setDefault = "设为默认"
    override val defaultLabel = "默认"
    override val noConfig = "暂无配置"
    override val editConfig = "编辑配置"
    override val configName = "配置名称"
    override val supportReasoning = "支持深度思考"
    override val about = "关于"
    override val versionLabel = "版本"
    override val developer = "开发者"
    override val githubRepo = "GitHub 仓库"
    override val iKnow = "我已知晓"
    override val testNotice = "注意！这是 LLMChatMobile 的 beta 测试版本。\n测试版不代表本产品的最终质量，感谢您的理解和支持，祝您好运！"
    override val regenerate = "重新生成"
    override val editPrompt = "编辑提示词"
    override val editPromptHint = "留空则使用全局默认提示词"
    override val export = "导出对话"
    override val exportMarkdown = "Markdown 格式"
    override val exportPlainText = "纯文本格式"
    override val exportSuccess = "已导出到"
    override val followSystem = "跟随系统"
    override val visionUnsupported = "当前模型不支持图片输入"
    override val upload = "上传"
    override val uploadImage = "上传图片"
    override val imageLimitHint = "一条消息最多 4 张图片"
    override val last1Day = "近 1 天"
    override val last7Days = "近 7 天"
    override val welcomeTitle = "欢迎使用 LLMChatMobile"
    override val welcomeSubtitle = "轻量原生的多模型 AI 聊天客户端"
    override val next = "下一步"
    override val skip = "跳过"
    override val configApiTitle = "配置你的 AI 服务"
    override val apiGuideText = "1. 打开 DeepSeek 开放平台 platform.deepseek.com 注册账号\n2. 登录后在「API Keys」页面点击创建密钥\n3. 复制生成的 API Key（形如 sk-xxxx），妥善保存\n4. 点击下方「已获取 API Key」填入即可开始使用"
    override val goDeepSeek = "前往 DeepSeek 开放平台"
    override val gotApiKey = "已获取 API Key"
    override val useOtherProvider = "使用其他供应商 API"
    override val verifyConfig = "验证配置"
    override val configSuccess = "配置成功"
    override val configFail = "验证失败，请检查 API Key、网络或余额"
    override val quickGuideTitle = "快速上手指南"
    override val guideMarkdown = "## 模式选择\n新建对话时可选择 8 种模式：聊天、工作、代码开发、角色扮演（支持角色卡快捷表单）、创意写作、自定义、DS 网页版与 Legacy。每个模式绑定不同的回复风格与参数；发送第一条消息后模式锁定，可在回复下方切换多版本。\n\n## 基础对话\n在底部输入框输入内容，点击右侧箭头发送，回复逐字流式显示。生成中可再次点击按钮停止。长按任意消息可复制全文；长按自己发送的消息可编辑后重发。\n\n## 联网搜索与深度思考\n输入栏右侧「搜索」「思考」开关：搜索让回答引用最新网页信息（显示已搜索网页数）；思考展示推理过程（半透明小字，可折叠）。\n\n## 多模态上传\n点输入栏 📎 按钮可附加图片，支持视觉模型识别图片内容。\n\n## 多版本回复\n「重新生成」不再覆盖旧回答：同一提问的多版回答全部保留，回复下方 ‹ 2/3 › 可切换版本，每个版本独立统计 Token 消耗。长按自己的消息可编辑重发，产生新一轮对话。\n\n## 模型与 API\n点击输入栏左侧模型名快速切换已保存的 API 配置；设置 → API 配置可添加多套配置（供应商/地址/密钥/模型），高级选项可单独限制上下文与输出长度。\n\n## Token 统计\n侧边栏底部「Token 统计」查看累计消耗、按日趋势与模型分布。\n\n## 会话管理\n左上角菜单展开会话列表：长按会话可重命名、归档、导出 Markdown 或永久删除；设置里可恢复已归档会话。"
    override val allDone = "完成！"
    override val backToGuide = "返回指引"
    override val skipHint = "跳过后仍可在设置中配置 API"
    override val searchWeb = "联网搜索"
    override val searching = "正在联网搜索..."
    override val searchDone = "已联网搜索"
    override val configCustomModel = "配置自定义模型"
    override val priceCheap = "便宜"
    override val priceMedium = "中等"
    override val priceHigh = "昂贵"
    override val priceVeryHigh = "非常昂贵"
    override val searchedPagesFormat = "已搜索 %d 个网页"
    override val defaultModeLabel = "新对话默认模式"
    override val defaultModeOff = "禁用（新建时手动选择）"
    override val advancedOptions = "高级选项"
    override val newConversationTitle = "新建对话"
    override val createConversation = "创建对话"
    override val modeUserPromptLabel = "你的补充提示词"
    override val customTemperature = "温度（随机性）"
    override val customTopP = "Top P（多样性）"
    override val modeLocked = "已发送消息后模式不可修改"
    override val promptTruncated = "系统提示词过长已自动截断"
    override val modeLabel = "模式"
    override val modeSelectTitle = "选择模式"
    override val searchShort = "搜索"
    override val thinkingShort = "思考"
    override val errGuideAuth = "密钥无效 / 密钥填写错误，请检查 API 密钥"
    override val errGuideBalance = "余额不足，请前往服务商充值"
    override val errGuideRateLimit = "请求频率超限，请降低发送速度或稍后重试"
    override val errGuideTimeout = "网络连接超时，请检查网络、代理、API 地址"
    override val errGuideNetwork = "无网络连接，请检查设备网络"
    override val errGuideUnknown = "请求失败，请核对配置或稍后重试"
    override val roleplayParenLabel = "动作、神态、环境描写统一用（）包裹"
    override val modePreviewTitle = "模式预览"
    override val modePreviewUserLayer = "用户层提示词"
    override val modePreviewRoleCard = "角色卡"
    override val modePreviewCopy = "复制全文"
    override val modePreviewCopied = "已复制"
    override val searchSettingLabel = "联网搜索"
    override val searchProxyHint = "DeepSeek 原生联网搜索；其他模型将先由 DeepSeek-Flash 联网搜索，再把结果交给当前模型作答（消息内容会额外发送给 DeepSeek）"
    override val userNicknameLabel = "用户称呼"
    override val modeExport = "导出"
    override val modeImport = "导入"
    override val modeImportFailed = "导入失败：不是有效的模式配置文件"
    override val aboutDownload = "下载最新安装包"
    override val preferencesSetting = "偏好设置"
    override val tokenSavingUltimate = "终极"
    override val tokenSavingUltimateWarning = "⚠ 终极档：输出与上下文极限压缩，并禁用联网搜索与深度思考，只能在省钱模式对话中发送消息。不建议开启，除非余额很少。"
    override val tokenCacheHit = "缓存命中"
    override val colorPresetLabel = "配色预设"
    override val exportJson = "JSON 格式"
    override val presetLightGroup = "浅色预设"
    override val presetDarkGroup = "深色预设"
    override val presetLightDefault = "默认"
    override val presetLightBlue = "浅蓝"
    override val presetLightGreen = "浅绿"
    override val presetLightYellow = "黄"
    override val presetLightGray = "浅灰"
    override val presetLightPink = "浅粉"
    override val presetDarkDefault = "默认"
    override val presetDarkBlue = "蓝"
    override val presetDarkGreen = "绿"
    override val presetDarkBrown = "棕"
    override val presetDarkBlack = "纯黑"
    override val presetDarkRed = "深红"
    override val backgroundLabel = "聊天背景图"
    override val pickImage = "选择图片"
    override val clearImage = "清除"
    override val contextStatsTitle = "上下文统计"
    override val contextCapacity = "上下文容量"
    override val contextUsed = "已使用"
    override val segMessages = "消息"
    override val segReasoning = "深度思考"
    override val segSearch = "联网搜索"
    override val segSystem = "系统提示词"
    override val manualCompressButton = "手动压缩上下文"
    override val tokenSavingLabel = "省 Token 档位"
    override val tokenSavingOff = "关"
    override val tokenSavingOffHint = "不限制输出，不自动压缩上下文"
    override val tokenSavingQuality = "质量"
    override val tokenSavingQualityHint = "单次输出≤4096；上下文达 12 万自动压缩"
    override val tokenSavingMedium = "中等"
    override val tokenSavingMediumHint = "单次输出≤1024；上下文达 5 万自动压缩"
    override val tokenSavingMax = "最大"
    override val tokenSavingMaxHint = "单次输出≤256；上下文达 3 万自动压缩；禁用联网搜索与深度思考"
    override val creditsButton = "开发者名单"
    override val creditsTitle = "开发者名单"
    override val creditsDev = "开发者"
    override val creditsPlan = "策划"
    override val creditsMainCode = "主要代码"
    override val creditsImpl = "功能实现"
    override val creditsUiDesign = "UI 与设计"
    override val creditsMentor = "开发指导"
    override val creditsGithub = "GitHub"
    override val contextInjectionLabel = "自动注入环境信息"
    override val contextInjectionHint = "在 system 末尾附带当前时间、地区与客户端信息（DS 网页版模式始终开启）"
    override val regionLabel = "地区"
    override val regionPlaceholder = "跟随设备地区，可手动填写城市名"
    override val costThisLabel = "本次"
    override val costTotalLabel = "累计"
    override val searchInConversation = "会话内搜索"
    override val searchHint = "搜索消息"
    override val searchEmpty = "无匹配消息"
    override val compressPreviewBodyFormat = "将保留最近 4 条消息，其余 %1\$d 条压缩为不超过 %2\$d 字的 AI 摘要，原细节不可回溯。确认执行？"
    override val compressConfirmBtn = "确认压缩"
    override val followSystemHint = "开启后按系统深浅自动切换浅色 / 深色预设组"
    override val compressLimitLabel = "压缩摘要上限（字）"
    override val compressLimitHint = "手动压缩时 AI 摘要的字数上限，100~1024（默认 400）"
    override val pinned = "置顶"
    override val unpinned = "取消置顶"
    override val sectionPinned = "置顶"
    override val sectionAll = "会话"
    override val sectionToday = "今天"
    override val section7Days = "7 天内"
    override val section30Days = "30 天内"
    override val sectionEarlier = "更早"
    override val glassLabel = "毛玻璃 / 半透明"
    override val glassLevelOff = "关"
    override val glassLevelTranslucent = "半透明"
    override val glassLevelFrosted = "毛玻璃"
    override val glassHint = "半透明：弹窗与抽屉半透明。毛玻璃：叠加系统背景实时模糊（需 Android 12+ 且系统动画开启；动画关闭、省电模式或部分 ROM 不支持时自动退化为半透明）"

}

internal object ZhTWStrings : AppStrings {
    override val appName = "LLMChatMobile"
    override val newConversation = "開啟新對話"
    override val settings = "設定"
    override val emptyConversations = "尚無對話記錄"
    override val inputPlaceholder = "給 AI 傳送訊息"
    override val send = "傳送"
    override val thinking = "深度思考"
    override val attachFile = "上傳檔案"
    override val sessionTokens = "Token"
    override val tokenSummary = "Token 統計"
    override val inputTokens = "輸入 Token"
    override val outputTokens = "輸出 Token"
    override val totalTokens = "總消耗"
    override val thisRoundFormat = "本輪消耗：%1\$d Token（輸入 %2\$d + 輸出 %3\$d）"
    override val rename = "重新命名"
    override val renameTitle = "重新命名對話"
    override val cancel = "取消"
    override val save = "儲存"
    override val back = "返回"
    override val apiConfig = "API 設定"
    override val appearance = "外觀設定"
    override val languageSetting = "語言設定"
    override val provider = "供應商"
    override val custom = "自訂"
    override val baseUrl = "Base URL"
    override val apiKey = "API Key"
    override val model = "模型"
    override val modelHint = "模型名稱"
    override val systemPrompt = "系統提示詞"
    override val darkMode = "深色模式"
    override val lightMode = "淺色模式"
    override val simplifiedChinese = "简体中文"
    override val traditionalChinese = "繁體中文"
    override val english = "English (US)"
    override val requestFailed = "請求失敗"
    override val loading = "載入中"
    override val expandMenu = "開啟選單"
    override val otherSettings = "其他設定"
    override val maxContext = "最大上下文 Token"
    override val maxOutput = "最大輸出 Token"
    override val autoCompress = "自動壓縮上下文"
    override val copy = "複製"
    override val exportCrashLogs = "匯出崩潰日誌"
    override val quote = "引用"
    override val copied = "已複製"
    override val archive = "歸檔"
    override val archivedConversations = "恢復對話"
    override val noArchivedConversations = "暫無歸檔對話"
    override val tutorialLabel = "使用教學"
    override val promptTemplateLibrary = "提示詞模板庫"
    override val flowTitle = "會話流程圖"
    override val flowEmpty = "還沒有訊息，傳送第一條後這裡會顯示對話流程。"
    override val flowTapToSwitch = "點按切換到這版"
    override val ttsSection = "語音朗讀"
    override val ttsVoiceLabel = "音色"
    override val ttsVoiceStella = "海星（女聲）"
    override val ttsVoiceTide = "暗潮（男聲）"
    override val ttsFailed = "語音合成失敗，請稍後再試"
    override val ttsAutoRead = "自動朗讀新回覆"
    override val ttsCostHint = "自動朗讀價格非常昂貴，餘額消耗速度極快，謹慎開啟"
    override val ttsEngineLabel = "朗讀引擎"
    override val ttsEngineDeepseek = "DeepSeek"
    override val ttsEngineQwen = "通義千問"
    override val ttsEngineGlm = "智譜 GLM"
    override val ttsHintQwen = "千問朗讀自動使用「通義千問」配置的 API Key，單次最長 600 字"
    override val ttsHintGlm = "GLM 朗讀自動使用「智譜 GLM」配置的 API Key，單次最長 1024 字"
    override val ttsMenuSpeak = "朗讀這條回覆"
    override val ttsMenuStop = "停止朗讀"
    override val netRetryLabel = "網路重試次數（0~5，0=不重試）"
    override val netRetryHint = "網路波動或限流時自動重試，次數可調；超過上限後正常報錯"
    override val exportAllData = "匯出全部資料"
    override val importAllData = "匯入全部資料"
    override val backupHint = "備份包含全部設定、API 設定、會話與訊息、token 統計；API Key 不會匯出，匯入後需重新填寫"
    override val importConfirmTitle = "確認匯入？"
    override val importConfirmBody = "匯入將清空並替換現有全部資料（會話、配置、設定），且無法撤銷。建議先匯出目前資料留底。"
    override val importConfirmConfirm = "匯入並替換"
    override val importRestartTitle = "匯入完成"
    override val importRestartBody = "資料已恢復。重啟應用後生效。"
    override val importRestartButton = "立即重啟"
    override val netRetryBannerFormat = "網路異常，正在重試（第 %1\$d 次 / 最多 %2\$d 次）…"
    override val restore = "恢復"
    override val delete = "刪除"
    override val deleteConfirmTitle = "確認永久刪除？"
    override val addConfig = "新增配置"
    override val edit = "編輯"
    override val setDefault = "設為預設"
    override val defaultLabel = "預設"
    override val noConfig = "暫無配置"
    override val editConfig = "編輯配置"
    override val configName = "配置名稱"
    override val supportReasoning = "支援深度思考"
    override val about = "關於"
    override val versionLabel = "版本"
    override val developer = "開發者"
    override val githubRepo = "GitHub 倉庫"
    override val iKnow = "我已知曉"
    override val testNotice = "注意！這是 LLMChatMobile 的 beta 測試版本。\n測試版不代表本產品的最終品質，感謝您的理解與支持，祝您好運！"
    override val regenerate = "重新生成"
    override val editPrompt = "編輯提示詞"
    override val editPromptHint = "留空則使用全域預設提示詞"
    override val export = "匯出對話"
    override val exportMarkdown = "Markdown 格式"
    override val exportPlainText = "純文字格式"
    override val exportSuccess = "已匯出到"
    override val followSystem = "跟隨系統"
    override val visionUnsupported = "目前模型不支援圖片輸入"
    override val upload = "上傳"
    override val uploadImage = "上傳圖片"
    override val imageLimitHint = "一條訊息最多 4 張圖片"
    override val last1Day = "近 1 天"
    override val last7Days = "近 7 天"
    override val welcomeTitle = "歡迎使用 LLMChatMobile"
    override val welcomeSubtitle = "輕量原生的多模型 AI 聊天客戶端"
    override val next = "下一步"
    override val skip = "跳過"
    override val configApiTitle = "設定你的 AI 服務"
    override val apiGuideText = "1. 開啟 DeepSeek 開放平台 platform.deepseek.com 註冊帳號\n2. 登入後在「API Keys」頁面點擊建立金鑰\n3. 複製生成的 API Key（形如 sk-xxxx），妥善儲存\n4. 點擊下方「已取得 API Key」填入即可開始使用"
    override val goDeepSeek = "前往 DeepSeek 開放平台"
    override val gotApiKey = "已取得 API Key"
    override val useOtherProvider = "使用其他供應商 API"
    override val verifyConfig = "驗證設定"
    override val configSuccess = "設定成功"
    override val configFail = "驗證失敗，請檢查 API Key、網路或餘額"
    override val quickGuideTitle = "快速上手指南"
    override val guideMarkdown = "## 模式選擇\n新建對話時可選擇 8 種模式：聊天、工作、代碼開發、角色扮演（支援角色卡快捷表單）、創意寫作、自訂、DS 網頁版與 Legacy。每個模式綁定不同的回覆風格與參數；傳送第一條訊息後模式鎖定，可在回覆下方切換多版本。\n\n## 基礎對話\n在底部輸入框輸入內容，點擊右側箭頭傳送，回覆逐字串流顯示。生成中可再次點擊按鈕停止。長按任意訊息可複製全文；長按自己傳送的訊息可編輯後重送。\n\n## 聯網搜尋與深度思考\n輸入欄右側「搜尋」「思考」開關：搜尋讓回答引用最新網頁資訊（顯示已搜尋網頁數）；思考展示推理過程（半透明小字，可摺疊）。\n\n## 多模態上傳\n點輸入欄 📎 按鈕可附加圖片，支援視覺模型辨識圖片內容。\n\n## 多版本回覆\n「重新生成」不再覆蓋舊回答：同一提問的多版回答全部保留，回覆下方 ‹ 2/3 › 可切換版本，每個版本獨立統計 Token 消耗。長按自己的訊息可編輯重送，產生新一輪對話。\n\n## 模型與 API\n點擊輸入欄左側模型名快速切換已儲存的 API 設定；設定 → API 設定可新增多套設定（供應商/位址/金鑰/模型），進階選項可單獨限制上下文與輸出長度。\n\n## Token 統計\n側邊欄底部「Token 統計」查看累計消耗、按日趨勢與模型分佈。\n\n## 會話管理\n左上角選單展開工作階段列表：長按工作階段可重新命名、封存、匯出 Markdown 或永久刪除；設定裡可恢復已封存工作階段。"
    override val allDone = "完成！"
    override val backToGuide = "返回指引"
    override val skipHint = "跳過後仍可在設定中設定 API"
    override val searchWeb = "連網搜尋"
    override val searching = "正在連網搜尋..."
    override val searchDone = "已連網搜尋"
    override val configCustomModel = "設定自訂模型"
    override val priceCheap = "便宜"
    override val priceMedium = "中等"
    override val priceHigh = "昂貴"
    override val priceVeryHigh = "非常昂貴"
    override val searchedPagesFormat = "已搜尋 %d 個網頁"
    override val defaultModeLabel = "新對話預設模式"
    override val defaultModeOff = "停用（新建時手動選擇）"
    override val advancedOptions = "進階選項"
    override val newConversationTitle = "新建對話"
    override val createConversation = "建立對話"
    override val modeUserPromptLabel = "你的補充提示詞"
    override val customTemperature = "溫度（隨機性）"
    override val customTopP = "Top P（多樣性）"
    override val modeLocked = "已傳送訊息後模式不可修改"
    override val promptTruncated = "系統提示詞過長已自動截斷"
    override val modeLabel = "模式"
    override val modeSelectTitle = "選擇模式"
    override val searchShort = "搜尋"
    override val thinkingShort = "思考"
    override val errGuideAuth = "密鑰無效 / 密鑰填寫錯誤，請檢查 API 密鑰"
    override val errGuideBalance = "餘額不足，請前往服務商儲值"
    override val errGuideRateLimit = "請求頻率超限，請降低發送速度或稍後重試"
    override val errGuideTimeout = "網路連線逾時，請檢查網路、代理、API 位址"
    override val errGuideNetwork = "無網路連線，請檢查裝置網路"
    override val errGuideUnknown = "請求失敗，請核對設定或稍後重試"
    override val roleplayParenLabel = "動作、神態、環境描寫統一用（）包裹"
    override val modePreviewTitle = "模式預覽"
    override val modePreviewUserLayer = "用戶層提示詞"
    override val modePreviewRoleCard = "角色卡"
    override val modePreviewCopy = "複製全文"
    override val modePreviewCopied = "已複製"
    override val searchSettingLabel = "聯網搜索"
    override val searchProxyHint = "DeepSeek 原生聯網搜索；其他模型將先由 DeepSeek-Flash 聯網搜索，再把結果交給當前模型作答（消息內容會額外發送給 DeepSeek）"
    override val userNicknameLabel = "用戶稱呼"
    override val modeExport = "導出"
    override val modeImport = "導入"
    override val modeImportFailed = "導入失敗：不是有效的模式配置檔案"
    override val aboutDownload = "下載最新安裝包"
    override val preferencesSetting = "偏好設定"
    override val tokenSavingUltimate = "終極"
    override val tokenSavingUltimateWarning = "⚠ 終極檔：輸出與上下文極限壓縮，並禁用聯網搜索與深度思考，只能在省錢模式對話中發送消息。不建議開啟，除非餘額很少。"
    override val tokenCacheHit = "緩存命中"
    override val colorPresetLabel = "配色預設"
    override val exportJson = "JSON 格式"
    override val presetLightGroup = "淺色配色"
    override val presetDarkGroup = "深色配色"
    override val presetLightDefault = "預設"
    override val presetLightBlue = "淺藍"
    override val presetLightGreen = "淺綠"
    override val presetLightYellow = "黃"
    override val presetLightGray = "淺灰"
    override val presetLightPink = "淺粉"
    override val presetDarkDefault = "預設"
    override val presetDarkBlue = "藍"
    override val presetDarkGreen = "綠"
    override val presetDarkBrown = "棕"
    override val presetDarkBlack = "純黑"
    override val presetDarkRed = "深紅"
    override val backgroundLabel = "聊天背景圖"
    override val pickImage = "選擇圖片"
    override val clearImage = "清除"
    override val contextStatsTitle = "上下文統計"
    override val contextCapacity = "上下文容量"
    override val contextUsed = "已使用"
    override val segMessages = "消息"
    override val segReasoning = "深度思考"
    override val segSearch = "聯網搜索"
    override val segSystem = "系統提示詞"
    override val manualCompressButton = "手動壓縮上下文"
    override val tokenSavingLabel = "省 Token 檔位"
    override val tokenSavingOff = "關"
    override val tokenSavingOffHint = "不限制輸出，不自動壓縮上下文"
    override val tokenSavingQuality = "品質"
    override val tokenSavingQualityHint = "單次輸出≤4096；上下文達 12 萬自動壓縮"
    override val tokenSavingMedium = "中等"
    override val tokenSavingMediumHint = "單次輸出≤1024；上下文達 5 萬自動壓縮"
    override val tokenSavingMax = "最大"
    override val tokenSavingMaxHint = "單次輸出≤256；上下文達 3 萬自動壓縮；禁用聯網搜索與深度思考"
    override val creditsButton = "開發者名單"
    override val creditsTitle = "開發者名單"
    override val creditsDev = "開發者"
    override val creditsPlan = "策劃"
    override val creditsMainCode = "主要代碼"
    override val creditsImpl = "功能實現"
    override val creditsUiDesign = "UI 與設計"
    override val creditsMentor = "開發指導"
    override val creditsGithub = "GitHub"
    override val contextInjectionLabel = "自動注入環境資訊"
    override val contextInjectionHint = "在 system 末尾附帶當前時間、地區與客戶端資訊（DS 網頁版模式始終開啟）"
    override val regionLabel = "地區"
    override val regionPlaceholder = "跟隨裝置地區，可手動填寫城市名"
    override val costThisLabel = "本次"
    override val costTotalLabel = "累計"
    override val searchInConversation = "會話內搜尋"
    override val searchHint = "搜尋訊息"
    override val searchEmpty = "無符合訊息"
    override val compressPreviewBodyFormat = "將保留最近 4 條訊息，其餘 %1\$d 條壓縮為不超過 %2\$d 字的 AI 摘要，原細節不可回溯。確認執行？"
    override val compressConfirmBtn = "確認壓縮"
    override val followSystemHint = "開啟後按系統深淺自動切換淺色 / 深色配色組"
    override val compressLimitLabel = "壓縮摘要上限（字）"
    override val compressLimitHint = "手動壓縮時 AI 摘要的字數上限，100~1024（預設 400）"
    override val pinned = "置頂"
    override val unpinned = "取消置頂"
    override val sectionPinned = "置頂"
    override val sectionAll = "會話"
    override val sectionToday = "今天"
    override val section7Days = "7 天內"
    override val section30Days = "30 天內"
    override val sectionEarlier = "更早"
    override val glassLabel = "毛玻璃 / 半透明"
    override val glassLevelOff = "關"
    override val glassLevelTranslucent = "半透明"
    override val glassLevelFrosted = "毛玻璃"
    override val glassHint = "半透明：彈窗與抽屜半透明。毛玻璃：疊加系統背景即時模糊（需 Android 12+ 且系統動畫開啟；動畫關閉、省電模式或部分 ROM 不支援時自動退化為半透明）"

}

internal object EnUSStrings : AppStrings {
    override val appName = "LLMChatMobile"
    override val newConversation = "New chat"
    override val settings = "Settings"
    override val emptyConversations = "No conversations yet"
    override val inputPlaceholder = "Message AI"
    override val send = "Send"
    override val thinking = "Deep thinking"
    override val attachFile = "Attach file"
    override val sessionTokens = "Tokens"
    override val tokenSummary = "Token usage"
    override val inputTokens = "Input tokens"
    override val outputTokens = "Output tokens"
    override val totalTokens = "Total"
    override val thisRoundFormat = "This turn: %1\$d tokens (in %2\$d + out %3\$d)"
    override val rename = "Rename"
    override val renameTitle = "Rename conversation"
    override val cancel = "Cancel"
    override val save = "Save"
    override val back = "Back"
    override val apiConfig = "API configuration"
    override val appearance = "Appearance"
    override val languageSetting = "Language"
    override val provider = "Provider"
    override val custom = "Custom"
    override val baseUrl = "Base URL"
    override val apiKey = "API Key"
    override val model = "Model"
    override val modelHint = "Model name"
    override val systemPrompt = "System prompt"
    override val darkMode = "Dark mode"
    override val lightMode = "Light mode"
    override val simplifiedChinese = "简体中文"
    override val traditionalChinese = "繁體中文"
    override val english = "English (US)"
    override val requestFailed = "Request failed"
    override val loading = "Loading"
    override val expandMenu = "Open menu"
    override val otherSettings = "Other settings"
    override val maxContext = "Max context tokens"
    override val maxOutput = "Max output tokens"
    override val autoCompress = "Auto-compress context"
    override val copy = "Copy"
    override val exportCrashLogs = "Export crash logs"
    override val quote = "Quote"
    override val copied = "Copied"
    override val archive = "Archive"
    override val archivedConversations = "Restore chats"
    override val noArchivedConversations = "No archived chats"
    override val tutorialLabel = "Tutorial"
    override val promptTemplateLibrary = "Prompt templates"
    override val flowTitle = "Chat flow"
    override val flowEmpty = "No messages yet — the flow appears after your first message."
    override val flowTapToSwitch = "Tap to switch"
    override val ttsSection = "Read aloud"
    override val ttsVoiceLabel = "Voice"
    override val ttsVoiceStella = "Stella (female)"
    override val ttsVoiceTide = "Tide (male)"
    override val ttsFailed = "Speech synthesis failed, try again later"
    override val ttsAutoRead = "Auto read new replies"
    override val ttsCostHint = "Auto-read is extremely expensive and drains balance very fast - enable with caution."
    override val ttsEngineLabel = "TTS engine"
    override val ttsEngineDeepseek = "DeepSeek"
    override val ttsEngineQwen = "Qwen"
    override val ttsEngineGlm = "Zhipu GLM"
    override val ttsHintQwen = "Qwen TTS automatically uses the API key of the Qwen provider config; max 600 characters per request"
    override val ttsHintGlm = "GLM TTS automatically uses the API key of the Zhipu GLM provider config; max 1024 characters per request"
    override val ttsMenuSpeak = "Read this reply"
    override val ttsMenuStop = "Stop reading"
    override val netRetryLabel = "Network retries (0-5, 0 = off)"
    override val netRetryHint = "Auto-retries on network errors or rate limits; normal error after the limit"
    override val exportAllData = "Export all data"
    override val importAllData = "Import all data"
    override val backupHint = "Backup includes settings, API configs, conversations & messages, token stats. API keys are NOT exported - re-enter them after import."
    override val importConfirmTitle = "Import backup?"
    override val importConfirmBody = "Importing replaces ALL existing data (conversations, configs, settings). This cannot be undone - consider exporting a backup first."
    override val importConfirmConfirm = "Import & replace"
    override val importRestartTitle = "Import complete"
    override val importRestartBody = "Data restored. Restart the app to apply."
    override val importRestartButton = "Restart now"
    override val netRetryBannerFormat = "Network error, retrying (attempt %1\$d of %2\$d)..."
    override val restore = "Restore"
    override val delete = "Delete"
    override val deleteConfirmTitle = "Delete permanently?"
    override val addConfig = "Add config"
    override val edit = "Edit"
    override val setDefault = "Set default"
    override val defaultLabel = "Default"
    override val noConfig = "No configs"
    override val editConfig = "Edit config"
    override val configName = "Config name"
    override val supportReasoning = "Supports reasoning"
    override val about = "About"
    override val versionLabel = "Version"
    override val developer = "Developer"
    override val githubRepo = "GitHub Repository"
    override val iKnow = "Got it"
    override val testNotice = "Note! This is a beta test version of LLMChatMobile.\nThe test version does not represent the final quality of this product. Thank you for your understanding and support. Good luck!"
    override val regenerate = "Regenerate"
    override val editPrompt = "Edit prompt"
    override val editPromptHint = "Leave blank to use the global default prompt"
    override val export = "Export"
    override val exportMarkdown = "Markdown"
    override val exportPlainText = "Plain text"
    override val exportSuccess = "Exported to"
    override val followSystem = "Follow system"
    override val visionUnsupported = "Current model does not support image input"
    override val upload = "Upload"
    override val uploadImage = "Upload image"
    override val imageLimitHint = "Up to 4 images per message"
    override val last1Day = "Last 1 day"
    override val last7Days = "Last 7 days"
    override val welcomeTitle = "Welcome to LLMChatMobile"
    override val welcomeSubtitle = "A lightweight native multi-model AI chat client"
    override val next = "Next"
    override val skip = "Skip"
    override val configApiTitle = "Set up your AI service"
    override val apiGuideText = "1. Go to platform.deepseek.com and sign up\n2. Open the API Keys page and create a new key\n3. Copy the API Key (starts with sk-) and keep it safe\n4. Tap \"I have an API Key\" below to continue"
    override val goDeepSeek = "Go to DeepSeek Platform"
    override val gotApiKey = "I have an API Key"
    override val useOtherProvider = "Use another provider"
    override val verifyConfig = "Verify"
    override val configSuccess = "Configuration successful"
    override val configFail = "Verification failed. Check your API Key, network or balance"
    override val quickGuideTitle = "Quick Start Guide"
    override val guideMarkdown = "## Pick a mode\nChoose from 8 modes when starting a chat: Chat, Work, Code, Roleplay (with quick role-card form), Creative, Custom, DS Web and Legacy. Each mode binds its own style and parameters. Mode locks after your first message; switch reply versions below a response.\n\n## Basic chat\nType and tap the send arrow; replies stream in. Tap the same button again to stop. Long-press any message to copy; long-press your own message to edit and resend.\n\n## Web search & thinking\nUse the Search / Think toggles in the input bar: search cites fresh web results (shows pages found); thinking reveals the reasoning (dimmed, collapsible).\n\n## Multimodal\nTap the paperclip to attach images; vision models can read them.\n\n## Multi-version replies\nRegenerate keeps old answers: all versions stay under one question, switch with the arrows, each version has its own token usage. Long-press your message to edit and resend for a new round.\n\n## Models & API\nTap the model name to switch saved API configs; Settings -> API Config manages multiple profiles (provider / URL / key / model) with per-config context & output limits under Advanced.\n\n## Token stats\nOpen Token Stats at the drawer bottom for totals, daily trends and per-model breakdown.\n\n## Conversations\nOpen the drawer to manage chats: long-press to rename, archive, export Markdown or delete forever; restore archived chats in Settings."
    override val allDone = "All set!"
    override val backToGuide = "Back to guide"
    override val skipHint = "You can still configure the API in Settings later"
    override val searchWeb = "Web search"
    override val searching = "Searching the web..."
    override val searchDone = "Searched the web"
    override val configCustomModel = "Configure custom models"
    override val priceCheap = "Cheap"
    override val priceMedium = "Medium"
    override val priceHigh = "Pricey"
    override val priceVeryHigh = "Very pricey"
    override val searchedPagesFormat = "Searched %d pages"
    override val defaultModeLabel = "Default mode for new chats"
    override val defaultModeOff = "Off (choose when creating)"
    override val advancedOptions = "Advanced"
    override val newConversationTitle = "New chat"
    override val createConversation = "Create chat"
    override val modeUserPromptLabel = "Your extra prompt"
    override val customTemperature = "Temperature (randomness)"
    override val customTopP = "Top P (diversity)"
    override val modeLocked = "Mode is locked after the first message"
    override val promptTruncated = "System prompt too long, truncated automatically"
    override val modeLabel = "Mode"
    override val modeSelectTitle = "Choose a mode"
    override val searchShort = "Search"
    override val thinkingShort = "Think"
    override val errGuideAuth = "Invalid or incorrect API key — please check your API key"
    override val errGuideBalance = "Insufficient balance — please top up at your provider"
    override val errGuideRateLimit = "Rate limit exceeded — slow down or retry later"
    override val errGuideTimeout = "Connection timed out — check network, proxy or API URL"
    override val errGuideNetwork = "No network connection — check your device network"
    override val errGuideUnknown = "Request failed — check your configuration or retry later"
    override val roleplayParenLabel = "Wrap actions, expressions and scene descriptions in parentheses"
    override val modePreviewTitle = "Mode Preview"
    override val modePreviewUserLayer = "User-layer prompt"
    override val modePreviewRoleCard = "Role card"
    override val modePreviewCopy = "Copy all"
    override val modePreviewCopied = "Copied"
    override val searchSettingLabel = "Web search"
    override val searchProxyHint = "DeepSeek uses native web search; other models search via DeepSeek-Flash first, then answer with the current model (message content is additionally sent to DeepSeek)"
    override val userNicknameLabel = "User nickname"
    override val modeExport = "Export"
    override val modeImport = "Import"
    override val modeImportFailed = "Import failed: not a valid mode config file"
    override val aboutDownload = "Download latest APK"
    override val preferencesSetting = "Preferences"
    override val tokenSavingUltimate = "Ultimate"
    override val tokenSavingUltimateWarning = "⚠ Ultimate tier: extreme output and context compression, web search and thinking disabled; messages are only allowed in Economy mode chats. Not recommended unless your balance is nearly empty."
    override val tokenCacheHit = "Cache hit"
    override val colorPresetLabel = "Color preset"
    override val exportJson = "JSON format"
    override val presetLightGroup = "Light presets"
    override val presetDarkGroup = "Dark presets"
    override val presetLightDefault = "Default"
    override val presetLightBlue = "Light blue"
    override val presetLightGreen = "Light green"
    override val presetLightYellow = "Yellow"
    override val presetLightGray = "Light gray"
    override val presetLightPink = "Light pink"
    override val presetDarkDefault = "Default"
    override val presetDarkBlue = "Blue"
    override val presetDarkGreen = "Green"
    override val presetDarkBrown = "Brown"
    override val presetDarkBlack = "Pure black"
    override val presetDarkRed = "Deep red"
    override val backgroundLabel = "Chat background"
    override val pickImage = "Pick image"
    override val clearImage = "Clear"
    override val contextStatsTitle = "Context stats"
    override val contextCapacity = "Context capacity"
    override val contextUsed = "Used"
    override val segMessages = "Messages"
    override val segReasoning = "Reasoning"
    override val segSearch = "Web search"
    override val segSystem = "System prompt"
    override val manualCompressButton = "Compress context"
    override val tokenSavingLabel = "Token saving"
    override val tokenSavingOff = "Off"
    override val tokenSavingOffHint = "No output cap, no context compression"
    override val tokenSavingQuality = "Quality"
    override val tokenSavingQualityHint = "Output ≤4096; compress context at 120k"
    override val tokenSavingMedium = "Medium"
    override val tokenSavingMediumHint = "Output ≤1024; compress context at 50k"
    override val tokenSavingMax = "Max"
    override val tokenSavingMaxHint = "Output ≤256; compress context at 30k; web search and thinking disabled"
    override val creditsButton = "Credits"
    override val creditsTitle = "Credits"
    override val creditsDev = "Developer"
    override val creditsPlan = "Planning"
    override val creditsMainCode = "Main code"
    override val creditsImpl = "Feature implementation"
    override val creditsUiDesign = "UI & design"
    override val creditsMentor = "Development mentoring"
    override val creditsGithub = "GitHub"
    override val contextInjectionLabel = "Auto-inject context info"
    override val contextInjectionHint = "Append current time, region and client info to the system prompt (always on for DS Web mode)"
    override val regionLabel = "Region"
    override val regionPlaceholder = "Follows device; tap to enter a city"
    override val costThisLabel = "This chat"
    override val costTotalLabel = "Total"
    override val searchInConversation = "Search in chat"
    override val searchHint = "Search messages"
    override val searchEmpty = "No matching messages"
    override val compressPreviewBodyFormat = "Keep the last 4 messages; the other %1\$d will be compressed into an AI digest of at most %2\$d characters. Details cannot be recovered. Continue?"
    override val compressConfirmBtn = "Compress"
    override val followSystemHint = "When on, follows system dark mode to switch between the light / dark preset groups"
    override val compressLimitLabel = "Summary size limit (chars)"
    override val compressLimitHint = "Max characters of the AI digest on manual compress, 100~1024 (default 400)"
    override val pinned = "Pin to top"
    override val unpinned = "Unpin"
    override val sectionPinned = "Pinned"
    override val sectionAll = "Chats"
    override val sectionToday = "Today"
    override val section7Days = "Last 7 days"
    override val section30Days = "Last 30 days"
    override val sectionEarlier = "Earlier"
    override val glassLabel = "Glass material"
    override val glassLevelOff = "Off"
    override val glassLevelTranslucent = "Translucent"
    override val glassLevelFrosted = "Frosted"
    override val glassHint = "Translucent: dialogs and drawer become translucent. Frosted: adds live system background blur (needs Android 12+ with system animations on; degrades to translucent when animations are off, in battery saver, or on ROMs without support)"

}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.SIMPLIFIED_CHINESE -> ZhCNStrings
    AppLanguage.TRADITIONAL_CHINESE -> ZhTWStrings
    AppLanguage.ENGLISH -> EnUSStrings
}

internal val LocalAppStrings: androidx.compose.runtime.ProvidableCompositionLocal<AppStrings> = androidx.compose.runtime.staticCompositionLocalOf<AppStrings> { ZhCNStrings }



/**
 * 桌面端统一提供入口：LocalAppStrings + 玻璃档一次挂载。
 * （Main 所在包里 infix provides 解析失败的原因未明，故把 Provider 收拢到本文件。）
 */
@androidx.compose.runtime.Composable
internal fun ProvideAppStrings(
    language: com.bamboomosaic.llmchat.core.domain.AppLanguage,
    glassLevel: Int,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppStrings provides stringsFor(language),
        com.bamboomosaic.llmchat.feature.chat.LocalGlassLevel provides glassLevel,
        content = content,
    )
}
