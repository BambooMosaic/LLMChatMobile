package com.bamboomosaic.llmchat.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import java.io.File

/** 桌面端本轮 UI 固定深色，不读系统主题（TODO 跟随系统深浅色）。 */
actual fun isSystemDarkMode(): Boolean = false

/** TODO 桌面打包后接 jpackage 版本号；当前固定值即视为同一大版本段（不触发重引导）。 */
actual fun appVersionName(): String = "beta0.1.0"

/** 桌面端 DataStore：~/.llmchat/app_settings.preferences_pb。 */
fun desktopSettingsDataStore(scope: CoroutineScope): DataStore<Preferences> {
    val dir = File(System.getProperty("user.home"), ".llmchat").apply { mkdirs() }
    return PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = { File(dir, "app_settings.preferences_pb").absolutePath.toPath() },
    )
}
