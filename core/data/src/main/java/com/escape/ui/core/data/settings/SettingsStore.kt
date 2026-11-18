package com.escape.ui.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escape.ui.core.domain.AppSettings
import com.escape.ui.core.domain.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

/** 用 DataStore Preferences 保存 systemPrompt / model / baseUrl / apiKey。 */
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val systemPromptKey = stringPreferencesKey("system_prompt")
    private val modelKey = stringPreferencesKey("model")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val apiKeyKey = stringPreferencesKey("api_key")

    override fun observeSettings(): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            systemPrompt = prefs[systemPromptKey] ?: AppSettings.DEFAULT_SYSTEM_PROMPT,
            model = prefs[modelKey] ?: AppSettings.DEFAULT_MODEL,
            baseUrl = prefs[baseUrlKey] ?: AppSettings.DEFAULT_BASE_URL,
            apiKey = prefs[apiKeyKey] ?: AppSettings.DEFAULT_API_KEY,
        )
    }

    override suspend fun setSystemPrompt(value: String) {
        context.settingsDataStore.edit { prefs -> prefs[systemPromptKey] = value }
    }

    override suspend fun setModel(value: String) {
        context.settingsDataStore.edit { prefs -> prefs[modelKey] = value }
    }

    override suspend fun setBaseUrl(value: String) {
        context.settingsDataStore.edit { prefs -> prefs[baseUrlKey] = value }
    }

    override suspend fun setApiKey(value: String) {
        context.settingsDataStore.edit { prefs -> prefs[apiKeyKey] = value }
    }
}
