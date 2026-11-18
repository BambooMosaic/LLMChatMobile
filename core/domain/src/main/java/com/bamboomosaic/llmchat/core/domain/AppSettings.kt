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
}

/** 应用设置：提示词、模型、API 端点、主题、语言、思考模式。 */
data class AppSettings(
    val systemPrompt: String,
    val model: String,
    val baseUrl: String,
    val apiKey: String,
    val providerId: String = Providers.CUSTOM,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.SIMPLIFIED_CHINESE,
    val thinkingEnabled: Boolean = false,
    val maxContextTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
    val autoCompressEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_MODEL = "gpt-6-luna"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_API_KEY = ""
        const val DEFAULT_MAX_CONTEXT_TOKENS = 80000
        const val DEFAULT_MAX_OUTPUT_TOKENS = 4096

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
    suspend fun setMaxContextTokens(value: Int)
    suspend fun setMaxOutputTokens(value: Int)
    suspend fun setAutoCompressEnabled(enabled: Boolean)
}
