package com.bamboomosaic.llmchat.core.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.bamboomosaic.llmchat.core.data.db.ApiConfigEntity
import com.bamboomosaic.llmchat.core.data.db.AppDatabase
import com.bamboomosaic.llmchat.core.data.db.ConversationEntity
import com.bamboomosaic.llmchat.core.data.db.MessageEntity
import com.bamboomosaic.llmchat.core.data.db.TokenUsageRecordEntity
import com.bamboomosaic.llmchat.core.data.settings.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** 备份文件载荷（29 轮）。settings 为编码字符串映射，四个表直接序列化实体。 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int = SCHEMA_VERSION,
    val appVersion: String = "",
    val exportedAt: Long = 0,
    val settings: Map<String, String> = emptyMap(),
    val conversations: List<ConversationEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val apiConfigs: List<ApiConfigEntity> = emptyList(),
    val tokenUsage: List<TokenUsageRecordEntity> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * 29 轮：一键导出/导入全部数据——设置 + API 配置 + 会话 + 消息 + token 统计，一个 JSON 文件。
 * 约定（29 轮已拍板）：
 * - API Key 不导出（导出时全部置空串；导入后需重新填写）
 * - 导入 = 全量替换：事务内清空四表后重插；会话/消息 id 显式保留，parentId 分支树不断链
 * - 设置整体覆盖，但引导状态 / 更新公告游标按本机处理、背景图跨设备无效强制清空
 */
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore,
    @ApplicationContext private val appContext: Context,
) {

    private val json = Json {
        ignoreUnknownKeys = true // 跨版本前后多出的字段互相容忍
        encodeDefaults = true
    }

    /** 导出为 JSON 文本（耗时操作，调用方放 IO 线程）。 */
    suspend fun export(): String = withContext(Dispatchers.IO) {
        json.encodeToString(
            BackupPayload(
                appVersion = appVersion(),
                exportedAt = System.currentTimeMillis(),
                settings = settingsStore.backupSettings(),
                conversations = db.conversationDao().getAllConversations(),
                messages = db.conversationDao().getAllMessages(),
                // 29 轮约定：API Key 不导出，一律置空
                apiConfigs = db.apiConfigDao().getAllConfigs().map { it.copy(apiKey = "") },
                tokenUsage = db.tokenUsageDao().getAllRecords(),
            ),
        )
    }

    /** 从 JSON 文本导入（全量替换）。失败时返回异常（版本不匹配 / 数据损坏），原数据不受影响。 */
    suspend fun import(jsonText: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.decodeFromString<BackupPayload>(jsonText)
            check(payload.schemaVersion == BackupPayload.SCHEMA_VERSION) {
                "备份文件版本不匹配（schema ${payload.schemaVersion}，当前 ${BackupPayload.SCHEMA_VERSION}），无法导入"
            }
            // 单事务替换：任一步失败整体回滚，不留半截数据
            db.withTransaction {
                db.conversationDao().deleteAllMessages()
                db.conversationDao().deleteAllConversations()
                db.tokenUsageDao().deleteAllRecords()
                db.apiConfigDao().deleteAll()
                db.conversationDao().insertAllConversations(payload.conversations)
                db.conversationDao().insertAllMessages(payload.messages)
                db.tokenUsageDao().insertAllRecords(payload.tokenUsage)
                db.apiConfigDao().insertAll(payload.apiConfigs)
            }
            settingsStore.restoreSettings(payload.settings)
        }
    }

    private fun appVersion(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: ""
    }.getOrDefault("")
}
