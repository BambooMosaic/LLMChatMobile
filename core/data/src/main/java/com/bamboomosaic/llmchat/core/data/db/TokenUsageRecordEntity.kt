package com.bamboomosaic.llmchat.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token_usage_records")
data class TokenUsageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sessionId") val sessionId: String,
    @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "promptTokens") val promptTokens: Long,
    @ColumnInfo(name = "completionTokens") val completionTokens: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    /** 20 轮：prompt 中命中缓存的 token 数（DeepSeek 系返回，其他模型 0）。 */
    @ColumnInfo(name = "cacheHitTokens") val cacheHitTokens: Long = 0,
)
