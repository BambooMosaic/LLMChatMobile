package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

/** 应用主题模式。首次启动默认深色。SYSTEM = 跟随系统。 */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** 应用语言，切换后界面文案与 AI 回复语言同步。 */
enum class AppLanguage(val tag: String) {
    SIMPLIFIED_CHINESE("zh-CN"),
    TRADITIONAL_CHINESE("zh-TW"),
    ENGLISH("en-US");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: SIMPLIFIED_CHINESE
    }
}

/** AI 供应商预设。 */
data class ProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val models: List<String>,
    /** 该供应商的预设模型是否默认支持"深度思考"。 */
    val supportReasoning: Boolean = false,
)

object Providers {
    const val CUSTOM = "custom"

    val all: List<ProviderPreset> = listOf(
        ProviderPreset(
            id = CUSTOM,
            displayName = "自定义",
            baseUrl = "",
            models = emptyList(),
        ),
        ProviderPreset(
            id = "deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            models = listOf("deepseek-flash", "deepseek-v4-pro"),
            supportReasoning = true,
        ),
        ProviderPreset(
            id = "openai",
            displayName = "OpenAI ChatGPT",
            baseUrl = "https://api.openai.com/v1",
            models = listOf("gpt-6-astra", "gpt-6.1-sol", "gpt-6-luna"),
            supportReasoning = true,
        ),
        ProviderPreset(
            id = "glm",
            displayName = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            models = listOf("glm-5.3", "glm-5.3-flash"),
            supportReasoning = true,
        ),
        ProviderPreset(
            id = "ollama",
            displayName = "Ollama",
            baseUrl = "http://localhost:11434/v1",
            models = emptyList(),
        ),
        ProviderPreset(
            id = "qwen",
            displayName = "通义千问",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            models = listOf("qwen3.8-max", "qwen-plus", "qwen-turbo"),
        ),
        ProviderPreset(
            id = "moonshot",
            displayName = "Moonshot Kimi",
            baseUrl = "https://api.moonshot.cn/v1",
            models = listOf("kimi-k2-turbo", "moonshot-v1-128k"),
            supportReasoning = true,
        ),
    )

    fun find(id: String): ProviderPreset = all.firstOrNull { it.id == id } ?: all.first()

    /** 支持视觉（多模态）的模型集合。DeepSeek 实际可用模型中仅 deepseek-flash 支持 image 输入。 */
    val visionModels: Set<String> = setOf("deepseek-flash")

    fun supportsVision(model: String): Boolean = model in visionModels
}

/** 应用设置：提示词、模型、API 端点、主题、语言、思考模式。 */
data class AppSettings(
    val systemPrompt: String,
    val model: String,
    val baseUrl: String,
    val apiKey: String,
    val providerId: String = "deepseek",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SIMPLIFIED_CHINESE,
    val thinkingEnabled: Boolean = false,
    /** 联网搜索开关（仅 DeepSeek 支持）。 */
    val searchEnabled: Boolean = false,
    val maxContextTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
    val autoCompressEnabled: Boolean = true,
    /** 新对话默认模式 ID；"off" = 禁用（新建时手动选择）。 */
    val defaultModeId: String = "off",
    /** 自动注入环境信息开关（ds_web 强制开启，无视此开关）。 */
    val contextInjectionEnabled: Boolean = true,
    /** 省 Token 档位：0=关 1=质量 2=中等 3=最大。 */
    val tokenSavingLevel: Int = 0,
    /** 角色扮演模式：动作神态环境描写统一用（）包裹。 */
    val roleplayParenRule: Boolean = true,
    /** 用户称呼（可选）；非空时注入环境上下文，DS 网页版模式除外。 */
    val userNickname: String = "",
    /** 20 轮：聊天页面自定义背景图（应用内文件路径；空 = 默认背景）。 */
    val backgroundImageUri: String = "",
    /** 20 轮：UI 配色预设 ID。22 轮起 id 自带 light_/dark_ 前缀；跟随系统关闭时为生效值。 */
    val colorPreset: String = "dark_default",
    /** 23 轮：跟随系统明暗——开启时按系统深浅在浅色/深色两组预设间按位置配对切换（默认↔默认、黄↔棕…）。 */
    val followSystemTheme: Boolean = false,
    /** 23 轮：手动压缩摘要的字数上限（100~1024，默认 400）。 */
    val compressSummaryLimit: Int = 400,
    /** 25 轮：玻璃材质档位 0=关 1=半透明 2=毛玻璃（毛玻璃需设备支持系统模糊）。 */
    val glassLevel: Int = 0,
    /** 27 轮：语音朗读（DeepSeek TTS）入口显示开关。 */
    val ttsEnabled: Boolean = true,
    /** 27 轮：TTS 音色（DeepSeek voice_id：stella=海星女声 / tide=暗潮男声）。 */
    val ttsVoice: String = "stella",
    /** 27 轮修正：AI 回复完成后自动朗读（默认关——DeepSeek TTS 按字符计费，长回复消耗明显）。 */
    val ttsAutoRead: Boolean = false,
    /** 28 轮：网络 / 429 自动重试次数（29 轮起固定 3 次，不再提供设置项）。 */
    val netRetryCount: Int = 3,
    /** 29 轮：TTS 朗读引擎（deepseek / qwen / glm）。 */
    val ttsProvider: String = "deepseek",
    /** 29 轮：通义千问 TTS 音色（预置如 Cherry/Serena/Ethan，大小写敏感）。 */
    val ttsVoiceQwen: String = "Cherry",
    /** 29 轮：智谱 GLM TTS 音色（预置 tongtong/chuichui…）。 */
    val ttsVoiceGlm: String = "tongtong",
) {
    companion object {
        const val DEFAULT_MODEL = "deepseek-flash"
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_API_KEY = ""
        const val DEFAULT_MAX_CONTEXT_TOKENS = 1_000_000
        const val DEFAULT_MAX_OUTPUT_TOKENS = 4096
        const val DEFAULT_MODE_OFF = "off"

        /** 各语言的默认系统提示词（systemPrompt 为空时使用）。 */
        fun defaultSystemPrompt(language: AppLanguage): String = when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> "你是一个乐于助人的 AI 助手，请用简体中文回答。"
            AppLanguage.TRADITIONAL_CHINESE -> "你是一個樂於助人的 AI 助手，請用繁體中文回答。"
            AppLanguage.ENGLISH -> "You are a helpful AI assistant. Always respond in English."
        }
    }
}

