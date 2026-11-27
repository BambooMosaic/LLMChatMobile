package com.bamboomosaic.llmchat.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 最新发布版查询端点（Latest 不含 prerelease）。Android 平台件（桌面端本轮不需要下载更新）。 */
private const val LATEST_RELEASE_API =
    "https://api.github.com/repos/BambooMosaic/LLMChatMobile/releases/latest"

/** 20 轮逻辑原样迁出：GitHub Releases Latest API → 安卓 APK 下载直链。 */
object LatestReleaseFetcher {

    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        runCatching {
            // JDK HttpURLConnection：不穿透网络模块边界
            val conn = java.net.URL(LATEST_RELEASE_API)
                .openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.inputStream.use { input ->
                val root = org.json.JSONObject(input.bufferedReader().readText())
                val assets = root.optJSONArray("assets") ?: return@runCatching null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith("_android_release.apk")) {
                        return@runCatching asset.optString("browser_download_url")
                            .takeIf { it.isNotBlank() }
                    }
                }
                null
            }
        }.getOrNull()
    }
}
