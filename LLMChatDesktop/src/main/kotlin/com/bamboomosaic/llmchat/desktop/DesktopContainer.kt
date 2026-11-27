package com.bamboomosaic.llmchat.desktop

import com.bamboomosaic.llmchat.core.data.ChatRepositoryImpl
import com.bamboomosaic.llmchat.core.data.ConfigRepositoryImpl
import com.bamboomosaic.llmchat.core.data.ConversationRepositoryImpl
import com.bamboomosaic.llmchat.core.data.TokenUsageRepositoryImpl
import com.bamboomosaic.llmchat.core.data.context.SlidingWindowManager
import com.bamboomosaic.llmchat.core.data.db.AppDatabase
import com.bamboomosaic.llmchat.core.data.desktopDatabase
import com.bamboomosaic.llmchat.core.data.settings.SettingsStore
import com.bamboomosaic.llmchat.core.data.settings.desktopSettingsDataStore
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.ContextReminderProvider
import com.bamboomosaic.llmchat.core.domain.ContextReminderProviderImpl
import com.bamboomosaic.llmchat.core.domain.ContextWindowRepository
import com.bamboomosaic.llmchat.core.domain.ConversationRepository
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import com.bamboomosaic.llmchat.core.network.OpenAiSseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 桌面端手写依赖注入（桌面端第 2 轮；Hilt 不支持桌面端，Koin 等框架本轮不引入）。
 * 进程级单例：数据库 / DataStore / 各仓库 / SSE 客户端——装配对应 Mobile 侧 RepositoryModule。
 */
object DesktopContainer {

    /** 全进程协程作用域（DataStore / Room Flow 的宿主）。 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy { desktopDatabase() }

    val settingsRepository: SettingsRepository by lazy {
        SettingsStore(desktopSettingsDataStore(appScope))
    }

    val configRepository: ConfigRepository by lazy {
        ConfigRepositoryImpl(database.apiConfigDao(), settingsRepository)
    }

    val conversationRepository: ConversationRepository by lazy {
        ConversationRepositoryImpl(database.conversationDao())
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(
            sseClient = OpenAiSseClient(),
            tokenUsageDao = database.tokenUsageDao(),
            configRepository = configRepository,
            // 桌面端本轮不需要「检查更新」的 APK 直链查询
        )
    }

    val contextWindowRepository: ContextWindowRepository by lazy {
        SlidingWindowManager(settingsRepository)
    }

    val contextReminderProvider: ContextReminderProvider by lazy {
        ContextReminderProviderImpl()
    }
}
