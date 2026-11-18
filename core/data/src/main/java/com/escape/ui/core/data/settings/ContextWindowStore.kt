package com.escape.ui.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.escape.ui.core.domain.ContextWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.contextWindowDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "context_window_prefs",
)

/** 用 DataStore Preferences 保存上下文窗口配置（不塞 SharedPreferences 字符串）。 */
class ContextWindowStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val maxTokensKey = intPreferencesKey("max_tokens")
    private val compressThresholdKey = floatPreferencesKey("compress_threshold")

    val window: Flow<ContextWindow> = context.contextWindowDataStore.data.map { prefs ->
        ContextWindow(
            maxTokens = prefs[maxTokensKey] ?: ContextWindow.DEFAULT_MAX_TOKENS,
            compressThreshold = prefs[compressThresholdKey] ?: ContextWindow.DEFAULT_COMPRESS_THRESHOLD,
        )
    }

    suspend fun setWindow(maxTokens: Int, compressThreshold: Float) {
        context.contextWindowDataStore.edit { prefs ->
            prefs[maxTokensKey] = maxTokens
            prefs[compressThresholdKey] = compressThreshold
        }
    }
}
