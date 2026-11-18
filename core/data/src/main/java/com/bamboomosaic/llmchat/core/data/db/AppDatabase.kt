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
    ],
    views = [ModelStatsView::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenUsageDao(): TokenUsageDao
    abstract fun conversationDao(): ConversationDao

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
    }
}
