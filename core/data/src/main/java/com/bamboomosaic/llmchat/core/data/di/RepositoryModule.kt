package com.bamboomosaic.llmchat.core.data.di

import android.content.Context
import androidx.room.Room
import com.bamboomosaic.llmchat.core.data.ChatRepositoryImpl
import com.bamboomosaic.llmchat.core.data.ConfigRepositoryImpl
import com.bamboomosaic.llmchat.core.data.ConversationRepositoryImpl
import com.bamboomosaic.llmchat.core.data.TokenUsageRepositoryImpl
import com.bamboomosaic.llmchat.core.data.context.SlidingWindowManager
import com.bamboomosaic.llmchat.core.data.settings.SettingsStore
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

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindTokenUsageRepository(impl: TokenUsageRepositoryImpl): TokenUsageRepository

    @Binds
    @Singleton
    abstract fun bindContextWindowRepository(impl: SlidingWindowManager): ContextWindowRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    companion object {
        @Provides
        @Singleton
        fun provideOpenAiSseClient(): OpenAiSseClient = OpenAiSseClient()

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "llmchat.db")
                // 显式迁移保留用户已有对话；未覆盖到的版本变更才走重建兜底
                .addMigrations(
                    AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_6_7,
                )
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
