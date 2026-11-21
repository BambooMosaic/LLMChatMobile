package com.bamboomosaic.llmchat.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bamboomosaic.llmchat.core.domain.AppLanguage
import com.bamboomosaic.llmchat.core.domain.AppSettings
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import com.bamboomosaic.llmchat.core.domain.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

/** 用 DataStore Preferences 保存全部配置项。 */
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val systemPromptKey = stringPreferencesKey("system_prompt")
    private val modelKey = stringPreferencesKey("model")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val providerKey = stringPreferencesKey("provider_id")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val languageKey = stringPreferencesKey("language")
    private val thinkingKey = booleanPreferencesKey("thinking_enabled")
    private val searchKey = booleanPreferencesKey("search_enabled")
    private val maxContextKey = intPreferencesKey("max_context_tokens")
    private val maxOutputKey = intPreferencesKey("max_output_tokens")
    private val autoCompressKey = booleanPreferencesKey("auto_compress_enabled")
    private val currentConfigIdKey = longPreferencesKey("current_config_id")
    private val firstLaunchKey = booleanPreferencesKey("is_first_launch")
    private val defaultModeIdKey = stringPreferencesKey("default_mode_id")
    private val contextInjectionKey = booleanPreferencesKey("context_injection_enabled")
    private val tokenSavingKey = intPreferencesKey("token_saving_level")

    override fun observeSettings(): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val language = AppLanguage.fromTag(prefs[languageKey])
        AppSettings(
            // 未显式设置过提示词时，按当前语言给默认值，实现"AI 语言跟随界面语言"
            systemPrompt = prefs[systemPromptKey] ?: AppSettings.defaultSystemPrompt(language),
            model = prefs[modelKey] ?: AppSettings.DEFAULT_MODEL,
            baseUrl = prefs[baseUrlKey] ?: AppSettings.DEFAULT_BASE_URL,
            apiKey = prefs[apiKeyKey] ?: AppSettings.DEFAULT_API_KEY,
            providerId = prefs[providerKey] ?: "deepseek",
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeModeKey] ?: "") }
                .getOrDefault(ThemeMode.DARK),
            language = language,
            thinkingEnabled = prefs[thinkingKey] ?: false,
            searchEnabled = prefs[searchKey] ?: false,
            maxContextTokens = prefs[maxContextKey] ?: AppSettings.DEFAULT_MAX_CONTEXT_TOKENS,
            maxOutputTokens = prefs[maxOutputKey] ?: AppSettings.DEFAULT_MAX_OUTPUT_TOKENS,
            autoCompressEnabled = prefs[autoCompressKey] ?: true,
            defaultModeId = prefs[defaultModeIdKey] ?: "off",
            contextInjectionEnabled = prefs[contextInjectionKey] ?: true,
            tokenSavingLevel = prefs[tokenSavingKey] ?: 0,
        )
    }

    override suspend fun setSystemPrompt(value: String) {
        context.settingsDataStore.edit { it[systemPromptKey] = value }
    }

    override suspend fun setModel(value: String) {
        context.settingsDataStore.edit { it[modelKey] = value }
    }

    override suspend fun setBaseUrl(value: String) {
        context.settingsDataStore.edit { it[baseUrlKey] = value }
    }

    override suspend fun setApiKey(value: String) {
        context.settingsDataStore.edit { it[apiKeyKey] = value }
    }

    override suspend fun setProvider(providerId: String) {
        context.settingsDataStore.edit { it[providerKey] = providerId }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModeKey] = mode.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs ->
            prefs[languageKey] = language.tag
            // 语言切换时同步重置提示词默认值（用户自定义过的会被覆盖，可在设置里再改）
            prefs[systemPromptKey] = AppSettings.defaultSystemPrompt(language)
        }
    }

    override suspend fun setThinkingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[thinkingKey] = enabled }
    }

    override suspend fun setSearchEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[searchKey] = enabled }
    }

    override suspend fun setDefaultModeId(modeId: String) {
        context.settingsDataStore.edit { it[defaultModeIdKey] = modeId }
    }

    override suspend fun setContextInjectionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[contextInjectionKey] = enabled }
    }

    override suspend fun setTokenSavingLevel(level: Int) {
        context.settingsDataStore.edit { it[tokenSavingKey] = level.coerceIn(0, 3) }
    }

    override fun observeModeUserPrompt(modeId: String): Flow<String?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[stringPreferencesKey("mode_user_prompt_$modeId")]
        }

    override suspend fun setModeUserPrompt(modeId: String, prompt: String?) {
        context.settingsDataStore.edit { prefs ->
            val key = stringPreferencesKey("mode_user_prompt_$modeId")
            if (prompt.isNullOrBlank()) prefs.remove(key) else prefs[key] = prompt
        }
    }

    override fun observeModeCustomParams(modeId: String): Flow<Pair<Float, Float>?> =
        context.settingsDataStore.data.map { prefs ->
            val t = prefs[floatPreferencesKey("mode_temp_$modeId")]
            val p = prefs[floatPreferencesKey("mode_topp_$modeId")]
            if (t != null && p != null) t to p else null
        }

    override suspend fun setModeCustomParams(modeId: String, temperature: Float, topP: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[floatPreferencesKey("mode_temp_$modeId")] = temperature
            prefs[floatPreferencesKey("mode_topp_$modeId")] = topP
        }
    }

    override suspend fun setMaxContextTokens(value: Int) {
        context.settingsDataStore.edit { it[maxContextKey] = value }
    }

    override suspend fun setMaxOutputTokens(value: Int) {
        context.settingsDataStore.edit { it[maxOutputKey] = value }
    }

    override suspend fun setAutoCompressEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[autoCompressKey] = enabled }
    }

    override fun observeCurrentConfigId(): Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[currentConfigIdKey] ?: 0L
    }

    override suspend fun setCurrentConfigId(id: Long) {
        context.settingsDataStore.edit { it[currentConfigIdKey] = id }
    }

    override fun observeFirstLaunch(): Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[firstLaunchKey] ?: true
    }

    override suspend fun setFirstLaunchDone() {
        context.settingsDataStore.edit { it[firstLaunchKey] = false }
    }
}