/** 设置仓储，ViewModel 只注入此接口。 */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    /** 当前选中的 API 配置 id；0 表示尚未选择（用默认配置兜底）。 */
    fun observeCurrentConfigId(): Flow<Long>

    suspend fun setCurrentConfigId(id: Long)

    /** 是否首次启动（用于欢迎弹窗）。 */
    fun observeFirstLaunch(): Flow<Boolean>

    suspend fun setFirstLaunchDone()

    suspend fun setSystemPrompt(value: String)
    suspend fun setModel(value: String)
    suspend fun setBaseUrl(value: String)
    suspend fun setApiKey(value: String)
    suspend fun setProvider(providerId: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThinkingEnabled(enabled: Boolean)
    suspend fun setSearchEnabled(enabled: Boolean)
    suspend fun setMaxContextTokens(value: Int)
    suspend fun setMaxOutputTokens(value: Int)
    suspend fun setAutoCompressEnabled(enabled: Boolean)

    /** 新对话默认模式（"off" = 禁用）。 */
    suspend fun setDefaultModeId(modeId: String)

    /** 自动注入环境信息开关。 */
    suspend fun setContextInjectionEnabled(enabled: Boolean)

    /** 省 Token 档位（0-3）。 */
    suspend fun setTokenSavingLevel(level: Int)

    /** 角色扮演模式括号规则开关。 */
    suspend fun setRoleplayParenRule(enabled: Boolean)

    /** 用户称呼（可选，留空不注入）。 */
    suspend fun setUserNickname(value: String)

    /** 20 轮：聊天页自定义背景图（应用内路径；空串 = 使用默认背景）。 */
    suspend fun setBackgroundImageUri(value: String)

    /** 20 轮：UI 配色预设 ID。 */
    suspend fun setColorPreset(value: String)

    /** 23 轮：跟随系统明暗开关。 */
    suspend fun setFollowSystemTheme(value: Boolean)

    /** 23 轮：手动压缩摘要字数上限。 */
    suspend fun setCompressSummaryLimit(value: Int)

    /** 25 轮：玻璃材质档位（0/1/2）。 */
    suspend fun setGlassLevel(value: Int)

    /** 27 轮：语音朗读开关。 */
    suspend fun setTtsEnabled(enabled: Boolean)

    /** 27 轮：TTS 音色（stella / tide）。 */
    suspend fun setTtsVoice(voiceId: String)

    /** 27 轮修正：自动朗读开关。 */
    suspend fun setTtsAutoRead(enabled: Boolean)

    /** 29 轮：TTS 朗读引擎（deepseek / qwen / glm）。 */
    suspend fun setTtsProvider(providerId: String)

    /** 29 轮：通义千问 TTS 音色。 */
    suspend fun setTtsVoiceQwen(voiceId: String)

    /** 29 轮：智谱 GLM TTS 音色。 */
    suspend fun setTtsVoiceGlm(voiceId: String)

    /** 28 轮：会话草稿（空串清除）。 */
    suspend fun setDraft(conversationId: String, text: String)

    suspend fun getDraft(conversationId: String): String

    /**
     * 27 轮修正：大版本更新公告（已完成后首次运行时观察）——
     * 大版本段（major.minor）变化返回 true；首次引导本身不看这个。
     */
    fun observeMajorVersionChanged(): Flow<Boolean>

    /** 27 轮修正：关闭更新公告后记录当前大版本段。 */
    suspend fun markMajorVersionNoticeSeen()

    /** 模式模板记忆：每个模式下用户最后一次填写的用户提示词。 */
    fun observeModeUserPrompt(modeId: String): kotlinx.coroutines.flow.Flow<String?>

    suspend fun setModeUserPrompt(modeId: String, prompt: String?)

    /** 自定义模式缓存的参数。 */
    fun observeModeCustomParams(modeId: String): kotlinx.coroutines.flow.Flow<Pair<Float, Float>?>

    suspend fun setModeCustomParams(modeId: String, temperature: Float, topP: Float)
}
