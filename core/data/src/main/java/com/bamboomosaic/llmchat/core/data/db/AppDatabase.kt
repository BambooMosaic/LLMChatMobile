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
    version = 8,
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
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE messages ADD COLUMN versionKey INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN usageId INTEGER")
            }
        }
    }
}
