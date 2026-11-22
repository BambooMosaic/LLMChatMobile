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
    /** 22 轮：深度思考消耗的 token 数（usage.completion_tokens_details.reasoning_tokens，无则为 0）。 */
    @ColumnInfo(name = "reasoningTokens") val reasoningTokens: Long = 0,
    /** 22 轮：是否为搜索代理轮次（供上下文面板分项统计联网搜索 token）。 */
    @ColumnInfo(name = "isSearch") val isSearch: Boolean = false,
)
