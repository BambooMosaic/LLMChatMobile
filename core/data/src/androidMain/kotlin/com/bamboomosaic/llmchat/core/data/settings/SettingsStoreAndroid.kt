package com.bamboomosaic.llmchat.core.data.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Android 侧 DataStore 实例（文件名与读写行为与改造前完全一致）。 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

/** 系统深色判定：uiMode 夜间掩码（与改造前一致）。 */
actual fun isSystemDarkMode(): Boolean =
    (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/** 当前应用版本名：framework PackageInfo（经全局 Application 单例取 context）。 */
actual fun appVersionName(): String = runCatching {
    val ctx = appContextHolder()
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
}.getOrDefault("")

/**
 * 全局 Application context（进程创建时注册，见 MyApplication）。
 * 反射 AppGlobals / ActivityThread 两条路兜底——core:data 无 Hilt 注入点可用时的最小方案。
 */
internal fun appContextHolder(): Context = runCatching {
    Class.forName("android.app.AppGlobals")
        .getMethod("getInitialApplication").invoke(null) as android.app.Application
}.getOrElse {
    Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null) as android.app.Application
}
