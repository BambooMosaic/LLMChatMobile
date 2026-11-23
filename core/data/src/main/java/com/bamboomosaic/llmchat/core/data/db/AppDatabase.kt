package com.bamboomosaic.llmchat.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TokenUsageRecordEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ApiConfigEntity::class,
    ],
    views = [ModelStatsView::class],
    version = 12,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenUsageDao(): TokenUsageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun apiConfigDao(): ApiConfigDao

    companion object {
        /**
         * v2 -> v3：会话支持归档、消息保存思考内容。
         * 用显式迁移而不是 destructive：升级时保留用户已有对话。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN reasoning TEXT")
            }
        }

        /**
         * v3 -> v4：新增多套 API 配置表。
         * 表建好后由 ConfigRepositoryImpl.ensureSeeded() 把 DataStore 里的旧配置写成第一条默认配置
         * （Migration 里读不到 DataStore，所以放在仓库层做）。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `api_configs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `baseUrl` TEXT NOT NULL,
                        `apiKey` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `supportReasoning` INTEGER NOT NULL DEFAULT 0,
                        `isDefault` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * v4 -> v5：会话独立系统提示词。
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN systemPrompt TEXT")
            }
        }

        /**
         * v5 -> v6：消息支持多模态图片。
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN images TEXT")
            }
        }

        /**
         * v6 -> v7：消息记录联网搜索到的网页总数（搜索状态展示）。
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN searchPages INTEGER")
            }
        }

        /**
         * v7 -> v8：消息多版本（重新生成/编辑重发保留历史版本）。
         * 历史单版本数据自动升级为 version=1；usageId 关联该版本的 token 用量。
         */
        /**
         * v8 -> v9：双层系统提示词架构。
         * 会话加模式字段；历史会话自动标记为自定义模式，原 systemPrompt 迁移至 userPrompt。
         * api_configs 加每配置独立的最大上下文/最大输出（NULL = 不限制，用全局默认）。
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN modeId TEXT NOT NULL DEFAULT 'custom'")
                db.execSQL("ALTER TABLE conversations ADD COLUMN userPrompt TEXT")
                db.execSQL("ALTER TABLE conversations ADD COLUMN effectiveTemperature REAL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN effectiveTopP REAL")
                // 原全局/会话系统提示词迁移至用户层（历史数据不丢失）
                db.execSQL("UPDATE conversations SET userPrompt = systemPrompt WHERE systemPrompt IS NOT NULL")
                db.execSQL("ALTER TABLE api_configs ADD COLUMN maxContextTokens INTEGER")
                db.execSQL("ALTER TABLE api_configs ADD COLUMN maxOutputTokens INTEGER")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE messages ADD COLUMN versionKey INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN usageId INTEGER")
            }
        }

        /**
         * v9 -> v10：用量记录增加缓存命中列（DeepSeek 系 usage 返回 prompt_cache_hit_tokens），
         * 用于缓存命中统计与三色柱状图。
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE token_usage_records ADD COLUMN cacheHitTokens INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v10 -> v11：用量记录增加深度思考 token 列（usage.completion_tokens_details.reasoning_tokens）
         * 与联网搜索标记列（搜索代理轮次），供上下文面板按会话分项统计。
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE token_usage_records ADD COLUMN reasoningTokens INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE token_usage_records ADD COLUMN isSearch INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v11 -> v12：会话置顶与自动标题标记（24 轮）。
         * pinned：置顶会话固定组优先；titleAuto：1 = 标题仍为自动生成，用户手动改名后置 0。
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN titleAuto INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
