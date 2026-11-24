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
    // 26 轮：完成引导时记录的大版本段（major.minor，如 "0.6"）——大版本变化才重新引导
    private val onboardVersionKey = stringPreferencesKey("onboard_version")
    private val defaultModeIdKey = stringPreferencesKey("default_mode_id")
    private val contextInjectionKey = booleanPreferencesKey("context_injection_enabled")
    private val tokenSavingKey = intPreferencesKey("token_saving_level")
    private val roleplayParenKey = booleanPreferencesKey("roleplay_paren_rule")
    private val userNicknameKey = stringPreferencesKey("user_nickname")
    private val backgroundImageUriKey = stringPreferencesKey("background_image_uri")
    private val colorPresetKey = stringPreferencesKey("color_preset")
    // 23 轮：跟随系统 + 配对索引（浅色组与深色组按位置一一配对：0=默认对默认…5=浅粉对深红）
    private val followSystemKey = booleanPreferencesKey("follow_system_theme")
    private val colorPresetPairKey = intPreferencesKey("color_preset_pair")
    private val compressSummaryLimitKey = intPreferencesKey("compress_summary_limit")
    private val glassLevelKey = intPreferencesKey("glass_level")
    // 27 轮：语音朗读（DeepSeek TTS）
    private val ttsEnabledKey = booleanPreferencesKey("tts_enabled")
    private val ttsVoiceKey = stringPreferencesKey("tts_voice")
    private val legacyExperimentalGlassKey = booleanPreferencesKey("experimental_glass")

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
            roleplayParenRule = prefs[roleplayParenKey] ?: true,
            userNickname = prefs[userNicknameKey] ?: "",
            backgroundImageUri = prefs[backgroundImageUriKey] ?: "",
            colorPreset = effectiveColorPreset(prefs),
            followSystemTheme = prefs[followSystemKey] ?: false,
            compressSummaryLimit = prefs[compressSummaryLimitKey] ?: 400,
            glassLevel = run {
                prefs[glassLevelKey]?.let { return@run it.coerceIn(0, 2) }
                // 25 轮迁移：旧实验开关 true = 半透明档
                if (prefs[legacyExperimentalGlassKey] == true) 1 else 0
            },
            ttsEnabled = prefs[ttsEnabledKey] ?: true,
            ttsVoice = prefs[ttsVoiceKey]?.takeIf { it == "stella" || it == "tide" } ?: "stella",
        )
    }

    /**
     * 23 轮：生效预设——跟随系统开启时按配对索引取系统深浅对应组的预设
     * （浅色组与深色组按位置一一配对：默认↔默认、黄↔棕、浅灰↔纯黑、浅粉↔深红…），
     * 关闭时取手动选中值。配对索引缺省时由当前选中值的位置推导。
     */
    private fun effectiveColorPreset(prefs: androidx.datastore.preferences.core.Preferences): String {
        val active = migrateColorPreset(prefs)
        val follow = prefs[followSystemKey] ?: false
        if (!follow) return active
        val pair = prefs[colorPresetPairKey] ?: pairIndexOf(active)
        if (pair !in 0..5) return active
        val systemDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        ) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (systemDark) darkPairIds().getOrNull(pair) ?: active else lightPairIds().getOrNull(pair) ?: active
    }

    /** 23 轮：配对位置表（与设置页两组色块的顺序一一对应）。 */
    private fun lightPairIds() = listOf(
        "light_default", "light_blue", "light_green", "light_yellow", "light_gray", "light_pink",
    )

    private fun darkPairIds() = listOf(
        "dark_default", "dark_blue", "dark_green", "dark_brown", "dark_black", "dark_red",
    )

    private fun pairIndexOf(presetId: String): Int =
        if (presetId.startsWith("light_")) lightPairIds().indexOf(presetId).coerceAtLeast(0)
        else darkPairIds().indexOf(presetId).coerceAtLeast(0)

    /**
     * 22 轮：配色预设改为自带深浅前缀的 12 套（light_ / dark_）。
     * 21 轮旧 id（default/blue/green/warm）按当时的明暗模式设置迁移到新 id，
     * 跟随系统时读一次系统夜间模式定组，之后明暗完全由所选预设决定。
     */
    private fun migrateColorPreset(prefs: androidx.datastore.preferences.core.Preferences): String {
        val stored = prefs[colorPresetKey] ?: return "dark_default"
        if (stored.startsWith("light_") || stored.startsWith("dark_")) return stored
        val group = when (
            runCatching { ThemeMode.valueOf(prefs[themeModeKey] ?: "") }.getOrDefault(ThemeMode.DARK)
        ) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            ThemeMode.SYSTEM ->
                if ((context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                ) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                ) "dark" else "light"
        }
        val name = if (group == "light" && stored == "warm") "yellow" else
            if (stored == "warm") "brown" else stored
        return "${group}_$name"
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
        context.settingsDataStore.edit { it[tokenSavingKey] = level.coerceIn(0, 4) }
    }

    override suspend fun setRoleplayParenRule(enabled: Boolean) {
        context.settingsDataStore.edit { it[roleplayParenKey] = enabled }
    }

    override suspend fun setUserNickname(value: String) {
        context.settingsDataStore.edit { it[userNicknameKey] = value.trim() }
    }

    override suspend fun setBackgroundImageUri(value: String) {
        context.settingsDataStore.edit { it[backgroundImageUriKey] = value }
    }

    override suspend fun setColorPreset(value: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[colorPresetKey] = value
            // 23 轮：同步记录配对索引（跟随系统自动切换时按位置取对应组）
            prefs[colorPresetPairKey] = pairIndexOf(value)
        }
    }

    /** 23 轮：跟随系统明暗开关。开启瞬间由当前选中值推导配对索引。 */
    override suspend fun setFollowSystemTheme(value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[followSystemKey] = value
            if (value) {
                val active = migrateColorPreset(prefs)
                prefs[colorPresetPairKey] = pairIndexOf(active)
            }
        }
    }

    /** 23 轮：手动压缩摘要字数上限（100~1024）。 */
    override suspend fun setCompressSummaryLimit(value: Int) {
        context.settingsDataStore.edit { it[compressSummaryLimitKey] = value.coerceIn(100, 1024) }
    }

    /** 25 轮：玻璃材质档位（0/1/2）。 */
    override suspend fun setGlassLevel(value: Int) {
        context.settingsDataStore.edit { it[glassLevelKey] = value.coerceIn(0, 2) }
    }

    /** 27 轮：语音朗读开关。 */
    override suspend fun setTtsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[ttsEnabledKey] = enabled }
    }

    /** 27 轮：TTS 音色（stella / tide）。 */
    override suspend fun setTtsVoice(voiceId: String) {
        context.settingsDataStore.edit {
            it[ttsVoiceKey] = if (voiceId == "tide") "tide" else "stella"
        }
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

    /**
     * 26 轮：首次引导跟随大版本——从未完成过引导照常展示；已完成过引导的用户，
     * 只在记录的大版本段（major.minor，如 "0.6"）与当前不同时重新引导，
     * 小版本更新（0.6.4 → 0.6.5）不再触发。
     * 老用户（26 轮前完成引导，无版本记录）首次运行时补记当前大版本段，0.7 起即可按大版本判断。
     */
    override fun observeFirstLaunch(): Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        if (prefs[firstLaunchKey] ?: true) return@map true
        val stored = prefs[onboardVersionKey]
        if (stored == null) {
            context.settingsDataStore.edit { it[onboardVersionKey] = currentMajorMinor() }
            return@map false
        }
        stored != currentMajorMinor()
    }

    override suspend fun setFirstLaunchDone() {
        context.settingsDataStore.edit {
            it[firstLaunchKey] = false
            it[onboardVersionKey] = currentMajorMinor()
        }
    }

    /** 26 轮：当前版本的大版本段——versionName（如 "beta0.6.5"）取前两段数字，解析失败返回空串（视为同段）。 */
    private fun currentMajorMinor(): String = runCatching {
        val name = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        Regex("(\\d+)\\.(\\d+)").find(name)?.let { m -> "${m.groupValues[1]}.${m.groupValues[2]}" } ?: name
    }.getOrDefault("")
}
