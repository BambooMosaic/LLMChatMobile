package com.bamboomosaic.llmchat.core.data.di

import android.content.Context
import androidx.room.Room
import com.bamboomosaic.llmchat.core.data.ChatRepositoryImpl
import com.bamboomosaic.llmchat.core.data.ConfigRepositoryImpl
import com.bamboomosaic.llmchat.core.data.ConversationRepositoryImpl
import com.bamboomosaic.llmchat.core.data.LatestReleaseFetcher
import com.bamboomosaic.llmchat.core.data.TokenUsageRepositoryImpl
import com.bamboomosaic.llmchat.core.data.context.SlidingWindowManager
import com.bamboomosaic.llmchat.core.data.settings.SettingsStore
import com.bamboomosaic.llmchat.core.data.settings.settingsDataStore
import com.bamboomosaic.llmchat.core.data.db.ApiConfigDao
import com.bamboomosaic.llmchat.core.data.db.AppDatabase
import com.bamboomosaic.llmchat.core.data.db.ConversationDao
import com.bamboomosaic.llmchat.core.data.db.TokenUsageDao
import com.bamboomosaic.llmchat.core.domain.ChatRepository
import com.bamboomosaic.llmchat.core.domain.ConfigRepository
import com.bamboomosaic.llmchat.core.domain.ContextWindowRepository
import com.bamboomosaic.llmchat.core.domain.ConversationRepository
import com.bamboomosaic.llmchat.core.domain.SettingsRepository
import com.bamboomosaic.llmchat.core.domain.TokenUsageRepository
import com.bamboomosaic.llmchat.core.network.DeepSeekTtsClient
import com.bamboomosaic.llmchat.core.network.OpenAiSseClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // 桌面端第 2 轮：ChatRepositoryImpl/SettingsStore 构造改为平台件注入，@Binds → @Provides
    @Binds
    @Singleton
    abstract fun bindTokenUsageRepository(impl: TokenUsageRepositoryImpl): TokenUsageRepository

    @Binds
    @Singleton
    abstract fun bindContextWindowRepository(impl: SlidingWindowManager): ContextWindowRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    companion object {
        @Provides
        @Singleton
        fun provideContextReminderProvider(): com.bamboomosaic.llmchat.core.domain.ContextReminderProvider =
            com.bamboomosaic.llmchat.core.domain.ContextReminderProviderImpl()

        @Provides
        @Singleton
        fun provideOpenAiSseClient(): OpenAiSseClient = OpenAiSseClient()

        @Provides
        @Singleton
        fun provideDeepSeekTtsClient(): DeepSeekTtsClient = DeepSeekTtsClient()

        @Provides
        @Singleton
        fun provideQwenTtsClient(): com.bamboomosaic.llmchat.core.network.QwenTtsClient =
            com.bamboomosaic.llmchat.core.network.QwenTtsClient()

        @Provides
        @Singleton
        fun provideGlmTtsClient(): com.bamboomosaic.llmchat.core.network.GlmTtsClient =
            com.bamboomosaic.llmchat.core.network.GlmTtsClient()

        @Provides
        @Singleton
        fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
            SettingsStore(context.settingsDataStore)

        @Provides
        @Singleton
        fun provideChatRepository(
            sseClient: OpenAiSseClient,
            tokenUsageDao: TokenUsageDao,
            configRepository: ConfigRepository,
        ): ChatRepository = ChatRepositoryImpl(
            sseClient = sseClient,
            tokenUsageDao = tokenUsageDao,
            configRepository = configRepository,
            // 20 轮：Android 侧保留 GitHub Latest 查询（桌面端本轮不需要）
            latestReleaseUrlFetcher = { LatestReleaseFetcher.fetch() },
        )

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "llmchat.db")
                // 显式迁移保留用户已有对话；未覆盖到的版本变更才走重建兜底
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        @Singleton
        fun provideTokenUsageDao(database: AppDatabase): TokenUsageDao =
            database.tokenUsageDao()

        @Provides
        @Singleton
        fun provideConversationDao(database: AppDatabase): ConversationDao =
            database.conversationDao()

        @Provides
        @Singleton
        fun provideApiConfigDao(database: AppDatabase): ApiConfigDao =
            database.apiConfigDao()
    }
}
