package com.bamboomosaic.llmchat.core.domain

import kotlinx.coroutines.flow.Flow

/** 应用设置：系统提示词、模型名、API 端点和密钥。 */
data class AppSettings(
    val systemPrompt: String,
    val model: String,
    val baseUrl: String,
    val apiKey: String,
) {
    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant."
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_API_KEY = ""
    }
}

/** 设置仓储，ViewModel 只注入此接口。 */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setSystemPrompt(value: String)
    suspend fun setModel(value: String)
    suspend fun setBaseUrl(value: String)
    suspend fun setApiKey(value: String)
}
